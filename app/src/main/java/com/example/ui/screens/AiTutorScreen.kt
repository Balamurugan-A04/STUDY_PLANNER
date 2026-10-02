package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PreparationMode
import com.example.model.SubjectData
import com.example.model.SyllabusData
import com.example.ui.components.BottomNavBar
import com.example.ui.components.NavTab
import com.example.ui.theme.AppTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: String = "Just now"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTutorScreen(
    mode: PreparationMode,
    subjects: List<SubjectData> = emptyList(),
    syllabusList: List<SyllabusData> = emptyList(),
    onSendMessage: suspend (String, String?, String?) -> String,
    onNavigateTab: (NavTab) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isAcademic = mode == PreparationMode.ACADEMIC

    val academicSubjects = remember(subjects, isAcademic) {
        if (isAcademic) {
            val userSubs = subjects.filter { it.mode == PreparationMode.ACADEMIC }.map { it.name }.distinct()
            if (userSubs.isNotEmpty()) userSubs else listOf("General Academic")
        } else {
            emptyList()
        }
    }

    var selectedSubject by remember(academicSubjects) {
        mutableStateOf(if (academicSubjects.isNotEmpty()) academicSubjects.first() else "General Academic")
    }

    val availableSubtopics = remember(selectedSubject, syllabusList, isAcademic) {
        if (isAcademic && selectedSubject.isNotBlank() && selectedSubject != "General Academic") {
            val topics = syllabusList.filter {
                it.mode == PreparationMode.ACADEMIC &&
                it.subject.trim().equals(selectedSubject.trim(), ignoreCase = true)
            }.map { it.topic }.distinct()
            if (topics.isNotEmpty()) listOf("All Subtopics") + topics else listOf("All Subtopics")
        } else {
            listOf("All Subtopics")
        }
    }

    var selectedSubtopic by remember(availableSubtopics) {
        mutableStateOf(if (availableSubtopics.isNotEmpty()) availableSubtopics.first() else "All Subtopics")
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                role = "assistant",
                content = if (mode == PreparationMode.PROFESSIONAL_GATE)
                    "Hello! I am your GATE AI Tutor. Ask me any GATE question, concept explanation, or exam strategy!"
                else
                    "Hi! I'm your AI Academic Tutor powered by gpt-oss-20b. Select your subject and subtopic above and ask any academic concept question!"
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val suggestedPrompts = remember(mode, selectedSubject, selectedSubtopic) {
        if (mode == PreparationMode.PROFESSIONAL_GATE) {
            listOf(
                "What is a stack in data structures?",
                "What is normalization in DBMS?",
                "What is polymorphism in Java?",
                "Explain Newton's second law."
            )
        } else {
            val subClean = selectedSubject.lowercase()
            val topClean = selectedSubtopic.lowercase()
            when {
                subClean.contains("cloud") || topClean.contains("cloud") -> listOf(
                    "What is SaaS?",
                    "Explain IaaS vs PaaS",
                    "What are Cloud Deployment Models?",
                    "Define Cloud Computing"
                )
                subClean.contains("mobile") || subClean.contains("mad") || subClean.contains("android") || topClean.contains("android") -> listOf(
                    "Define Android?",
                    "Uses of MAD (Mobile Application Development)?",
                    "What is an Activity Lifecycle in Android?",
                    "Explain MVC vs MVVM in mobile apps"
                )
                subClean.contains("math") || subClean.contains("stat") || topClean.contains("mean") || topClean.contains("median") -> listOf(
                    "Define Mean?",
                    "What is Median?",
                    "Explain Standard Deviation",
                    "Difference between Mean and Median"
                )
                else -> listOf(
                    "Explain the core principles of $selectedSubject",
                    "What are the main concepts in $selectedSubtopic?",
                    "Give an example problem for $selectedSubject",
                    "What are common exam questions for $selectedSubject?"
                )
            }
        }
    }

    fun sendMessage(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            Toast.makeText(context, "Please enter a question.", Toast.LENGTH_SHORT).show()
            errorMessage = "Please enter a question."
            return
        }
        if (isLoading) return

        errorMessage = null
        inputText = ""
        messages.add(ChatMessage(role = "user", content = trimmedQuery))
        isLoading = true

        val subjToSend = if (isAcademic) selectedSubject else null
        val subtopToSend = if (isAcademic && selectedSubtopic != "All Subtopics") selectedSubtopic else null

        coroutineScope.launch {
            try {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
                val reply = onSendMessage(trimmedQuery, subjToSend, subtopToSend)
                messages.add(ChatMessage(role = "assistant", content = reply))
            } catch (e: Exception) {
                messages.add(
                    ChatMessage(
                        role = "assistant",
                        content = "Sorry, I couldn't process your question right now. Please try again."
                    )
                )
            } finally {
                isLoading = false
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }
    }

    AppTheme(mode = mode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAcademic) "Academic AI Tutor" else "GATE AI Tutor",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                BottomNavBar(
                    currentTab = NavTab.AI_TUTOR,
                    onTabSelected = onNavigateTab
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("screen_ai_tutor")
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Academic Context Selectors
                if (isAcademic && academicSubjects.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Subject Dropdown
                                var subjectExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedCard(
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { subjectExpanded = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = selectedSubject,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = subjectExpanded,
                                        onDismissRequest = { subjectExpanded = false }
                                    ) {
                                        academicSubjects.forEach { sub ->
                                            DropdownMenuItem(
                                                text = { Text(sub, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedSubject = sub
                                                    subjectExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Subtopic Dropdown
                                var subtopicExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedCard(
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { subtopicExpanded = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = selectedSubtopic,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = subtopicExpanded,
                                        onDismissRequest = { subtopicExpanded = false }
                                    ) {
                                        availableSubtopics.forEach { top ->
                                            DropdownMenuItem(
                                                text = { Text(top, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedSubtopic = top
                                                    subtopicExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Messages List
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isUser = msg.role == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isUser) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Card(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Text(
                                    text = msg.content,
                                    modifier = Modifier.padding(14.dp),
                                    fontSize = 14.sp,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                    lineHeight = 20.sp
                                )
                            }

                            if (isUser) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("AI Tutor is thinking...", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }

                // Quick Prompt Chips
                ScrollableTabRow(
                    selectedTabIndex = 0,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                    divider = {}
                ) {
                    suggestedPrompts.forEach { prompt ->
                        SuggestionChip(
                            onClick = { sendMessage(prompt) },
                            label = { Text(prompt, fontSize = 12.sp) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input Bar
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                if (it.isNotBlank()) errorMessage = null
                            },
                            placeholder = {
                                Text(
                                    if (isAcademic) "Ask about $selectedSubject..." else "Ask your GATE Tutor..."
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_ai_query"),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { sendMessage(inputText) },
                            enabled = !isLoading,
                            modifier = Modifier.testTag("btn_send_ai")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

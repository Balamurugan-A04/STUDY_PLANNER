package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.PreparationMode
import com.example.model.UserProfile
import com.example.ui.components.*
import com.example.ui.theme.AppTheme

@Composable
fun AcademicDashboardScreen(
    user: UserProfile?,
    onNavigateStudyPlan: () -> Unit,
    onNavigateSubjects: () -> Unit,
    onNavigateAiTutor: () -> Unit,
    onNavigateNotes: () -> Unit,
    onNavigateMockTests: () -> Unit,
    onNavigateProgress: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    AppTheme(mode = PreparationMode.ACADEMIC) {
        Scaffold(
            bottomBar = {
                BottomNavBar(
                    currentTab = NavTab.HOME,
                    onTabSelected = { tab ->
                        when (tab) {
                            NavTab.HOME -> {}
                            NavTab.PROGRESS -> onNavigateProgress()
                            NavTab.AI_TUTOR -> onNavigateAiTutor()
                            NavTab.SYLLABUS -> onNavigateSubjects()
                            NavTab.PROFILE -> onNavigateProfile()
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("screen_academic_dashboard")
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                DashboardHeader(
                    userName = user?.fullName ?: "Academic Student",
                    subtitle = "Ready to learn today?",
                    onProfileClick = onNavigateProfile,
                    onNotificationClick = { /* Notification view */ }
                )

                HeroCard(
                    title = "Academic Dashboard",
                    subtitle = "Continue your academic preparation journey",
                    buttonText = "View Study Plan",
                    onButtonClick = onNavigateStudyPlan
                )

                Spacer(modifier = Modifier.height(12.dp))

                val quickAccessItems = listOf(
                    QuickItem("Subjects", Icons.Default.Book, onNavigateSubjects, "qa_subjects"),
                    QuickItem("AI Tutor", Icons.Default.AutoAwesome, onNavigateAiTutor, "qa_ai_tutor"),
                    QuickItem("Notes", Icons.Default.Description, onNavigateNotes, "qa_notes"),
                    QuickItem("Mock Tests", Icons.Default.Quiz, onNavigateMockTests, "qa_mock_tests")
                )

                QuickAccessGrid(items = quickAccessItems)

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

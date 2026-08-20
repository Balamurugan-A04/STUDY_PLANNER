package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun GateDashboardScreen(
    user: UserProfile?,
    onNavigateSyllabus: () -> Unit,
    onNavigateStudyPlan: () -> Unit,
    onNavigatePractice: () -> Unit,
    onNavigateMockTests: () -> Unit,
    onNavigateNotes: () -> Unit,
    onNavigatePyqs: () -> Unit,
    onNavigateProgress: () -> Unit,
    onNavigateAiTutor: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    AppTheme(mode = PreparationMode.PROFESSIONAL_GATE) {
        Scaffold(
            bottomBar = {
                BottomNavBar(
                    currentTab = NavTab.HOME,
                    onTabSelected = { tab ->
                        when (tab) {
                            NavTab.HOME -> {}
                            NavTab.PROGRESS -> onNavigateProgress()
                            NavTab.AI_TUTOR -> onNavigateAiTutor()
                            NavTab.SYLLABUS -> onNavigateSyllabus()
                            NavTab.PROFILE -> onNavigateProfile()
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("screen_gate_dashboard")
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                DashboardHeader(
                    userName = user?.fullName ?: "GATE Aspirant",
                    subtitle = "Ready for GATE Success?",
                    onProfileClick = onNavigateProfile,
                    onNotificationClick = { /* Notifications */ }
                )

                HeroCard(
                    title = "GATE Dashboard",
                    subtitle = "Your GATE preparation starts here",
                    buttonText = "View Study Plan",
                    onButtonClick = onNavigateStudyPlan
                )

                Spacer(modifier = Modifier.height(8.dp))

                val coreQuickAccess = listOf(
                    QuickItem("Syllabus", Icons.Default.MenuBook, onNavigateSyllabus, "qa_gate_syllabus"),
                    QuickItem("Study Plan", Icons.Default.CalendarMonth, onNavigateStudyPlan, "qa_gate_study_plan"),
                    QuickItem("Practice", Icons.Default.FitnessCenter, onNavigatePractice, "qa_gate_practice"),
                    QuickItem("Mock Tests", Icons.Default.Quiz, onNavigateMockTests, "qa_gate_mock_tests")
                )

                QuickAccessGrid(items = coreQuickAccess)

                Spacer(modifier = Modifier.height(8.dp))

                val extraQuickAccess = listOf(
                    QuickItem("Notes", Icons.Default.Description, onNavigateNotes, "qa_gate_notes"),
                    QuickItem("Previous Year Qs", Icons.Default.HistoryEdu, onNavigatePyqs, "qa_gate_pyqs"),
                    QuickItem("Progress", Icons.Default.BarChart, onNavigateProgress, "qa_gate_progress"),
                    QuickItem("AI Tutor", Icons.Default.AutoAwesome, onNavigateAiTutor, "qa_gate_ai_tutor")
                )

                QuickAccessGrid(items = extraQuickAccess)

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

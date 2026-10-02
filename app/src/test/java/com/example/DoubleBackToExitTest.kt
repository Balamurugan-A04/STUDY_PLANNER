package com.example

import com.example.model.PreparationMode
import com.example.ui.viewmodel.Screen
import org.junit.Assert.*
import org.junit.Test

class DoubleBackToExitTest {

    class BackPressNavigator(var currentScreen: Screen = Screen.Dashboard) {
        var lastBackPressedTime: Long = 0L
        var exitCalled: Boolean = false
        var toastMessage: String? = null

        fun handleBackPress(currentTime: Long) {
            when (currentScreen) {
                is Screen.Dashboard, is Screen.Login -> {
                    if (lastBackPressedTime != 0L && currentTime - lastBackPressedTime < 2000L) {
                        exitCalled = true
                    } else {
                        lastBackPressedTime = currentTime
                        toastMessage = "Press back again to exit"
                    }
                }
                is Screen.AcademicRegister, is Screen.GateRegister -> {
                    currentScreen = Screen.Login
                }
                is Screen.SubjectDetail -> {
                    currentScreen = Screen.Syllabus
                }
                is Screen.Splash -> {
                    exitCalled = true
                }
                else -> {
                    currentScreen = Screen.Dashboard
                }
            }
        }
    }

    @Test
    fun test1_singleBackPressAtRootDoesNotExitAndShowsMessage() {
        val navigator = BackPressNavigator(Screen.Dashboard)
        val time1 = 1000L

        navigator.handleBackPress(time1)

        assertFalse("App should not exit on single back press", navigator.exitCalled)
        assertEquals("Press back again to exit", navigator.toastMessage)
        assertEquals(time1, navigator.lastBackPressedTime)
    }

    @Test
    fun test2_secondBackPressWithinTwoSecondsExits() {
        val navigator = BackPressNavigator(Screen.Dashboard)
        val time1 = 1000L
        navigator.handleBackPress(time1)
        assertFalse(navigator.exitCalled)

        // Press again within 1.5 seconds (1500ms < 2000ms)
        val time2 = 2500L
        navigator.handleBackPress(time2)

        assertTrue("App should exit when back pressed second time within 2 seconds", navigator.exitCalled)
    }

    @Test
    fun test3_secondBackPressAfterTwoSecondsResetsAndDoesNotExit() {
        val navigator = BackPressNavigator(Screen.Dashboard)
        val time1 = 1000L
        navigator.handleBackPress(time1)
        assertFalse(navigator.exitCalled)

        // Reset toast
        navigator.toastMessage = null

        // Press again after 2.5 seconds (2500ms >= 2000ms)
        val time2 = 3500L
        navigator.handleBackPress(time2)

        assertFalse("App should not exit when second back press is after 2 seconds", navigator.exitCalled)
        assertEquals("Press back again to exit", navigator.toastMessage)
        assertEquals(time2, navigator.lastBackPressedTime)
    }

    @Test
    fun test4_internalScreenNavigationPreserved() {
        // From Syllabus -> Back -> Dashboard
        val navigator = BackPressNavigator(Screen.Syllabus)
        navigator.handleBackPress(1000L)
        assertFalse("Should not exit when navigating back from internal screen", navigator.exitCalled)
        assertEquals(Screen.Dashboard, navigator.currentScreen)

        // From SubjectDetail -> Back -> Syllabus
        navigator.currentScreen = Screen.SubjectDetail("sub1")
        navigator.handleBackPress(2000L)
        assertFalse(navigator.exitCalled)
        assertEquals(Screen.Syllabus, navigator.currentScreen)

        // From Notes -> Back -> Dashboard
        navigator.currentScreen = Screen.Notes
        navigator.handleBackPress(3000L)
        assertFalse(navigator.exitCalled)
        assertEquals(Screen.Dashboard, navigator.currentScreen)

        // From StudyPlanRoute -> Back -> Dashboard
        navigator.currentScreen = Screen.StudyPlanRoute
        navigator.handleBackPress(4000L)
        assertFalse(navigator.exitCalled)
        assertEquals(Screen.Dashboard, navigator.currentScreen)

        // From AcademicRegister -> Back -> Login
        navigator.currentScreen = Screen.AcademicRegister
        navigator.handleBackPress(5000L)
        assertFalse(navigator.exitCalled)
        assertEquals(Screen.Login, navigator.currentScreen)
    }

    @Test
    fun test5_modeIsolationPreserved() {
        // Verify modes remain completely separate enums
        assertNotEquals(PreparationMode.ACADEMIC, PreparationMode.PROFESSIONAL_GATE)
        assertEquals("ACADEMIC", PreparationMode.ACADEMIC.name)
        assertEquals("PROFESSIONAL_GATE", PreparationMode.PROFESSIONAL_GATE.name)
    }
}

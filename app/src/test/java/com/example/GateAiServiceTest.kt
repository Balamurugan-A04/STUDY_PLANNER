package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GateAiService
import com.example.model.PreparationMode
import com.example.model.SourceType
import com.example.model.StudyPlan
import com.example.model.SubjectData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class GateAiServiceTest {

    private val service = GateAiService()

    @Test
    fun testParseQuestionsJson_validGateJson_parsedCorrectly() {
        val json = """
        {
          "questions": [
            {
              "question": "What is the time complexity of searching an element in a balanced Binary Search Tree (BST) with n nodes?",
              "options": [
                "A) O(1)",
                "B) O(log n)",
                "C) O(n)",
                "D) O(n log n)"
              ],
              "correctAnswer": "B",
              "explanation": "In a balanced BST of n nodes, the height is bounded by O(log n). Therefore, search takes O(log n) time.",
              "difficulty": "Medium",
              "subject": "Programming and Data Structures",
              "subtopic": "Binary Search Trees",
              "marks": 1
            }
          ]
        }
        """.trimIndent()

        val parsed = service.parseQuestionsJson(
            rawJson = json,
            defaultSubject = "Programming and Data Structures",
            defaultTopic = "Binary Search Trees",
            defaultDifficulty = "Medium",
            userId = "gate_student@example.com"
        )

        assertEquals(1, parsed.size)
        val q = parsed[0]
        assertEquals("What is the time complexity of searching an element in a balanced Binary Search Tree (BST) with n nodes?", q.question)
        assertEquals(4, q.options.size)
        assertEquals("O(log n)", q.options[1])
        assertEquals(1, q.correctAnswer) // 'B' -> index 1
        assertEquals("Programming and Data Structures", q.subject)
        assertEquals("Binary Search Trees", q.topic)
        assertEquals(PreparationMode.PROFESSIONAL_GATE, q.mode)
        assertEquals(SourceType.AI_PRACTICE, q.sourceType)
    }

    @Test
    fun testPriorityAlgorithm_importanceTakesPrecedenceOverDifficultyAndExamDate() = runBlocking {
        // Subject A: High Importance, Medium Difficulty, Later Exam
        val subA = SubjectData(
            id = "s_a",
            name = "Subject A",
            importance = "High",
            difficulty = "Medium",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-12-01",
            mode = PreparationMode.PROFESSIONAL_GATE,
            userId = "user1"
        )

        // Subject B: Medium Importance, Hard Difficulty, Earlier Exam
        val subB = SubjectData(
            id = "s_b",
            name = "Subject B",
            importance = "Medium",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-05-01",
            mode = PreparationMode.PROFESSIONAL_GATE,
            userId = "user1"
        )

        val result = service.generateGateStudySchedule(listOf(subB, subA), emptyList(), "user1")
        assertTrue(result.isSuccess)
        val plan = result.getOrNull()!!
        assertNotNull(plan)
        // Subject A must be the first goal because Importance comes first
        assertTrue(plan.dailyGoals.isNotEmpty())
        assertTrue("Subject A must be first priority", plan.dailyGoals[0].contains("Subject A"))
    }

    @Test
    fun testPriorityAlgorithm_difficultyTakesPrecedenceWhenImportanceIsEqual() = runBlocking {
        // Both High Importance
        // Subject A: Easy Difficulty
        val subA = SubjectData(
            id = "s_a",
            name = "Subject A",
            importance = "High",
            difficulty = "Easy",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-05-20",
            mode = PreparationMode.PROFESSIONAL_GATE,
            userId = "user1"
        )

        // Subject B: Hard Difficulty
        val subB = SubjectData(
            id = "s_b",
            name = "Subject B",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-05-30",
            mode = PreparationMode.PROFESSIONAL_GATE,
            userId = "user1"
        )

        val result = service.generateGateStudySchedule(listOf(subA, subB), emptyList(), "user1")
        assertTrue(result.isSuccess)
        val plan = result.getOrNull()!!
        assertNotNull(plan)
        // Subject B must be first priority because it has higher difficulty
        assertTrue(plan.dailyGoals[0].contains("Subject B"))
    }

    @Test
    fun testPriorityAlgorithm_earlierExamDateTakesPrecedenceWhenImportanceAndDifficultyAreEqual() = runBlocking {
        // Both High Importance, Hard Difficulty
        // Subject A: Later Exam
        val subA = SubjectData(
            id = "s_a",
            name = "Subject A",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-06-10",
            mode = PreparationMode.PROFESSIONAL_GATE,
            userId = "user1"
        )

        // Subject B: Earlier Exam
        val subB = SubjectData(
            id = "s_b",
            name = "Subject B",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-06-05",
            mode = PreparationMode.PROFESSIONAL_GATE,
            userId = "user1"
        )

        val result = service.generateGateStudySchedule(listOf(subA, subB), emptyList(), "user1")
        assertTrue(result.isSuccess)
        val plan = result.getOrNull()!!
        assertNotNull(plan)
        // Subject B must be first priority because it has earlier exam date
        assertTrue(plan.dailyGoals[0].contains("Subject B"))
    }

    @Test
    fun testMissedTaskRescheduling_notDeletedAndIncludedInFutureSchedule() = runBlocking {
        val originalGoals = listOf(
            "Engineering Mathematics: Linear Algebra (60 mins)",
            "Data Structures: Trees & Graphs (60 mins)",
            "Computer Networks: Routing Protocols (60 mins)"
        )
        val initialPlan = StudyPlan(
            mode = PreparationMode.PROFESSIONAL_GATE,
            dailyHours = 3,
            targetExam = "GATE CS 2026",
            dailyGoals = originalGoals,
            weeklySchedule = listOf("Mon: Engineering Mathematics", "Tue: Data Structures", "Wed: Computer Networks")
        )

        // User completed goal 0 and goal 2, but MISSED goal 1 ("Data Structures: Trees & Graphs")
        val completedGoals = listOf(originalGoals[0], originalGoals[2])

        val sub = SubjectData(
            id = "s1",
            name = "Data Structures",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-05-15",
            mode = PreparationMode.PROFESSIONAL_GATE,
            userId = "user1"
        )

        val res = service.rescheduleGateMissedTasks(
            currentPlan = initialPlan,
            completedGoalsList = completedGoals,
            subjects = listOf(sub),
            syllabus = emptyList(),
            userId = "user1"
        )

        assertTrue(res.isSuccess)
        val updatedPlan = res.getOrNull()!!

        // 1. Missed task is NOT deleted
        assertEquals(1, updatedPlan.missedGoals.size)
        assertTrue(updatedPlan.missedGoals[0].contains("Data Structures"))

        // 2. Missed task is marked as rescheduled in daily goals
        val hasRescheduled = updatedPlan.dailyGoals.any { it.contains("Data Structures", ignoreCase = true) }
        assertTrue("Missed task must be retained in updated goals", hasRescheduled)

        // 3. Missed task is placed into upcoming weekly schedule as Make-up
        val hasMakeupInSchedule = updatedPlan.weeklySchedule.any { it.contains("Make-up", ignoreCase = true) || it.contains("Data Structures", ignoreCase = true) }
        assertTrue("Missed task must be reallocated in weekly schedule", hasMakeupInSchedule)

        // 4. Completed tasks are preserved
        assertEquals(2, updatedPlan.completedGoals.size)
    }

    @Test
    fun testMissedTaskRescheduling_doesNotOverloadDailyStudyLimit() = runBlocking {
        val originalGoals = listOf(
            "Algorithms: Dynamic Programming (120 mins)",
            "Operating Systems: Semaphores (60 mins)"
        )
        val initialPlan = StudyPlan(
            mode = PreparationMode.PROFESSIONAL_GATE,
            dailyHours = 3, // 3 hours limit = 180 mins
            targetExam = "GATE CS 2026",
            dailyGoals = originalGoals,
            weeklySchedule = listOf("Mon: Algorithms", "Tue: OS")
        )

        // None completed -> both missed
        val completedGoals = emptyList<String>()

        val sub1 = SubjectData(
            id = "s1",
            name = "Algorithms",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 120,
            examDate = "2026-05-15",
            mode = PreparationMode.PROFESSIONAL_GATE,
            userId = "user1"
        )

        val res = service.rescheduleGateMissedTasks(
            currentPlan = initialPlan,
            completedGoalsList = completedGoals,
            subjects = listOf(sub1),
            syllabus = emptyList(),
            userId = "user1"
        )

        assertTrue(res.isSuccess)
        val updatedPlan = res.getOrNull()!!
        assertEquals(3, updatedPlan.dailyHours) // Preserved daily capacity
        // Weekly schedule entries do not exceed 180 mins
        updatedPlan.weeklySchedule.forEach { entry ->
            val minsMatch = Regex("(\\d+)\\s*mins").find(entry)
            if (minsMatch != null) {
                val mins = minsMatch.groupValues[1].toInt()
                assertTrue("Daily schedule item must not exceed available daily capacity", mins <= 180)
            }
        }
    }

    @Test
    fun testGateDataIsolation_neverIncludesAcademicSubjectsInGeneratedPlan() = runBlocking {
        val gateSub1 = SubjectData(
            id = "gate_1",
            name = "Computer Organization and Architecture",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 120,
            examDate = "2026-02-01",
            mode = PreparationMode.PROFESSIONAL_GATE,
            userId = "gate_user1"
        )
        val acadSub = SubjectData(
            id = "acad_1",
            name = "Mobile Application Development",
            importance = "High",
            difficulty = "Medium",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-09-01",
            mode = PreparationMode.ACADEMIC,
            userId = "gate_user1"
        )

        val result = service.generateGateStudySchedule(
            subjects = listOf(gateSub1, acadSub),
            syllabus = emptyList(),
            userId = "gate_user1"
        )

        assertTrue(result.isSuccess)
        val plan = result.getOrNull()!!
        assertEquals(PreparationMode.PROFESSIONAL_GATE, plan.mode)

        // Ensure Academic subjects never leak into GATE daily goals or schedule
        plan.dailyGoals.forEach { goal ->
            assertFalse("Academic subject should not appear in GATE plan", goal.contains("Mobile Application Development", ignoreCase = true))
            assertFalse("Academic keyword should not appear in GATE plan", goal.contains("Academic", ignoreCase = true))
        }
        plan.weeklySchedule.forEach { item ->
            assertFalse("Academic subject should not appear in GATE schedule", item.contains("Mobile Application Development", ignoreCase = true))
            assertFalse("Academic keyword should not appear in GATE schedule", item.contains("Academic", ignoreCase = true))
        }
    }

    @Test
    fun testGateDataIsolation_reschedulingNeverIncludesAcademicData() = runBlocking {
        val initialPlan = StudyPlan(
            mode = PreparationMode.PROFESSIONAL_GATE,
            dailyHours = 4,
            targetExam = "GATE CS 2026",
            dailyGoals = listOf("Operating Systems: CPU Scheduling (120 mins)"),
            weeklySchedule = listOf("Mon: Operating Systems")
        )

        val gateSub = SubjectData(
            id = "gate_1",
            name = "Operating Systems",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 120,
            examDate = "2026-02-01",
            mode = PreparationMode.PROFESSIONAL_GATE,
            userId = "gate_user1"
        )
        val acadSub = SubjectData(
            id = "acad_1",
            name = "Cloud Computing",
            importance = "High",
            difficulty = "Medium",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-09-01",
            mode = PreparationMode.ACADEMIC,
            userId = "gate_user1"
        )

        val res = service.rescheduleGateMissedTasks(
            currentPlan = initialPlan,
            completedGoalsList = emptyList(),
            subjects = listOf(gateSub, acadSub),
            syllabus = emptyList(),
            userId = "gate_user1"
        )

        assertTrue(res.isSuccess)
        val plan = res.getOrNull()!!
        assertEquals(PreparationMode.PROFESSIONAL_GATE, plan.mode)

        plan.dailyGoals.forEach { goal ->
            assertFalse("Academic subject should not appear in GATE rescheduled plan", goal.contains("Cloud Computing", ignoreCase = true))
            assertFalse("Academic keyword should not appear in GATE rescheduled plan", goal.contains("Academic", ignoreCase = true))
        }
        plan.weeklySchedule.forEach { item ->
            assertFalse("Academic subject should not appear in GATE rescheduled schedule", item.contains("Cloud Computing", ignoreCase = true))
            assertFalse("Academic keyword should not appear in GATE rescheduled schedule", item.contains("Academic", ignoreCase = true))
        }
    }

    @Test
    fun testGateAiTutor_matchesGate2024Question() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = service.askGateAiTutor(
            context = context,
            query = "If '→' denotes increasing order of intensity, then the meaning of the words"
        )
        assertTrue("Query should succeed", result.isSuccess)
        val text = result.getOrNull()!!
        assertTrue("Should start with Correct Answer: C", text.startsWith("Correct Answer: C"))
        assertTrue("Should contain Explanation:", text.contains("Explanation:"))
        assertTrue("Should contain Solution:", text.contains("Solution:"))
    }

    @Test
    fun testGateAiTutor_matchesGate2023Question() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = service.askGateAiTutor(
            context = context,
            query = "We reached the station late, and ______ missed the train."
        )
        assertTrue("Query should succeed", result.isSuccess)
        val text = result.getOrNull()!!
        assertTrue("Should contain correct option B", text.startsWith("Correct Answer: B"))
        assertTrue("Should contain Explanation:", text.contains("Explanation:"))
        assertTrue("Should contain Solution:", text.contains("Solution:"))
    }

    @Test
    fun testGateAiTutor_matchesGate2022Question() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = service.askGateAiTutor(
            context = context,
            query = "The _____ is too high for it to be considered _____."
        )
        assertTrue("Query should succeed", result.isSuccess)
        val text = result.getOrNull()!!
        assertTrue("Should contain correct option D", text.startsWith("Correct Answer: D"))
        assertTrue("Should contain Explanation:", text.contains("Explanation:"))
        assertTrue("Should contain Solution:", text.contains("Solution:"))
    }

    @Test
    fun testGateAiTutor_matchesGate2025Question() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = service.askGateAiTutor(
            context = context,
            query = "Ravi had ______ younger brother who taught at ______ university."
        )
        assertTrue("Query should succeed", result.isSuccess)
        val text = result.getOrNull()!!
        assertTrue("Should contain correct option A", text.startsWith("Correct Answer: A"))
        assertTrue("Should contain Explanation:", text.contains("Explanation:"))
        assertTrue("Should contain Solution:", text.contains("Solution:"))
    }

    @Test
    fun testGateAiTutor_matchesMsqQuestion_returnsAllCorrectOptions() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = service.askGateAiTutor(
            context = context,
            query = "Node X has a TCP connection open to node Y through intermediate routers"
        )
        assertTrue("Query should succeed", result.isSuccess)
        val text = result.getOrNull()!!
        assertTrue("Should return all correct options B, D", text.startsWith("Correct Answer: B, D"))
        assertTrue("Should contain Explanation:", text.contains("Explanation:"))
        assertTrue("Should contain Solution:", text.contains("Solution:"))
    }

    @Test
    fun testGateAiTutor_matchesNatQuestion_returnsNumericalAnswer() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = service.askGateAiTutor(
            context = context,
            query = "Let P be the partial order defined on {1,2,3,4} as P = {(x,x"
        )
        assertTrue("Query should succeed", result.isSuccess)
        val text = result.getOrNull()!!
        assertTrue("Should return numerical answer 5", text.startsWith("Correct Answer: 5"))
        assertTrue("Should contain Explanation:", text.contains("Explanation:"))
        assertTrue("Should contain Solution:", text.contains("Solution:"))
    }

    @Test
    fun testGateAiTutor_copyPastedFromMockTestWithPrefix() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = service.askGateAiTutor(
            context = context,
            query = "Q1. We reached the station late, and ______ missed the train.\n(A) nearly\n(B) scarcely\n(C) utterly\n(D) mostly"
        )
        assertTrue("Query should succeed", result.isSuccess)
        val text = result.getOrNull()!!
        assertTrue("Should find match and return option B", text.startsWith("Correct Answer: B"))
        assertTrue("Should contain Explanation:", text.contains("Explanation:"))
        assertTrue("Should contain Solution:", text.contains("Solution:"))
    }
}

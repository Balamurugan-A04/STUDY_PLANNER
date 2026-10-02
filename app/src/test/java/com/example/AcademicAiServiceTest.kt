package com.example

import com.example.data.AcademicAiService
import com.example.model.PreparationMode
import com.example.model.SourceType
import com.example.model.StudyPlan
import com.example.model.SubjectData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AcademicAiServiceTest {

    private val service = AcademicAiService()

    @Test
    fun testParseQuestionsJson_validJson_parsedCorrectly() {
        val sampleJson = """
        {
          "questions": [
            {
              "question": "Which cloud service model provides virtualized computing resources over the internet?",
              "options": [
                "A) Infrastructure as a Service (IaaS)",
                "B) Platform as a Service (PaaS)",
                "C) Software as a Service (SaaS)",
                "D) Network as a Service (NaaS)"
              ],
              "correctAnswer": "A",
              "explanation": "IaaS delivers virtualized hardware resources such as virtual machines, storage, and networking.",
              "difficulty": "Medium",
              "subject": "Cloud Computing",
              "subtopic": "Cloud Service Models"
            },
            {
              "question": "In which model does the customer only use pre-built applications?",
              "options": [
                "IaaS",
                "PaaS",
                "SaaS",
                "BaaS"
              ],
              "correctAnswer": "C",
              "explanation": "SaaS delivers ready-to-use software applications over the web.",
              "difficulty": "Easy",
              "subject": "Cloud Computing",
              "subtopic": "Cloud Service Models"
            }
          ]
        }
        """.trimIndent()

        val parsed = service.parseQuestionsJson(
            rawJson = sampleJson,
            defaultSubject = "Cloud Computing",
            defaultTopic = "Cloud Service Models",
            defaultDifficulty = "Medium",
            userId = "student@example.com"
        )

        assertEquals(2, parsed.size)

        val q1 = parsed[0]
        assertEquals("Which cloud service model provides virtualized computing resources over the internet?", q1.question)
        assertEquals(4, q1.options.size)
        assertEquals("Infrastructure as a Service (IaaS)", q1.options[0])
        assertEquals(0, q1.correctAnswer) // 'A' -> index 0
        assertEquals("Cloud Computing", q1.subject)
        assertEquals("Cloud Service Models", q1.topic)
        assertEquals(PreparationMode.ACADEMIC, q1.mode)
        assertEquals(SourceType.AI_PRACTICE, q1.sourceType)

        val q2 = parsed[1]
        assertEquals(2, q2.correctAnswer) // 'C' -> index 2
        assertEquals(PreparationMode.ACADEMIC, q2.mode)
    }

    @Test
    fun testParseQuestionsJson_specificSubjectDay_parsedWithCorrectSubjectAndSubtopic() {
        val sampleJson = """
        {
          "questions": [
            {
              "question": "What is the formula for calculating the arithmetic mean?",
              "options": [
                "Sum of observations divided by total number of observations",
                "Product of all observations",
                "Middle observation in sorted array",
                "Most frequent observation"
              ],
              "correctAnswer": "A",
              "explanation": "The mean is calculated as the sum of all data values divided by the count of data values.",
              "difficulty": "Medium",
              "subject": "day",
              "subtopic": "Mean"
            }
          ]
        }
        """.trimIndent()

        val parsed = service.parseQuestionsJson(
            rawJson = sampleJson,
            defaultSubject = "day",
            defaultTopic = "Mean",
            defaultDifficulty = "Medium",
            userId = "student@example.com"
        )

        assertEquals(1, parsed.size)
        val q = parsed[0]
        assertEquals("day", q.subject)
        assertEquals("Mean", q.topic)
        assertEquals(0, q.correctAnswer)
        assertEquals(PreparationMode.ACADEMIC, q.mode)
    }

    @Test
    fun testParseQuestionsJson_withMarkdownFences_parsedCorrectly() {
        val markdownJson = """
        ```json
        {
          "questions": [
            {
              "question": "What is Mobile Application Development (MAD)?",
              "options": [
                "Creating software for mobile devices",
                "Building desktop servers only",
                "Configuring hardware routers",
                "Managing physical databases"
              ],
              "correctAnswer": "A",
              "explanation": "MAD refers to developing software for mobile platforms like Android and iOS.",
              "difficulty": "Easy",
              "subject": "MAD",
              "subtopic": "Introduction"
            }
          ]
        }
        ```
        """.trimIndent()

        val parsed = service.parseQuestionsJson(
            rawJson = markdownJson,
            defaultSubject = "MAD",
            defaultTopic = "Introduction",
            defaultDifficulty = "Easy",
            userId = "student@example.com"
        )

        assertEquals(1, parsed.size)
        assertEquals("What is Mobile Application Development (MAD)?", parsed[0].question)
        assertEquals(0, parsed[0].correctAnswer)
        assertEquals(PreparationMode.ACADEMIC, parsed[0].mode)
    }

    @Test
    fun testParseQuestionsJson_invalidOrEmpty_returnsEmptyList() {
        val parsed = service.parseQuestionsJson(
            rawJson = "Invalid response from server",
            defaultSubject = "Math",
            defaultTopic = "Stats",
            defaultDifficulty = "Medium",
            userId = "student@example.com"
        )

        assertTrue(parsed.isEmpty())
    }

    // =========================================================================
    // ACADEMIC STUDY PLANNER TESTS
    // =========================================================================

    @Test
    fun testAcademicPriority_importanceTakesPrecedenceOverDifficultyAndExamDate() = runBlocking {
        // Subject A: High Importance, Easy Difficulty, Later Exam Date
        val subA = SubjectData(
            id = "acad_a",
            name = "Subject A",
            importance = "High",
            difficulty = "Easy",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-11-20",
            mode = PreparationMode.ACADEMIC,
            userId = "student1"
        )

        // Subject B: Medium Importance, Hard Difficulty, Earlier Exam Date
        val subB = SubjectData(
            id = "acad_b",
            name = "Subject B",
            importance = "Medium",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-05-10",
            mode = PreparationMode.ACADEMIC,
            userId = "student1"
        )

        val result = service.generateAcademicStudySchedule(listOf(subB, subA), emptyList(), "student1")
        assertTrue(result.isSuccess)
        val plan = result.getOrNull()!!
        assertNotNull(plan)
        assertEquals(PreparationMode.ACADEMIC, plan.mode)
        assertTrue(plan.dailyGoals.isNotEmpty())
        // Subject A must be first priority because Importance is the first preference
        assertTrue("Subject A must receive higher priority than Subject B", plan.dailyGoals[0].contains("Subject A"))
    }

    @Test
    fun testAcademicPriority_difficultyTakesPrecedenceWhenImportanceIsEqual() = runBlocking {
        // Both High Importance
        // Subject A: Easy Difficulty
        val subA = SubjectData(
            id = "acad_a",
            name = "Subject A",
            importance = "High",
            difficulty = "Easy",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-06-15",
            mode = PreparationMode.ACADEMIC,
            userId = "student1"
        )

        // Subject B: Hard Difficulty
        val subB = SubjectData(
            id = "acad_b",
            name = "Subject B",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-06-30",
            mode = PreparationMode.ACADEMIC,
            userId = "student1"
        )

        val result = service.generateAcademicStudySchedule(listOf(subA, subB), emptyList(), "student1")
        assertTrue(result.isSuccess)
        val plan = result.getOrNull()!!
        assertNotNull(plan)
        // Subject B must receive higher priority because it has higher difficulty
        assertTrue("Subject B must be first priority due to higher difficulty", plan.dailyGoals[0].contains("Subject B"))
    }

    @Test
    fun testAcademicPriority_earlierExamDateTakesPrecedenceWhenImportanceAndDifficultyAreEqual() = runBlocking {
        // Both High Importance, Hard Difficulty
        // Subject A: Later Exam
        val subA = SubjectData(
            id = "acad_a",
            name = "Subject A",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-08-20",
            mode = PreparationMode.ACADEMIC,
            userId = "student1"
        )

        // Subject B: Earlier Exam
        val subB = SubjectData(
            id = "acad_b",
            name = "Subject B",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-08-05",
            mode = PreparationMode.ACADEMIC,
            userId = "student1"
        )

        val result = service.generateAcademicStudySchedule(listOf(subA, subB), emptyList(), "student1")
        assertTrue(result.isSuccess)
        val plan = result.getOrNull()!!
        assertNotNull(plan)
        // Subject B must receive higher priority because it has earlier exam date
        assertTrue("Subject B must be first priority due to earlier exam date", plan.dailyGoals[0].contains("Subject B"))
    }

    @Test
    fun testAcademicMissedTaskRescheduling_notDeletedAndIncludedInFutureSchedule() = runBlocking {
        val originalGoals = listOf(
            "Database Systems: Normalization (60 mins)",
            "Web Tech: REST APIs (60 mins)",
            "Software Eng: Agile Models (60 mins)"
        )
        val initialPlan = StudyPlan(
            mode = PreparationMode.ACADEMIC,
            dailyHours = 3,
            targetExam = "Semester Finals",
            dailyGoals = originalGoals,
            weeklySchedule = listOf("Mon: Database Systems", "Tue: Web Tech", "Wed: Software Eng")
        )

        // Completed goal 0 & goal 2, but MISSED goal 1 ("Web Tech: REST APIs")
        val completedGoals = listOf(originalGoals[0], originalGoals[2])

        val sub = SubjectData(
            id = "acad_s1",
            name = "Web Tech",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-06-01",
            mode = PreparationMode.ACADEMIC,
            userId = "student1"
        )

        val res = service.rescheduleAcademicMissedTasks(
            currentPlan = initialPlan,
            completedGoalsList = completedGoals,
            subjects = listOf(sub),
            syllabus = emptyList(),
            userId = "student1"
        )

        assertTrue(res.isSuccess)
        val updatedPlan = res.getOrNull()!!

        // 1. Missed task is preserved
        assertEquals(1, updatedPlan.missedGoals.size)
        assertTrue(updatedPlan.missedGoals[0].contains("Web Tech"))

        // 2. Retained with [RESCHEDULED] in updated daily goals
        val hasRescheduled = updatedPlan.dailyGoals.any { it.contains("Web Tech", ignoreCase = true) }
        assertTrue("Missed task must be retained in updated daily goals", hasRescheduled)

        // 3. Allocated in weekly schedule as [Make-up]
        val hasMakeupInSchedule = updatedPlan.weeklySchedule.any { it.contains("Make-up", ignoreCase = true) || it.contains("Web Tech", ignoreCase = true) }
        assertTrue("Missed task must be reallocated in weekly schedule", hasMakeupInSchedule)

        // 4. Completed tasks remain completed
        assertEquals(2, updatedPlan.completedGoals.size)
    }

    @Test
    fun testAcademicMissedTaskRescheduling_doesNotOverloadDailyStudyLimit() = runBlocking {
        val originalGoals = listOf(
            "Machine Learning: Linear Regression (120 mins)",
            "Cloud Computing: Virtualization (60 mins)"
        )
        val initialPlan = StudyPlan(
            mode = PreparationMode.ACADEMIC,
            dailyHours = 3, // 3 hours = 180 mins limit
            targetExam = "Semester Finals",
            dailyGoals = originalGoals,
            weeklySchedule = listOf("Mon: Machine Learning", "Tue: Cloud Computing")
        )

        val completedGoals = emptyList<String>()

        val sub1 = SubjectData(
            id = "acad_s1",
            name = "Machine Learning",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 120,
            examDate = "2026-07-10",
            mode = PreparationMode.ACADEMIC,
            userId = "student1"
        )

        val res = service.rescheduleAcademicMissedTasks(
            currentPlan = initialPlan,
            completedGoalsList = completedGoals,
            subjects = listOf(sub1),
            syllabus = emptyList(),
            userId = "student1"
        )

        assertTrue(res.isSuccess)
        val updatedPlan = res.getOrNull()!!
        assertEquals(3, updatedPlan.dailyHours)
        updatedPlan.weeklySchedule.forEach { entry ->
            val minsMatch = Regex("(\\d+)\\s*mins").find(entry)
            if (minsMatch != null) {
                val mins = minsMatch.groupValues[1].toInt()
                assertTrue("Daily schedule item must not exceed available daily capacity of 180 mins", mins <= 180)
            }
        }
    }

    @Test
    fun testAcademicDataIsolation_neverIncludesGateSubjectsInGeneratedPlan() = runBlocking {
        val academicSub1 = SubjectData(
            id = "acad_1",
            name = "Mobile Application Development",
            importance = "High",
            difficulty = "Medium",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-09-01",
            mode = PreparationMode.ACADEMIC,
            userId = "student1"
        )
        val gateSub = SubjectData(
            id = "gate_1",
            name = "Theory of Computation",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 120,
            examDate = "2026-02-01",
            mode = PreparationMode.PROFESSIONAL_GATE,
            userId = "student1"
        )

        val result = service.generateAcademicStudySchedule(
            subjects = listOf(academicSub1, gateSub),
            syllabus = emptyList(),
            userId = "student1"
        )

        assertTrue(result.isSuccess)
        val plan = result.getOrNull()!!
        assertEquals(PreparationMode.ACADEMIC, plan.mode)

        // Ensure GATE subjects never leak into Academic daily goals or schedule
        plan.dailyGoals.forEach { goal ->
            assertFalse("GATE subject should not appear in Academic plan", goal.contains("Theory of Computation", ignoreCase = true))
            assertFalse("GATE keyword should not appear in Academic plan", goal.contains("GATE", ignoreCase = true))
        }
        plan.weeklySchedule.forEach { item ->
            assertFalse("GATE subject should not appear in Academic schedule", item.contains("Theory of Computation", ignoreCase = true))
            assertFalse("GATE keyword should not appear in Academic schedule", item.contains("GATE", ignoreCase = true))
        }
    }

    @Test
    fun testAcademicDataIsolation_reschedulingNeverIncludesGateData() = runBlocking {
        val initialPlan = StudyPlan(
            mode = PreparationMode.ACADEMIC,
            dailyHours = 2,
            targetExam = "Academic Exam",
            dailyGoals = listOf("Cloud Computing: AWS EC2 (60 mins)"),
            weeklySchedule = listOf("Mon: Cloud Computing")
        )

        val academicSub = SubjectData(
            id = "acad_1",
            name = "Cloud Computing",
            importance = "High",
            difficulty = "Medium",
            availableStudyMinutesPerDay = 60,
            examDate = "2026-09-01",
            mode = PreparationMode.ACADEMIC,
            userId = "student1"
        )
        val gateSub = SubjectData(
            id = "gate_1",
            name = "Digital Logic",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 120,
            examDate = "2026-02-01",
            mode = PreparationMode.PROFESSIONAL_GATE,
            userId = "student1"
        )

        val res = service.rescheduleAcademicMissedTasks(
            currentPlan = initialPlan,
            completedGoalsList = emptyList(),
            subjects = listOf(academicSub, gateSub),
            syllabus = emptyList(),
            userId = "student1"
        )

        assertTrue(res.isSuccess)
        val plan = res.getOrNull()!!
        assertEquals(PreparationMode.ACADEMIC, plan.mode)

        plan.dailyGoals.forEach { goal ->
            assertFalse("GATE subject should not appear in Academic rescheduled plan", goal.contains("Digital Logic", ignoreCase = true))
            assertFalse("GATE keyword should not appear in Academic rescheduled plan", goal.contains("GATE", ignoreCase = true))
        }
        plan.weeklySchedule.forEach { item ->
            assertFalse("GATE subject should not appear in Academic rescheduled schedule", item.contains("Digital Logic", ignoreCase = true))
            assertFalse("GATE keyword should not appear in Academic rescheduled schedule", item.contains("GATE", ignoreCase = true))
        }
    }
}

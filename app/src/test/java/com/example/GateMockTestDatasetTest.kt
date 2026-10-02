package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CsvQuestionParser
import com.example.model.PreparationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GateMockTestDatasetTest {

    @Test
    fun testGateMockTestDatasetExpandedPool() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val questions = CsvQuestionParser.parseAllGateQuestions(context)

        // 1. Verify total questions count is 260 (65 original + 195 from previous 3 years)
        assertEquals("Total GATE questions must be 260", 260, questions.size)

        // 2. Verify all question IDs are unique
        val uniqueIds = questions.map { it.id }.toSet()
        assertEquals("All question IDs must be unique", 260, uniqueIds.size)

        // 3. Verify all normalized question texts are unique (no duplicate questions)
        val uniqueTexts = questions.map { CsvQuestionParser.normalizeQuestionText(it.question) }.toSet()
        assertEquals("All normalized question texts must be unique", 260, uniqueTexts.size)

        // 4. Verify original 65 questions are preserved first
        val first65 = questions.take(65)
        assertEquals(65, first65.size)
        assertTrue(first65.first().id.contains("GATE2025") || first65.first().id.contains("GATE_JSON"))

        // 5. Verify previous 3-year questions exist
        val has2024 = questions.any { it.year == "2024" }
        val has2023 = questions.any { it.year == "2023" }
        val has2022 = questions.any { it.year == "2022" }
        assertTrue("Must contain 2024 questions", has2024)
        assertTrue("Must contain 2023 questions", has2023)
        assertTrue("Must contain 2022 questions", has2022)

        // 6. Verify strict isolation: all questions have mode PROFESSIONAL_GATE
        val allGateMode = questions.all { it.mode == PreparationMode.PROFESSIONAL_GATE }
        assertTrue("All questions must belong strictly to PROFESSIONAL_GATE mode", allGateMode)

        // 7. Verify subject filtering works across the expanded pool
        val osQuestions = questions.filter { it.subject.equals("Operating Systems", ignoreCase = true) }
        assertTrue("Must have multiple Operating Systems questions", osQuestions.size >= 20)

        val cnQuestions = questions.filter { it.subject.equals("Computer Networks", ignoreCase = true) }
        assertTrue("Must have multiple Computer Networks questions", cnQuestions.size >= 20)

        val algoQuestions = questions.filter { it.subject.equals("Algorithms", ignoreCase = true) }
        assertTrue("Must have multiple Algorithms questions", algoQuestions.size >= 20)

        val dbQuestions = questions.filter { it.subject.equals("Databases", ignoreCase = true) }
        assertTrue("Must have multiple Databases questions", dbQuestions.size >= 15)

        val gaQuestions = questions.filter { it.subject.equals("General Aptitude", ignoreCase = true) }
        assertEquals("Must have exactly 40 General Aptitude questions (10 per year * 4 years)", 40, gaQuestions.size)

        // 8. Verify answers and explanations are preserved
        questions.forEach { q ->
            assertTrue("Question text must not be blank", q.question.isNotBlank())
            assertTrue("Explanation must not be blank", q.explanation.isNotBlank())
            assertTrue("Explanation must contain verified correct answer", q.explanation.contains("Verified Correct Answer:"))
            if (q.options.isNotEmpty()) {
                assertTrue("MCQ/MSQ options must have at least 2 options", q.options.size >= 2)
            }
        }
    }
}

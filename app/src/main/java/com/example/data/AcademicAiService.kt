package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.model.PreparationMode
import com.example.model.QuestionData
import com.example.model.SourceType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AcademicChatMessageDto(
    @param:Json(name = "role") val role: String,
    @param:Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class AcademicChatCompletionRequest(
    @param:Json(name = "model") val model: String = BuildConfig.NVIDIA_MODEL.ifBlank { "openai/gpt-oss-20b" },
    @param:Json(name = "messages") val messages: List<AcademicChatMessageDto>,
    @param:Json(name = "temperature") val temperature: Double = 0.5,
    @param:Json(name = "top_p") val topP: Double = 1.0,
    @param:Json(name = "max_tokens") val maxTokens: Int = 4096,
    @param:Json(name = "stream") val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AcademicChoiceMessageDto(
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "content") val content: String? = null,
    @param:Json(name = "reasoning_content") val reasoningContent: String? = null
)

@JsonClass(generateAdapter = true)
data class AcademicChoiceDto(
    @param:Json(name = "index") val index: Int? = null,
    @param:Json(name = "message") val message: AcademicChoiceMessageDto? = null,
    @param:Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class AcademicChatCompletionResponse(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "choices") val choices: List<AcademicChoiceDto>? = null
)

interface AcademicOpenAiApi {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: AcademicChatCompletionRequest
    ): AcademicChatCompletionResponse
}

class AcademicAiService {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl: String = if (BuildConfig.NVIDIA_BASE_URL.isNotBlank()) {
        val url = BuildConfig.NVIDIA_BASE_URL.trim()
        if (url.endsWith("/")) url else "$url/"
    } else {
        "https://integrate.api.nvidia.com/v1/"
    }

    private val api: AcademicOpenAiApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(AcademicOpenAiApi::class.java)

    private fun getAuthHeader(): String {
        val apiKey = BuildConfig.NVIDIA_API_KEY.trim()
        return if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"
    }

    suspend fun askAcademicTutor(
        query: String,
        subject: String? = null,
        subtopic: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.success("Please enter a question.")
        }

        val subjectContext = if (!subject.isNullOrBlank() && subject != "All" && subject != "All Subjects") {
            subject.trim()
        } else {
            "Academic Coursework"
        }

        val subtopicContext = if (!subtopic.isNullOrBlank() && subtopic != "All" && subtopic != "All Subtopics") {
            subtopic.trim()
        } else {
            "General Core Concepts"
        }

        val systemPrompt = """
            You are an expert Academic AI Tutor assisting a college or university student.
            Mode: Academic
            Selected Subject: $subjectContext
            Selected Subtopic: $subtopicContext

            Instructions:
            1. Answer the student's question specifically within the context of '$subjectContext' and '$subtopicContext'.
            2. Make explanations clear, accurate, insightful, and student-friendly.
            3. DO NOT include any GATE exam information, GATE questions, or mentions of GATE.
            4. Structure your response clearly using the following format:

            Answer:
            <direct, precise answer>

            Explanation:
            <clear explanation of the concept>

            Example:
            <practical example or code/mathematical illustration if helpful>

            Key Point:
            <critical takeaway or key point for examinations>
        """.trimIndent()

        val messages = listOf(
            AcademicChatMessageDto(role = "system", content = systemPrompt),
            AcademicChatMessageDto(role = "user", content = trimmed)
        )

        try {
            val response = api.createChatCompletion(
                authorization = getAuthHeader(),
                request = AcademicChatCompletionRequest(
                    model = BuildConfig.NVIDIA_MODEL.ifBlank { "openai/gpt-oss-20b" },
                    messages = messages,
                    temperature = 0.6,
                    maxTokens = 2048
                )
            )

            val choice = response.choices?.firstOrNull()
            val content = choice?.message?.content?.trim()
            if (!content.isNullOrBlank()) {
                return@withContext Result.success(content)
            }

            val reasoning = choice?.message?.reasoningContent?.trim()
            if (!reasoning.isNullOrBlank()) {
                return@withContext Result.success(reasoning)
            }

            Result.failure(Exception("Empty response received from AI model."))
        } catch (e: Exception) {
            Log.e("AcademicAiService", "Error calling Academic AI Tutor", e)
            Result.failure(e)
        }
    }

    suspend fun generateAcademicMockQuestions(
        subject: String,
        subtopics: List<String>,
        count: Int,
        difficulty: String = "Medium",
        userId: String = ""
    ): Result<List<QuestionData>> = withContext(Dispatchers.IO) {
        val cleanSubject = if (subject.isNotBlank() && subject != "All" && subject != "All Subjects" && subject != "Full Syllabus") {
            subject.trim()
        } else {
            "Academic Core"
        }

        val cleanSubtopics = if (subtopics.isNotEmpty()) {
            subtopics.joinToString(", ")
        } else {
            "Core Syllabus Concepts"
        }

        val allQuestions = mutableListOf<QuestionData>()
        val batchSize = if (count <= 10) count else 10
        var needed = count
        var attempts = 0
        val maxAttempts = 3

        while (needed > 0 && attempts < maxAttempts) {
            val currentBatchCount = minOf(needed, batchSize)
            val batchResult = fetchQuestionsBatch(cleanSubject, cleanSubtopics, currentBatchCount, difficulty, userId, allQuestions.size)
            if (batchResult.isNotEmpty()) {
                allQuestions.addAll(batchResult)
                needed = count - allQuestions.size
            }
            attempts++
        }

        if (allQuestions.isNotEmpty()) {
            val finalQuestions = allQuestions.distinctBy { it.question.trim() }.take(count)
            return@withContext Result.success(finalQuestions)
        }

        Result.failure(Exception("Failed to generate Academic mock questions from AI."))
    }

    private suspend fun fetchQuestionsBatch(
        subject: String,
        subtopicsStr: String,
        batchCount: Int,
        difficulty: String,
        userId: String,
        startIndex: Int
    ): List<QuestionData> {
        val systemPrompt = """
            You are an expert Academic Exam Creator.
            Mode: Academic
            Subject: $subject
            Subtopics: $subtopicsStr
            Question Count: $batchCount
            Difficulty: $difficulty

            Generate exactly $batchCount high-quality multiple choice questions testing academic mastery of $subject ($subtopicsStr).
            DO NOT generate GATE questions, GATE PYQs, or mention GATE.

            You MUST return ONLY a valid JSON object matching this schema without any markdown formatting, code fences, or surrounding text:
            {
              "questions": [
                {
                  "question": "Question statement here",
                  "options": [
                    "Option A text",
                    "Option B text",
                    "Option C text",
                    "Option D text"
                  ],
                  "correctAnswer": "A",
                  "explanation": "Clear explanation of the correct option",
                  "difficulty": "$difficulty",
                  "subject": "$subject",
                  "subtopic": "Specific subtopic name"
                }
              ]
            }
        """.trimIndent()

        val userPrompt = "Generate $batchCount Academic multiple choice questions for subject '$subject' on topic '$subtopicsStr'."

        val messages = listOf(
            AcademicChatMessageDto(role = "system", content = systemPrompt),
            AcademicChatMessageDto(role = "user", content = userPrompt)
        )

        return try {
            val response = api.createChatCompletion(
                authorization = getAuthHeader(),
                request = AcademicChatCompletionRequest(
                    model = BuildConfig.NVIDIA_MODEL.ifBlank { "openai/gpt-oss-20b" },
                    messages = messages,
                    temperature = 0.4,
                    maxTokens = 4000
                )
            )

            val content = response.choices?.firstOrNull()?.message?.content?.trim() ?: ""
            parseQuestionsJson(content, subject, subtopicsStr, difficulty, userId, startIndex)
        } catch (e: Exception) {
            Log.e("AcademicAiService", "Failed to fetch question batch", e)
            emptyList()
        }
    }

    fun parseQuestionsJson(
        rawJson: String,
        defaultSubject: String,
        defaultTopic: String,
        defaultDifficulty: String,
        userId: String,
        startIndex: Int = 0
    ): List<QuestionData> {
        val result = mutableListOf<QuestionData>()
        try {
            var cleaned = rawJson.replace("```json", "").replace("```", "").trim()
            val firstBrace = cleaned.indexOf('{')
            val lastBrace = cleaned.lastIndexOf('}')
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                cleaned = cleaned.substring(firstBrace, lastBrace + 1)
            } else {
                val firstBracket = cleaned.indexOf('[')
                val lastBracket = cleaned.lastIndexOf(']')
                if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
                    cleaned = "{\"questions\": ${cleaned.substring(firstBracket, lastBracket + 1)}}"
                }
            }

            val root = JSONObject(cleaned)
            val questionsArray = root.optJSONArray("questions") ?: return emptyList()

            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.optJSONObject(i) ?: continue
                val questionText = qObj.optString("question", "").trim()
                if (questionText.isBlank()) continue

                val options = mutableListOf<String>()
                val optArray = qObj.optJSONArray("options")
                if (optArray != null && optArray.length() > 0) {
                    for (j in 0 until optArray.length()) {
                        val opt = optArray.optString(j, "").trim()
                        if (opt.isNotBlank()) options.add(opt)
                    }
                } else {
                    val optA = qObj.optString("optionA", "").trim()
                    val optB = qObj.optString("optionB", "").trim()
                    val optC = qObj.optString("optionC", "").trim()
                    val optD = qObj.optString("optionD", "").trim()
                    if (optA.isNotBlank() && optB.isNotBlank() && optC.isNotBlank() && optD.isNotBlank()) {
                        options.addAll(listOf(optA, optB, optC, optD))
                    }
                }

                if (options.size != 4) continue

                val cleanOptions = options.map { opt ->
                    opt.replace(Regex("^[A-Da-d][\\)\\.\\:\\-]\\s*"), "").trim()
                }

                val correctAnsRaw = qObj.opt("correctAnswer")
                val correctIndex: Int = when (correctAnsRaw) {
                    is Int -> correctAnsRaw.coerceIn(0, 3)
                    is String -> {
                        val s = correctAnsRaw.trim()
                        when (s.uppercase()) {
                            "A", "0" -> 0
                            "B", "1" -> 1
                            "C", "2" -> 2
                            "D", "3" -> 3
                            else -> {
                                val matchIdx = cleanOptions.indexOfFirst { it.equals(s, ignoreCase = true) }
                                if (matchIdx != -1) matchIdx else 0
                            }
                        }
                    }
                    else -> 0
                }

                val explanation = qObj.optString("explanation", "Verified academic explanation.").trim()
                val qSubject = qObj.optString("subject", defaultSubject).trim().ifEmpty { defaultSubject }
                val qSubtopic = qObj.optString("subtopic", qObj.optString("topic", defaultTopic)).trim().ifEmpty { defaultTopic }
                val difficulty = qObj.optString("difficulty", defaultDifficulty).trim().ifEmpty { defaultDifficulty }

                val qIdx = startIndex + i
                result.add(
                    QuestionData(
                        id = "acad_ai_${System.currentTimeMillis()}_$qIdx",
                        year = "2024",
                        subject = qSubject,
                        topic = qSubtopic,
                        question = questionText,
                        options = cleanOptions,
                        correctAnswer = correctIndex,
                        explanation = explanation,
                        difficulty = difficulty,
                        sourceType = SourceType.AI_PRACTICE,
                        mode = PreparationMode.ACADEMIC,
                        userId = userId
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("AcademicAiService", "Failed to parse Academic questions JSON", e)
        }
        return result
    }

    // =========================================================================
    // 3. ACADEMIC AI STUDY SCHEDULE PLANNER
    // =========================================================================
    /**
     * Mandatory Priority Order for Academic Mode:
     * 1. Important Level (High > Medium > Low)
     * 2. Difficulty Level (Hard > Medium > Easy)
     * 3. Exam Date (Earliest date first)
     */
    suspend fun generateAcademicStudySchedule(
        subjects: List<com.example.model.SubjectData>,
        syllabus: List<com.example.model.SyllabusData>,
        userId: String
    ): Result<com.example.model.StudyPlan> = withContext(Dispatchers.IO) {
        val academicSubjects = subjects.filter { it.mode == PreparationMode.ACADEMIC }
        if (academicSubjects.isEmpty()) {
            val emptyPlan = com.example.model.StudyPlan(
                mode = PreparationMode.ACADEMIC,
                dailyHours = 0,
                targetExam = "Academic Preparation",
                dailyGoals = emptyList(),
                weeklySchedule = emptyList(),
                completedGoals = emptyList(),
                missedGoals = emptyList()
            )
            return@withContext Result.success(emptyPlan)
        }

        fun impRank(imp: String?) = when (imp?.trim()?.lowercase()) {
            "high" -> 3
            "medium" -> 2
            "low" -> 1
            else -> 2
        }

        fun diffRank(diff: String?) = when (diff?.trim()?.lowercase()) {
            "hard" -> 3
            "medium" -> 2
            "easy" -> 1
            else -> 2
        }

        fun parseDateMillis(dateStr: String?): Long {
            if (dateStr.isNullOrBlank()) return Long.MAX_VALUE
            return try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                sdf.parse(dateStr.trim())?.time ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
        }

        // DETERMINISTIC SORTING: Importance -> Difficulty -> Exam Date
        val sortedSubjects = academicSubjects.sortedWith { s1, s2 ->
            val i1 = impRank(s1.importance)
            val i2 = impRank(s2.importance)
            if (i1 != i2) return@sortedWith i2.compareTo(i1)

            val d1 = diffRank(s1.difficulty)
            val d2 = diffRank(s2.difficulty)
            if (d1 != d2) return@sortedWith d2.compareTo(d1)

            val t1 = parseDateMillis(s1.examDate)
            val t2 = parseDateMillis(s2.examDate)
            if (t1 != t2) return@sortedWith t1.compareTo(t2)

            return@sortedWith s2.availableStudyMinutesPerDay.compareTo(s1.availableStudyMinutesPerDay)
        }

        val totalDailyMins = sortedSubjects.sumOf { it.availableStudyMinutesPerDay }
        val totalDailyHours = (totalDailyMins / 60).coerceAtLeast(1)

        val earliestExam = sortedSubjects
            .filter { it.examDate.isNotBlank() }
            .minByOrNull { parseDateMillis(it.examDate) }
            ?.let { "${it.name} Exam: ${it.examDate}" }
            ?: "Academic Preparation"

        val fallbackDailyGoals = sortedSubjects.mapIndexed { idx, subj ->
            val priorityLabel = when (idx) {
                0 -> "Top Priority (Rank 1)"
                1 -> "High Priority (Rank 2)"
                2 -> "Core Priority (Rank 3)"
                else -> "Standard Priority (Rank ${idx + 1})"
            }
            val uncompletedTopics = syllabus
                .filter { it.subject.equals(subj.name, ignoreCase = true) && !it.isCompleted }
                .take(2)
                .joinToString { it.topic }
            val topicsInfo = if (uncompletedTopics.isNotBlank()) " | Topics: $uncompletedTopics" else ""
            "${subj.name}: $priorityLabel [${subj.importance} Importance, ${subj.difficulty} Difficulty, ${subj.availableStudyMinutesPerDay} mins/day]$topicsInfo"
        }

        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val fallbackWeeklySchedule = days.mapIndexed { dIdx, day ->
            val targetSubj = sortedSubjects[dIdx % sortedSubjects.size]
            val examTag = if (targetSubj.examDate.isNotBlank()) " | Exam: ${targetSubj.examDate}" else ""
            "$day: Academic Study on ${targetSubj.name} (${targetSubj.availableStudyMinutesPerDay} mins)$examTag"
        }

        val subjectOrderSummary = sortedSubjects.mapIndexed { idx, s ->
            "${idx + 1}. ${s.name} (Importance: ${s.importance}, Difficulty: ${s.difficulty}, Exam Date: ${if (s.examDate.isBlank()) "None" else s.examDate}, Available: ${s.availableStudyMinutesPerDay} mins/day)"
        }.joinToString("\n")

        val systemPrompt = """
            You are an expert Academic Study Planner.
            Mode: Academic
            Create a structured, highly actionable daily study roadmap and weekly schedule for an academic student.
            DO NOT mention GATE, GATE exams, or GATE PYQs.

            MANDATORY PRIORITY ORDER (Strictly pre-calculated by Importance -> Difficulty -> Exam Date):
            $subjectOrderSummary

            Instructions:
            1. Allocate daily study attention strictly following this ranking (Top ranked subjects get highest priority and focus).
            2. Return ONLY a valid JSON object with the following schema:
            {
              "dailyGoals": [
                "Subject Name: Priority details and specific actionable topics to master"
              ],
              "weeklySchedule": [
                "Mon: Subject (N mins) - topics to cover",
                "Tue: Subject (N mins) - topics to cover",
                "Wed: Subject (N mins) - topics to cover",
                "Thu: Subject (N mins) - topics to cover",
                "Fri: Subject (N mins) - topics to cover",
                "Sat: Subject (N mins) - Revision & Practice",
                "Sun: Weekly Review & Concept Mapping"
              ]
            }
        """.trimIndent()

        val messages = listOf(
            AcademicChatMessageDto(role = "system", content = systemPrompt),
            AcademicChatMessageDto(role = "user", content = "Generate a prioritized Academic study schedule in strict JSON format.")
        )

        try {
            val response = api.createChatCompletion(
                authorization = getAuthHeader(),
                request = AcademicChatCompletionRequest(
                    model = BuildConfig.NVIDIA_MODEL.ifBlank { "openai/gpt-oss-20b" },
                    messages = messages,
                    temperature = 0.4,
                    maxTokens = 3000
                )
            )

            val content = response.choices?.firstOrNull()?.message?.content?.trim() ?: ""
            val cleaned = content.replace("```json", "", ignoreCase = true).replace("```", "").trim()
            if (cleaned.startsWith("{")) {
                val json = JSONObject(cleaned)
                val goalsArray = json.optJSONArray("dailyGoals")
                val schedArray = json.optJSONArray("weeklySchedule")

                val parsedGoals = mutableListOf<String>()
                if (goalsArray != null) {
                    for (i in 0 until goalsArray.length()) {
                        val g = goalsArray.optString(i, "").trim()
                        if (g.isNotBlank()) parsedGoals.add(g)
                    }
                }

                val parsedSched = mutableListOf<String>()
                if (schedArray != null) {
                    for (i in 0 until schedArray.length()) {
                        val s = schedArray.optString(i, "").trim()
                        if (s.isNotBlank()) parsedSched.add(s)
                    }
                }

                // Strict validation: Ensure each parsed goal belongs to Academic subjects and does not mention GATE
                val validParsedGoals = parsedGoals.filter { g ->
                    academicSubjects.any { subj -> g.contains(subj.name, ignoreCase = true) } &&
                    !g.contains("GATE", ignoreCase = true) && !g.contains("PYQ", ignoreCase = true)
                }
                val validParsedSched = parsedSched.filter { s ->
                    !s.contains("GATE", ignoreCase = true) && !s.contains("PYQ", ignoreCase = true)
                }

                if (validParsedGoals.isNotEmpty() && validParsedSched.isNotEmpty()) {
                    val enrichedPlan = com.example.model.StudyPlan(
                        mode = PreparationMode.ACADEMIC,
                        dailyHours = totalDailyHours,
                        targetExam = earliestExam,
                        dailyGoals = validParsedGoals,
                        weeklySchedule = validParsedSched,
                        completedGoals = emptyList(),
                        missedGoals = emptyList()
                    )
                    return@withContext Result.success(enrichedPlan)
                }
            }
        } catch (e: Exception) {
            Log.e("AcademicAiService", "Error enriching Academic study plan with AI, using deterministic fallback", e)
        }

        val finalPlan = com.example.model.StudyPlan(
            mode = PreparationMode.ACADEMIC,
            dailyHours = totalDailyHours,
            targetExam = earliestExam,
            dailyGoals = fallbackDailyGoals,
            weeklySchedule = fallbackWeeklySchedule,
            completedGoals = emptyList(),
            missedGoals = emptyList()
        )
        Result.success(finalPlan)
    }

    // =========================================================================
    // 4. AUTOMATIC RESCHEDULING OF ACADEMIC MISSED TASKS
    // =========================================================================
    /**
     * Reschedules missed/uncompleted Academic tasks while strictly preserving the priority algorithm:
     * 1. Important Level (High > Medium > Low)
     * 2. Difficulty Level (Hard > Medium > Easy)
     * 3. Exam Date (Earliest date first)
     * Daily study time limits are strictly respected without overloading the student.
     */
    suspend fun rescheduleAcademicMissedTasks(
        currentPlan: com.example.model.StudyPlan,
        completedGoalsList: List<String>,
        subjects: List<com.example.model.SubjectData>,
        syllabus: List<com.example.model.SyllabusData>,
        userId: String
    ): Result<com.example.model.StudyPlan> = withContext(Dispatchers.IO) {
        val allDailyGoals = currentPlan.dailyGoals
        val completedSet = completedGoalsList.toSet()
        val missedGoals = allDailyGoals.filter { !completedSet.contains(it) }

        if (missedGoals.isEmpty()) {
            val updated = currentPlan.copy(
                completedGoals = completedGoalsList,
                missedGoals = emptyList()
            )
            return@withContext Result.success(updated)
        }

        val academicSubjects = subjects.filter { it.mode == PreparationMode.ACADEMIC }

        fun impRank(imp: String?) = when (imp?.trim()?.lowercase()) {
            "high" -> 3
            "medium" -> 2
            "low" -> 1
            else -> 2
        }

        fun diffRank(diff: String?) = when (diff?.trim()?.lowercase()) {
            "hard" -> 3
            "medium" -> 2
            "easy" -> 1
            else -> 2
        }

        fun parseDateMillis(dateStr: String?): Long {
            if (dateStr.isNullOrBlank()) return Long.MAX_VALUE
            return try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                sdf.parse(dateStr.trim())?.time ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
        }

        // DETERMINISTIC SORTING: Importance -> Difficulty -> Exam Date
        val sortedSubjects = academicSubjects.sortedWith { s1, s2 ->
            val i1 = impRank(s1.importance)
            val i2 = impRank(s2.importance)
            if (i1 != i2) return@sortedWith i2.compareTo(i1)

            val d1 = diffRank(s1.difficulty)
            val d2 = diffRank(s2.difficulty)
            if (d1 != d2) return@sortedWith d2.compareTo(d1)

            val t1 = parseDateMillis(s1.examDate)
            val t2 = parseDateMillis(s2.examDate)
            if (t1 != t2) return@sortedWith t1.compareTo(t2)

            return@sortedWith s2.availableStudyMinutesPerDay.compareTo(s1.availableStudyMinutesPerDay)
        }

        val dailyLimitHours = currentPlan.dailyHours.coerceAtLeast(1)
        val dailyLimitMins = dailyLimitHours * 60

        // Build prioritized updated daily goals including missed/rescheduled items
        val cleanMissedList = missedGoals.map { it.replace(Regex("^\\[(MISSED|RESCHEDULED)\\]\\s*"), "").trim() }
        val updatedDailyGoals = mutableListOf<String>()

        cleanMissedList.forEach { cleanMissed ->
            updatedDailyGoals.add("[RESCHEDULED] $cleanMissed")
        }

        sortedSubjects.forEach { subj ->
            val alreadyPresent = updatedDailyGoals.any { it.contains(subj.name, ignoreCase = true) }
            if (!alreadyPresent) {
                val uncompletedTopics = syllabus
                    .filter { it.subject.equals(subj.name, ignoreCase = true) && !it.isCompleted }
                    .take(2)
                    .joinToString { it.topic }
                val topicsInfo = if (uncompletedTopics.isNotBlank()) " | Topics: $uncompletedTopics" else ""
                updatedDailyGoals.add("${subj.name}: Core Priority [${subj.importance} Importance, ${subj.difficulty} Difficulty, ${subj.availableStudyMinutesPerDay} mins/day]$topicsInfo")
            }
        }

        // Build updated weekly schedule allocating missed tasks to upcoming days without exceeding daily capacity
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val updatedWeeklySchedule = mutableListOf<String>()
        var missedIdx = 0

        days.forEachIndexed { dIdx, day ->
            if (missedIdx < cleanMissedList.size) {
                val mGoal = cleanMissedList[missedIdx].take(45)
                val targetSubj = sortedSubjects.firstOrNull { mGoal.contains(it.name, ignoreCase = true) } ?: (if (sortedSubjects.isNotEmpty()) sortedSubjects[dIdx % sortedSubjects.size] else null)
                val mins = minOf(targetSubj?.availableStudyMinutesPerDay ?: 60, dailyLimitMins)
                updatedWeeklySchedule.add("$day: [Make-up] $mGoal (${mins} mins)")
                missedIdx++
            } else {
                val targetSubj = if (sortedSubjects.isNotEmpty()) sortedSubjects[dIdx % sortedSubjects.size] else null
                val mins = minOf(targetSubj?.availableStudyMinutesPerDay ?: 60, dailyLimitMins)
                val examTag = if (targetSubj?.examDate?.isNotBlank() == true) " | Exam: ${targetSubj.examDate}" else ""
                val subjName = targetSubj?.name ?: "Academic Studies"
                updatedWeeklySchedule.add("$day: Academic Study on $subjName (${mins} mins)$examTag")
            }
        }

        var enrichedGoals = emptyList<String>()
        var enrichedSched = emptyList<String>()

        val missedSummary = cleanMissedList.joinToString("; ")
        val systemPrompt = """
            You are an expert Academic Study Planner.
            Mode: Academic
            Update and reschedule the study roadmap for a student who missed tasks: $missedSummary.
            DO NOT mention GATE, GATE exams, or GATE PYQs.

            Prioritized Subjects:
            ${sortedSubjects.mapIndexed { i, s -> "${i + 1}. ${s.name} (${s.importance} Importance, ${s.difficulty} Difficulty, Exam: ${s.examDate})" }.joinToString("\n")}

            Instructions:
            1. Integrate missed tasks into upcoming study sessions without exceeding the daily study time limit of $dailyLimitHours hours ($dailyLimitMins mins/day).
            2. Maintain strict priority ordering: Importance -> Difficulty -> Exam Date.
            3. Return ONLY a valid JSON object matching:
            {
              "dailyGoals": ["Rescheduled/Prioritized goal statements"],
              "weeklySchedule": ["Day: Detailed study allocation"]
            }
        """.trimIndent()

        val messages = listOf(
            AcademicChatMessageDto(role = "system", content = systemPrompt),
            AcademicChatMessageDto(role = "user", content = "Reschedule missed Academic tasks: $missedSummary in strict JSON format.")
        )

        try {
            val response = api.createChatCompletion(
                authorization = getAuthHeader(),
                request = AcademicChatCompletionRequest(
                    model = BuildConfig.NVIDIA_MODEL.ifBlank { "openai/gpt-oss-20b" },
                    messages = messages,
                    temperature = 0.4,
                    maxTokens = 3000
                )
            )

            val content = response.choices?.firstOrNull()?.message?.content?.trim() ?: ""
            val cleaned = content.replace("```json", "", ignoreCase = true).replace("```", "").trim()
            if (cleaned.startsWith("{")) {
                val json = JSONObject(cleaned)
                val goalsArray = json.optJSONArray("dailyGoals")
                val schedArray = json.optJSONArray("weeklySchedule")

                val parsedGoals = mutableListOf<String>()
                if (goalsArray != null) {
                    for (i in 0 until goalsArray.length()) {
                        val g = goalsArray.optString(i, "").trim()
                        if (g.isNotBlank()) parsedGoals.add(g)
                    }
                }

                val parsedSched = mutableListOf<String>()
                if (schedArray != null) {
                    for (i in 0 until schedArray.length()) {
                        val s = schedArray.optString(i, "").trim()
                        if (s.isNotBlank()) parsedSched.add(s)
                    }
                }

                if (parsedGoals.isNotEmpty() && parsedSched.isNotEmpty()) {
                    enrichedGoals = parsedGoals
                    enrichedSched = parsedSched
                }
            }
        } catch (e: Exception) {
            Log.e("AcademicAiService", "Error calling AI for academic rescheduling, using deterministic plan", e)
        }

        // VALIDATE: Enforce retention of all missed tasks and priority order, filtering out any non-academic or GATE items
        val finalDailyGoals = mutableListOf<String>()
        cleanMissedList.filter { !it.contains("GATE", ignoreCase = true) && !it.contains("PYQ", ignoreCase = true) }.forEach { cleanMissed ->
            finalDailyGoals.add("[RESCHEDULED] $cleanMissed")
        }

        if (enrichedGoals.isNotEmpty()) {
            enrichedGoals.forEach { g ->
                val cleanG = g.replace(Regex("^\\[(MISSED|RESCHEDULED)\\]\\s*"), "").trim()
                val isAcademic = academicSubjects.any { subj -> cleanG.contains(subj.name, ignoreCase = true) } &&
                        !cleanG.contains("GATE", ignoreCase = true) && !cleanG.contains("PYQ", ignoreCase = true)
                if (isAcademic && !finalDailyGoals.any { it.contains(cleanG.take(25), ignoreCase = true) }) {
                    finalDailyGoals.add(g)
                }
            }
        }
        updatedDailyGoals.filter { !it.contains("GATE", ignoreCase = true) && !it.contains("PYQ", ignoreCase = true) }.forEach { g ->
            if (!finalDailyGoals.contains(g)) {
                finalDailyGoals.add(g)
            }
        }

        val finalPlan = currentPlan.copy(
            dailyGoals = finalDailyGoals.take(6),
            weeklySchedule = updatedWeeklySchedule.filter { !it.contains("GATE", ignoreCase = true) && !it.contains("PYQ", ignoreCase = true) },
            completedGoals = completedGoalsList,
            missedGoals = missedGoals
        )
        Result.success(finalPlan)
    }
}

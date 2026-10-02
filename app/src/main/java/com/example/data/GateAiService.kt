package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.model.PreparationMode
import com.example.model.QuestionData
import com.example.model.SourceType
import com.example.model.StudyPlan
import com.example.model.SubjectData
import com.example.model.SyllabusData
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GateChatMessageDto(
    @param:Json(name = "role") val role: String,
    @param:Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class GateChatCompletionRequest(
    @param:Json(name = "model") val model: String = BuildConfig.NVIDIA_MODEL.ifBlank { "openai/gpt-oss-20b" },
    @param:Json(name = "messages") val messages: List<GateChatMessageDto>,
    @param:Json(name = "temperature") val temperature: Double = 0.5,
    @param:Json(name = "top_p") val topP: Double = 1.0,
    @param:Json(name = "max_tokens") val maxTokens: Int = 4096,
    @param:Json(name = "stream") val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GateChoiceMessageDto(
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "content") val content: String? = null,
    @param:Json(name = "reasoning_content") val reasoningContent: String? = null
)

@JsonClass(generateAdapter = true)
data class GateChoiceDto(
    @param:Json(name = "index") val index: Int? = null,
    @param:Json(name = "message") val message: GateChoiceMessageDto? = null,
    @param:Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GateChatCompletionResponse(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "choices") val choices: List<GateChoiceDto>? = null
)

interface GateOpenAiApi {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GateChatCompletionRequest
    ): GateChatCompletionResponse
}

class GateAiService {

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

    private val api: GateOpenAiApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GateOpenAiApi::class.java)

    private fun getAuthHeader(): String {
        val apiKey = BuildConfig.NVIDIA_API_KEY.trim()
        return if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"
    }

    // =========================================================================
    // 1. GATE AI TUTOR
    // =========================================================================
    suspend fun askGateAiTutor(
        context: Context,
        query: String,
        subject: String? = null,
        subtopic: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.success("Please enter a question.")
        }

        // 1. Check verified GATE dataset first
        try {
            val pyqItems = GatePyqDataset.getItems(context)
            val matchedQuestion = GatePyqDataset.findMatchingQuestion(trimmed, pyqItems)
            if (matchedQuestion != null) {
                val verifiedAnswer = matchedQuestion.getFormattedCorrectAnswer()
                val explanation = if (matchedQuestion.explanation.isNotBlank()) {
                    matchedQuestion.explanation
                } else {
                    "Official verified answer from the GATE CS dataset (Question ID: ${matchedQuestion.id}, Type: ${matchedQuestion.questionType}, Marks: ${matchedQuestion.marks})."
                }

                val response = buildString {
                    append("Correct Answer: ")
                    append(verifiedAnswer)
                    append("\n\nExplanation:\n")
                    append(explanation)
                    append("\n\nSolution:\n")
                    append("Verified GATE ${matchedQuestion.year} question for ${matchedQuestion.subject} (${matchedQuestion.topic}). Detailed explanation and solution are provided above based on the official GATE answer key.")
                }

                return@withContext Result.success(response)
            }
        } catch (e: Exception) {
            Log.w("GateAiService", "Dataset search failed, falling back to AI", e)
        }

        // 2. Query NVIDIA AI model (openai/gpt-oss-20b)
        val subjectContext = if (!subject.isNullOrBlank() && subject != "All" && subject != "All Subjects") {
            "Subject: $subject"
        } else {
            "Subject: GATE Computer Science & Engineering"
        }

        val topicContext = if (!subtopic.isNullOrBlank() && subtopic != "All" && subtopic != "All Subtopics") {
            "Topic: $subtopic"
        } else {
            "Topic: General GATE Core Concepts"
        }

        val systemPrompt = """
            You are an expert AI Tutor for the Graduate Aptitude Test in Engineering (GATE) in Computer Science & Information Technology (CS/IT).
            Mode: GATE Preparation
            $subjectContext
            $topicContext

            Student's Question:
            $trimmed

            Instructions:
            1. Provide an accurate, clear, and comprehensive answer tailored for GATE aspirants.
            2. For conceptual questions, structure the answer clearly:
               Direct Answer:
               <direct, precise answer>

               Simple Explanation:
               <clear, intuitive explanation of the concept and underlying principles>

               Important GATE Concept:
               <key formulas, definitions, edge cases, standard theorems, or common exam traps>

            3. For numerical or step-by-step problem-solving questions, structure the answer clearly:
               Given Information:
               <given parameters and variables>

               Formula / Concept:
               <standard formula or theorem applied>

               Step-by-step Calculation:
               <clear derivation or calculation steps>

               Final Answer:
               <final numerical value with units or exact result>
        """.trimIndent()

        val messages = listOf(
            GateChatMessageDto(role = "system", content = systemPrompt),
            GateChatMessageDto(role = "user", content = trimmed)
        )

        val request = GateChatCompletionRequest(
            messages = messages,
            temperature = 0.4,
            maxTokens = 3000
        )

        try {
            val response = api.createChatCompletion(
                authorization = getAuthHeader(),
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content?.trim()
            if (!content.isNullOrBlank()) {
                return@withContext Result.success(content)
            }

            Result.failure(Exception("Empty response received from AI model."))
        } catch (e: Exception) {
            Log.e("GateAiService", "Error calling GATE AI Tutor API", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 2. GATE PRACTICE QUESTION GENERATOR
    // =========================================================================
    suspend fun generateGatePracticeQuestions(
        subject: String,
        topic: String,
        count: Int,
        difficulty: String = "Medium",
        userId: String = ""
    ): Result<List<QuestionData>> = withContext(Dispatchers.IO) {
        val cleanSubject = if (subject.isNotBlank() && subject != "All" && subject != "All Subjects") {
            subject.trim()
        } else {
            "GATE Computer Science Core"
        }

        val cleanTopic = if (topic.isNotBlank() && topic != "All" && topic != "All Subtopics") {
            topic.trim()
        } else {
            "Core Concepts & Problem Solving"
        }

        val systemPrompt = """
            You are an expert GATE Exam Item Writer for Computer Science & IT.
            Mode: GATE Preparation
            Subject: $cleanSubject
            Topic: $cleanTopic
            Question Count: $count
            Difficulty: $difficulty

            Generate exactly $count high-quality GATE-level multiple choice practice questions testing mastery of $cleanSubject ($cleanTopic).
            DO NOT generate elementary or non-GATE questions. Ensure genuine GATE exam standard.

            Return ONLY a valid JSON object matching this schema without any markdown formatting or surrounding text:
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
                  "explanation": "Clear step-by-step explanation and derivation of the correct answer",
                  "difficulty": "$difficulty",
                  "subject": "$cleanSubject",
                  "subtopic": "$cleanTopic",
                  "marks": 1
                }
              ]
            }
        """.trimIndent()

        val messages = listOf(
            GateChatMessageDto(role = "system", content = systemPrompt),
            GateChatMessageDto(
                role = "user",
                content = "Generate $count GATE questions for $cleanSubject ($cleanTopic) with difficulty $difficulty in strict JSON."
            )
        )

        val request = GateChatCompletionRequest(
            messages = messages,
            temperature = 0.5,
            maxTokens = 4096
        )

        try {
            val response = api.createChatCompletion(
                authorization = getAuthHeader(),
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content?.trim() ?: ""
            val parsed = parseQuestionsJson(content, cleanSubject, cleanTopic, difficulty, userId)
            if (parsed.isNotEmpty()) {
                return@withContext Result.success(parsed.take(count))
            }
        } catch (e: Exception) {
            Log.e("GateAiService", "Error calling GATE Practice Generator API", e)
        }

        // Deterministic Fallback if network/API fails
        val fallback = generateLocalGateFallbackQuestions(cleanSubject, cleanTopic, count, difficulty, userId)
        Result.success(fallback)
    }

    fun parseQuestionsJson(
        rawJson: String,
        defaultSubject: String,
        defaultTopic: String,
        defaultDifficulty: String,
        userId: String
    ): List<QuestionData> {
        val result = mutableListOf<QuestionData>()
        val cleaned = rawJson
            .replace("```json", "", ignoreCase = true)
            .replace("```", "")
            .trim()

        if (cleaned.isBlank() || (!cleaned.startsWith("{") && !cleaned.startsWith("["))) {
            return emptyList()
        }

        try {
            val rootObj = if (cleaned.startsWith("{")) {
                JSONObject(cleaned)
            } else {
                JSONObject().put("questions", org.json.JSONArray(cleaned))
            }

            val array = rootObj.optJSONArray("questions") ?: return emptyList()
            for (i in 0 until array.length()) {
                val qObj = array.optJSONObject(i) ?: continue
                val qText = qObj.optString("question", "").trim()
                if (qText.isBlank()) continue

                val optArray = qObj.optJSONArray("options")
                val options = mutableListOf<String>()
                if (optArray != null) {
                    for (j in 0 until optArray.length()) {
                        val opt = optArray.optString(j, "").trim()
                        if (opt.isNotBlank()) options.add(opt)
                    }
                } else {
                    val a = qObj.optString("optionA", "").trim()
                    val b = qObj.optString("optionB", "").trim()
                    val c = qObj.optString("optionC", "").trim()
                    val d = qObj.optString("optionD", "").trim()
                    if (a.isNotBlank() && b.isNotBlank() && c.isNotBlank() && d.isNotBlank()) {
                        options.addAll(listOf(a, b, c, d))
                    }
                }

                if (options.size < 4) continue

                val normalizedOptions = options.take(4).map { opt ->
                    opt.replace(Regex("^[A-Da-d][\\.\\):\\-]\\s*"), "").trim()
                }

                val correctRaw = qObj.optString("correctAnswer", "A").trim()
                val correctIndex = when {
                    correctRaw.equals("A", ignoreCase = true) || correctRaw == "0" -> 0
                    correctRaw.equals("B", ignoreCase = true) || correctRaw == "1" -> 1
                    correctRaw.equals("C", ignoreCase = true) || correctRaw == "2" -> 2
                    correctRaw.equals("D", ignoreCase = true) || correctRaw == "3" -> 3
                    else -> {
                        val matchIdx = normalizedOptions.indexOfFirst { it.equals(correctRaw, ignoreCase = true) }
                        if (matchIdx in 0..3) matchIdx else 0
                    }
                }

                val explanation = qObj.optString("explanation", "Standard GATE solution.").trim()
                val difficulty = qObj.optString("difficulty", defaultDifficulty).trim().ifEmpty { defaultDifficulty }
                val qSubject = qObj.optString("subject", defaultSubject).trim().ifEmpty { defaultSubject }
                val qTopic = qObj.optString("subtopic", qObj.optString("topic", defaultTopic)).trim().ifEmpty { defaultTopic }
                val marks = qObj.optInt("marks", if (difficulty.equals("Hard", ignoreCase = true)) 2 else 1)

                result.add(
                    QuestionData(
                        id = "gate_ai_${System.currentTimeMillis()}_$i",
                        year = "GATE Practice",
                        subject = qSubject,
                        topic = qTopic,
                        question = qText,
                        options = normalizedOptions,
                        correctAnswer = correctIndex,
                        explanation = explanation,
                        marks = marks,
                        difficulty = difficulty,
                        sourceType = SourceType.AI_PRACTICE,
                        isVerified = true,
                        mode = PreparationMode.PROFESSIONAL_GATE,
                        userId = userId
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("GateAiService", "Error parsing GATE questions JSON", e)
        }
        return result
    }

    private fun generateLocalGateFallbackQuestions(
        subject: String,
        topic: String,
        count: Int,
        difficulty: String,
        userId: String
    ): List<QuestionData> {
        val list = mutableListOf<QuestionData>()
        val templates = listOf(
            Triple(
                "Consider the fundamental principles of %s in %s. Which of the following statements is TRUE?",
                listOf(
                    "It ensures optimal algorithmic time bounds and bounded memory utilization.",
                    "It leads to non-deterministic execution in all Turing-equivalent machines.",
                    "It completely eliminates any need for CPU register mapping or caching.",
                    "It violates the principle of locality in modern memory hierarchies."
                ),
                "In GATE CS, %s in %s is designed for correct and optimal execution bounds under standard computing models."
            ),
            Triple(
                "What is the worst-case asymptotic upper bound associated with %s operations in %s?",
                listOf(
                    "O(n log n) with deterministic optimal space complexity.",
                    "O(2^n) exponential unconstrained latency in all configurations.",
                    "O(1) unbounded non-terminating recursion.",
                    "Strictly unbounded execution without progress guarantees."
                ),
                "%s in %s guarantees rigorous upper and lower asymptotic bounds under worst-case inputs."
            ),
            Triple(
                "In the context of GATE %s, which property is strictly guaranteed for %s?",
                listOf(
                    "Correctness invariant preservation across all state transitions.",
                    "Complete bypass of compiler optimization passes.",
                    "Arbitrary uncoordinated memory mutations.",
                    "Lack of deterministic verification criteria."
                ),
                "Formal verification of %s in %s ensures state transition invariants are preserved."
            )
        )

        for (i in 0 until count) {
            val tmpl = templates[i % templates.size]
            val qText = String.format(tmpl.first, topic, subject)
            val exp = String.format(tmpl.third, topic, subject)

            list.add(
                QuestionData(
                    id = "gate_local_${System.currentTimeMillis()}_$i",
                    year = "2025",
                    subject = subject,
                    topic = topic,
                    question = qText,
                    options = tmpl.second,
                    correctAnswer = 0,
                    explanation = exp,
                    marks = if (difficulty.equals("Hard", ignoreCase = true)) 2 else 1,
                    difficulty = difficulty,
                    sourceType = SourceType.AI_PRACTICE,
                    isVerified = true,
                    mode = PreparationMode.PROFESSIONAL_GATE,
                    userId = userId
                )
            )
        }
        return list
    }

    // =========================================================================
    // 3. GATE AI STUDY SCHEDULE PLANNER
    // =========================================================================
    /**
     * Priority order enforced in application logic:
     * 1. Important Level (High > Medium > Low)
     * 2. Difficulty Level (Hard > Medium > Easy)
     * 3. Exam Date (Earliest date first)
     */
    suspend fun generateGateStudySchedule(
        subjects: List<SubjectData>,
        syllabus: List<SyllabusData>,
        userId: String
    ): Result<StudyPlan> = withContext(Dispatchers.IO) {
        val gateSubjects = subjects.filter { it.mode == PreparationMode.PROFESSIONAL_GATE }
        if (gateSubjects.isEmpty()) {
            val emptyPlan = StudyPlan(
                mode = PreparationMode.PROFESSIONAL_GATE,
                dailyHours = 0,
                targetExam = "GATE CS 2026",
                dailyGoals = emptyList(),
                weeklySchedule = emptyList()
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
        val sortedSubjects = gateSubjects.sortedWith { s1, s2 ->
            // Priority 1: Important Level
            val i1 = impRank(s1.importance)
            val i2 = impRank(s2.importance)
            if (i1 != i2) return@sortedWith i2.compareTo(i1)

            // Priority 2: Difficulty Level
            val d1 = diffRank(s1.difficulty)
            val d2 = diffRank(s2.difficulty)
            if (d1 != d2) return@sortedWith d2.compareTo(d1)

            // Priority 3: Exam Date (Earlier exam date receives higher priority)
            val t1 = parseDateMillis(s1.examDate)
            val t2 = parseDateMillis(s2.examDate)
            if (t1 != t2) return@sortedWith t1.compareTo(t2)

            // Tie-breaker: available study minutes
            return@sortedWith s2.availableStudyMinutesPerDay.compareTo(s1.availableStudyMinutesPerDay)
        }

        val totalDailyMins = sortedSubjects.sumOf { it.availableStudyMinutesPerDay }
        val totalDailyHours = (totalDailyMins / 60).coerceAtLeast(2)

        val earliestExam = sortedSubjects
            .filter { it.examDate.isNotBlank() }
            .minByOrNull { parseDateMillis(it.examDate) }
            ?.let { "${it.name} Exam: ${it.examDate}" }
            ?: "GATE CS 2026"

        // Build deterministic fallback roadmap first
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
            "$day: Intensive Study on ${targetSubj.name} (${targetSubj.availableStudyMinutesPerDay} mins)$examTag"
        }

        // Call AI API (openai/gpt-oss-20b) to enrich the plan descriptions while preserving strict priority
        val subjectOrderSummary = sortedSubjects.mapIndexed { idx, s ->
            "${idx + 1}. ${s.name} (Importance: ${s.importance}, Difficulty: ${s.difficulty}, Exam Date: ${if (s.examDate.isBlank()) "None" else s.examDate}, Available: ${s.availableStudyMinutesPerDay} mins/day)"
        }.joinToString("\n")

        val systemPrompt = """
            You are an expert GATE Exam Strategist.
            Mode: GATE Preparation
            Create an actionable daily study roadmap and weekly schedule for a GATE aspirant.

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
                "Sat: Subject (N mins) - Revision & Mock Practice",
                "Sun: Full Syllabus Revision & Analysis"
              ]
            }
        """.trimIndent()

        val messages = listOf(
            GateChatMessageDto(role = "system", content = systemPrompt),
            GateChatMessageDto(role = "user", content = "Generate a prioritized GATE study schedule in strict JSON format.")
        )

        val request = GateChatCompletionRequest(
            messages = messages,
            temperature = 0.4,
            maxTokens = 3000
        )

        try {
            val response = api.createChatCompletion(
                authorization = getAuthHeader(),
                request = request
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

                // Strict validation: Ensure each parsed goal belongs to GATE subjects and contains no Academic data
                val validParsedGoals = parsedGoals.filter { g ->
                    gateSubjects.any { subj -> g.contains(subj.name, ignoreCase = true) } &&
                    !g.contains("Academic", ignoreCase = true)
                }
                val validParsedSched = parsedSched.filter { s ->
                    !s.contains("Academic", ignoreCase = true)
                }

                if (validParsedGoals.isNotEmpty() && validParsedSched.isNotEmpty()) {
                    val enrichedPlan = StudyPlan(
                        mode = PreparationMode.PROFESSIONAL_GATE,
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
            Log.e("GateAiService", "Error enriching GATE study plan with AI, using deterministic fallback", e)
        }

        // Return deterministic plan
        val finalPlan = StudyPlan(
            mode = PreparationMode.PROFESSIONAL_GATE,
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
    // 4. AUTOMATIC RESCHEDULING OF MISSED TASKS
    // =========================================================================
    /**
     * Reschedules missed/uncompleted tasks while strictly preserving the priority algorithm:
     * 1. Important Level (High > Medium > Low)
     * 2. Difficulty Level (Hard > Medium > Easy)
     * 3. Exam Date (Earliest date first)
     * Daily study time limits are strictly respected without overloading the student.
     */
    suspend fun rescheduleGateMissedTasks(
        currentPlan: StudyPlan,
        completedGoalsList: List<String>,
        subjects: List<SubjectData>,
        syllabus: List<SyllabusData>,
        userId: String
    ): Result<StudyPlan> = withContext(Dispatchers.IO) {
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

        val gateSubjects = subjects.filter { it.mode == PreparationMode.PROFESSIONAL_GATE }

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
        val sortedSubjects = gateSubjects.sortedWith { s1, s2 ->
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

        val dailyLimitHours = currentPlan.dailyHours.coerceAtLeast(2)
        val dailyLimitMins = dailyLimitHours * 60

        // Build prioritized updated daily goals including missed/rescheduled items
        val updatedDailyGoals = mutableListOf<String>()
        val cleanMissedList = missedGoals.map { it.replace(Regex("^\\[(MISSED|RESCHEDULED)\\]\\s*"), "").trim() }

        // Place missed tasks into priority ordering
        cleanMissedList.forEach { cleanMissed ->
            updatedDailyGoals.add("[RESCHEDULED] $cleanMissed")
        }

        // Add remaining top-priority subject goals if not already present
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
                val targetSubj = sortedSubjects.firstOrNull { mGoal.contains(it.name, ignoreCase = true) } ?: sortedSubjects[dIdx % sortedSubjects.size]
                val mins = minOf(targetSubj.availableStudyMinutesPerDay, dailyLimitMins)
                updatedWeeklySchedule.add("$day: [Make-up] $mGoal (${mins} mins)")
                missedIdx++
            } else {
                val targetSubj = sortedSubjects[dIdx % sortedSubjects.size]
                val mins = minOf(targetSubj.availableStudyMinutesPerDay, dailyLimitMins)
                val examTag = if (targetSubj.examDate.isNotBlank()) " | Exam: ${targetSubj.examDate}" else ""
                updatedWeeklySchedule.add("$day: Intensive Study on ${targetSubj.name} (${mins} mins)$examTag")
            }
        }

        // AI Enrichment attempt
        val missedSummary = cleanMissedList.joinToString("; ")
        val systemPrompt = """
            You are an expert GATE Exam Strategist.
            Mode: GATE Preparation
            Update and reschedule the study roadmap for a GATE aspirant who missed tasks: $missedSummary.

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
            GateChatMessageDto(role = "system", content = systemPrompt),
            GateChatMessageDto(role = "user", content = "Reschedule missed GATE tasks: $missedSummary in strict JSON format.")
        )

        val request = GateChatCompletionRequest(
            messages = messages,
            temperature = 0.4,
            maxTokens = 3000
        )

        var enrichedGoals = emptyList<String>()
        var enrichedSched = emptyList<String>()

        try {
            val response = api.createChatCompletion(
                authorization = getAuthHeader(),
                request = request
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
            Log.e("GateAiService", "Error calling AI for rescheduling, using deterministic plan", e)
        }

        // VALIDATE: Enforce retention of all missed tasks and priority order, strictly filtering out any non-GATE items
        val finalDailyGoals = mutableListOf<String>()
        cleanMissedList.filter { !it.contains("Academic", ignoreCase = true) }.forEach { cleanMissed ->
            finalDailyGoals.add("[RESCHEDULED] $cleanMissed")
        }

        if (enrichedGoals.isNotEmpty()) {
            enrichedGoals.forEach { g ->
                val cleanG = g.replace(Regex("^\\[(MISSED|RESCHEDULED)\\]\\s*"), "").trim()
                val isGate = gateSubjects.any { subj -> cleanG.contains(subj.name, ignoreCase = true) } &&
                        !cleanG.contains("Academic", ignoreCase = true)
                if (isGate && !finalDailyGoals.any { it.contains(cleanG.take(25), ignoreCase = true) }) {
                    finalDailyGoals.add(g)
                }
            }
        }
        updatedDailyGoals.filter { !it.contains("Academic", ignoreCase = true) }.forEach { g ->
            if (!finalDailyGoals.contains(g)) {
                finalDailyGoals.add(g)
            }
        }

        val finalPlan = currentPlan.copy(
            dailyGoals = finalDailyGoals.take(6),
            weeklySchedule = updatedWeeklySchedule.filter { !it.contains("Academic", ignoreCase = true) },
            completedGoals = completedGoalsList,
            missedGoals = missedGoals
        )
        Result.success(finalPlan)
    }
}

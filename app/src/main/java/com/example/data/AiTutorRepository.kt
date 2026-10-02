package com.example.data

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class GatePyqItem(
    val id: String,
    val year: String,
    val exam: String,
    val branch: String,
    val subject: String,
    val topic: String,
    val questionType: String, // MCQ, MSQ, NAT
    val marks: Int,
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String,
    val explanation: String
) {
    fun getFormattedCorrectAnswer(): String {
        val cleanAns = correctAnswer.trim()
        val qType = questionType.trim().uppercase()

        return when (qType) {
            "MCQ" -> {
                val letter = cleanAns.uppercase().take(1)
                val optText = when (letter) {
                    "A" -> optionA.trim()
                    "B" -> optionB.trim()
                    "C" -> optionC.trim()
                    "D" -> optionD.trim()
                    else -> ""
                }
                if (optText.isNotBlank() && !optText.startsWith("CHECK_", ignoreCase = true)) {
                    "$cleanAns ($optText)"
                } else {
                    cleanAns
                }
            }
            "MSQ" -> {
                cleanAns
            }
            "NAT" -> {
                if (cleanAns.contains("–") || cleanAns.contains("-")) {
                    val parts = cleanAns.split(Regex("[–-]")).map { it.trim() }
                    if (parts.size == 2 && parts[0] == parts[1]) {
                        parts[0]
                    } else if (parts.size == 2) {
                        "${parts[0]} to ${parts[1]}"
                    } else {
                        cleanAns
                    }
                } else {
                    cleanAns
                }
            }
            else -> cleanAns
        }
    }
}

object GatePyqDataset {
    private var cachedItems: List<GatePyqItem>? = null

    suspend fun getItems(context: Context): List<GatePyqItem> = withContext(Dispatchers.IO) {
        cachedItems?.let { return@withContext it }

        val list = mutableListOf<GatePyqItem>()
        val seenIds = mutableSetOf<String>()
        val seenTexts = mutableSetOf<String>()

        val files = listOf(
            "GATE_2023_CS_AI_Tutor.json.json",
            "GATE_2024_CS.json",
            "GATE_2023_CS.json",
            "GATE_2022_CS.json"
        )

        for (filename in files) {
            try {
                val jsonStream = context.assets.open(filename)
                val jsonText = jsonStream.bufferedReader().use { it.readText() }
                val jsonArray = org.json.JSONArray(jsonText)

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val id = item.optString("id", "${filename}_Q${i + 1}")
                    if (seenIds.contains(id)) continue

                    val qNum = item.optInt("question_number", i + 1)
                    val section = item.optString("section", "CS-1")
                    val qType = if (item.has("question_type")) item.optString("question_type") else item.optString("type", "MCQ")
                    val marks = item.optInt("marks", 1)
                    val question = if (item.has("question_text")) item.optString("question_text") else item.optString("question", "")
                    val correctAns = if (item.has("correct_answer")) item.optString("correct_answer") else item.optString("answer", "").trim()
                    val explanation = item.optString("explanation", "").trim()

                    val optObj = item.optJSONObject("options")
                    val optA = optObj?.optString("A", "") ?: ""
                    val optB = optObj?.optString("B", "") ?: ""
                    val optC = optObj?.optString("C", "") ?: ""
                    val optD = optObj?.optString("D", "") ?: ""

                    val subject = if (item.has("subject")) item.optString("subject") else mapSubjectAndTopic(qNum, section, question).first
                    val topic = if (item.has("topic")) item.optString("topic") else mapSubjectAndTopic(qNum, section, question).second
                    val year = if (item.has("year")) item.optString("year") else {
                        if (id.contains("2025")) "2025" else if (id.contains("2024")) "2024" else if (id.contains("2023")) "2023" else "2022"
                    }

                    val normText = normalizeText(question)
                    if (question.isNotBlank() && (normText.length < 10 || !seenTexts.contains(normText))) {
                        seenIds.add(id)
                        if (normText.length >= 10) seenTexts.add(normText)

                        list.add(
                            GatePyqItem(
                                id = id,
                                year = year,
                                exam = "GATE",
                                branch = "CS",
                                subject = subject,
                                topic = topic,
                                questionType = qType,
                                marks = marks,
                                question = question,
                                optionA = optA,
                                optionB = optB,
                                optionC = optC,
                                optionD = optD,
                                correctAnswer = correctAns,
                                explanation = explanation
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Silently skip missing or invalid optional asset
            }
        }

        cachedItems = list
        list
    }

    private fun mapSubjectAndTopic(questionNumber: Int, section: String, text: String): Pair<String, String> {
        if (section.equals("GA", ignoreCase = true) || questionNumber in 1..10) {
            return Pair("General Aptitude", "General Aptitude")
        }
        return when (questionNumber) {
            11 -> Pair("Computer Organization and Architecture", "Instruction Pipelining")
            12 -> Pair("Programming and Data Structures", "Software & Program Execution")
            13 -> Pair("Compiler Design", "Parsing & Code Generation")
            14 -> Pair("Operating Systems", "Demand Paging & Virtual Memory")
            15 -> Pair("Databases", "Transactions & Concurrency Control")
            16 -> Pair("Computer Networks", "OSI & TCP/IP Protocol Layers")
            17 -> Pair("Engineering Mathematics", "Functions & Discrete Mathematics")
            18 -> Pair("Algorithms", "Graph Algorithms & Shortest Paths")
            19 -> Pair("Theory of Computation", "Context-Free Grammars & Languages")
            20 -> Pair("Algorithms", "Recurrence Relations & Complexity")
            21 -> Pair("Databases", "B+ Trees & Indexing")
            22 -> Pair("Computer Networks", "TCP Connection & 3-Way Handshake")
            23 -> Pair("Engineering Mathematics", "Linear Algebra & Systems of Equations")
            24 -> Pair("Digital Logic", "Boolean Functions & Minimization")
            25 -> Pair("Digital Logic", "Number Representation & 2's Complement")
            26 -> Pair("Algorithms", "Complexity Classes & Analysis")
            27 -> Pair("Computer Organization and Architecture", "Processor Data Path & Control")
            28 -> Pair("Theory of Computation", "Nondeterministic Finite Automata (NFA)")
            29 -> Pair("Programming and Data Structures", "C Programming & Concurrency")
            30 -> Pair("Engineering Mathematics", "Combinatorics & String Counting")
            31 -> Pair("Engineering Mathematics", "Calculus & Function Analysis")
            32 -> Pair("Engineering Mathematics", "Probability & Conditional Expectation")
            33 -> Pair("Algorithms", "Array Searching & Sorting")
            34 -> Pair("Programming and Data Structures", "C Pointers & Memory Management")
            35 -> Pair("Programming and Data Structures", "Trees & Binary Tree Properties")
            36 -> Pair("Computer Organization and Architecture", "Memory Hierarchy & Cache Mapping")
            37 -> Pair("Computer Organization and Architecture", "Instruction Formats & Addressing Modes")
            38 -> Pair("Operating Systems", "CPU Scheduling & Multi-core Systems")
            39 -> Pair("Databases", "Relational Algebra & Tuple Relational Calculus")
            40 -> Pair("Computer Networks", "IPv4 Addressing & IP Routing")
            41 -> Pair("Engineering Mathematics", "Linear Algebra & Eigenvalues")
            42 -> Pair("Digital Logic", "K-Maps & Combinational Minimization")
            43 -> Pair("Algorithms", "Graph Theory & Breadth-First Search")
            44 -> Pair("Theory of Computation", "Formal Languages & Pumping Lemma")
            45 -> Pair("Theory of Computation", "Context-Free & Context-Sensitive Languages")
            46 -> Pair("Compiler Design", "Lexical Analysis & Syntax Analysis")
            47 -> Pair("Databases", "Relational Schemas & Functional Dependencies")
            48 -> Pair("Engineering Mathematics", "Predicate Logic & First-Order Logic")
            49 -> Pair("Engineering Mathematics", "Set Theory & Relations")
            50 -> Pair("Theory of Computation", "Pushdown Automata & Turing Machines")
            51 -> Pair("Operating Systems", "File System Organization & Disk Allocation")
            52 -> Pair("Compiler Design", "Intermediate Code & 3-Address Code")
            53 -> Pair("Computer Organization and Architecture", "Memory Access Time & Cache Performance")
            54 -> Pair("Operating Systems", "Page Replacement Algorithms")
            55 -> Pair("Databases", "SQL Queries & Aggregations")
            56 -> Pair("Computer Networks", "Error Detection & Hamming Distance")
            57 -> Pair("Computer Networks", "IPv4 Fragmentation & MTU Calculation")
            58 -> Pair("Engineering Mathematics", "Probability & Continuous Random Variables")
            59 -> Pair("Theory of Computation", "DFA State Minimization")
            60 -> Pair("Digital Logic", "Sequential Circuits & State Machines")
            61 -> Pair("Programming and Data Structures", "Recursion & Array Processing")
            62 -> Pair("Programming and Data Structures", "Linked List Operations")
            63 -> Pair("Programming and Data Structures", "Bitwise Operations in C")
            64 -> Pair("Algorithms", "Minimum Spanning Trees & Greedy Algorithms")
            65 -> Pair("Algorithms", "Hashing & Double Hashing")
            else -> {
                val lower = text.lowercase()
                when {
                    lower.contains("network") || lower.contains("tcp") || lower.contains("ip") || lower.contains("router") || lower.contains("osi") -> Pair("Computer Networks", "Networking")
                    lower.contains("operating system") || lower.contains("process") || lower.contains("paging") || lower.contains("deadlock") || lower.contains("scheduling") -> Pair("Operating Systems", "Operating Systems")
                    lower.contains("database") || lower.contains("sql") || lower.contains("relation") || lower.contains("b+ tree") || lower.contains("transaction") -> Pair("Databases", "Database Management")
                    lower.contains("algorithm") || lower.contains("graph") || lower.contains("sorting") || lower.contains("spanning tree") || lower.contains("dynamic programming") -> Pair("Algorithms", "Algorithms")
                    lower.contains("data structure") || lower.contains("tree") || lower.contains("stack") || lower.contains("queue") || lower.contains("linked list") || lower.contains("pointer") || lower.contains("array") -> Pair("Programming and Data Structures", "Data Structures")
                    lower.contains("logic") || lower.contains("boolean") || lower.contains("k-map") || lower.contains("circuit") || lower.contains("nand") -> Pair("Digital Logic", "Digital Logic")
                    lower.contains("automata") || lower.contains("grammar") || lower.contains("turing") || lower.contains("nfa") || lower.contains("dfa") || lower.contains("language") -> Pair("Theory of Computation", "Theory of Computation")
                    lower.contains("compiler") || lower.contains("parser") || lower.contains("lexical") || lower.contains("3-address") || lower.contains("syntax") -> Pair("Compiler Design", "Compiler Design")
                    lower.contains("pipeline") || lower.contains("cache") || lower.contains("instruction") || lower.contains("memory hierarchy") || lower.contains("processor") || lower.contains("alu") -> Pair("Computer Organization and Architecture", "Computer Architecture")
                    lower.contains("matrix") || lower.contains("eigen") || lower.contains("calculus") || lower.contains("probability") || lower.contains("discrete") || lower.contains("integral") -> Pair("Engineering Mathematics", "Mathematics")
                    else -> Pair("Computer Science Core", "General Core")
                }
            }
        }
    }

    fun findMatchingQuestion(query: String, items: List<GatePyqItem>): GatePyqItem? {
        val normalizedQuery = normalizeText(query)
        if (normalizedQuery.length < 10) return null

        // 1. Direct exact or substring matches
        for (item in items) {
            val normQuestion = normalizeText(item.question)
            if (normQuestion.isBlank()) continue

            if (normQuestion == normalizedQuery) return item
            if (normalizedQuery.length >= 20 && normQuestion.contains(normalizedQuery)) return item
            if (normQuestion.length >= 20 && normalizedQuery.contains(normQuestion)) return item
        }

        // 2. High-confidence token similarity match
        val queryTokens = tokenize(normalizedQuery)
        if (queryTokens.size < 3) return null

        var bestMatch: GatePyqItem? = null
        var highestScore = 0.0

        for (item in items) {
            val normQuestion = normalizeText(item.question)
            val itemTokens = tokenize(normQuestion)
            if (itemTokens.size < 3) continue

            val intersection = queryTokens.intersect(itemTokens).size
            val union = queryTokens.union(itemTokens).size
            val jaccard = if (union > 0) intersection.toDouble() / union else 0.0

            val minTokens = minOf(queryTokens.size, itemTokens.size)
            val overlapRatio = if (minTokens > 0) intersection.toDouble() / minTokens else 0.0

            if (jaccard > 0.55 || (overlapRatio > 0.75 && intersection >= 5)) {
                val combinedScore = (jaccard + overlapRatio) / 2.0
                if (combinedScore > highestScore) {
                    highestScore = combinedScore
                    bestMatch = item
                }
            }
        }

        return if (highestScore >= 0.60) bestMatch else null
    }

    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace("ﬁ", "fi")
            .replace("ﬂ", "fl")
            .replace("−", "-")
            .replace("–", "-")
            .replace("—", "-")
            .replace("“", "\"")
            .replace("”", "\"")
            .replace("’", "'")
            .replace("‘", "'")
            .replace(Regex("^(?:q|question)?\\s*\\d+[\\s.:-]+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\\\(?:left|right|frac|times|le|ge|int|sum|sqrt|cdot|to|in|mid|cup|cap|oplus|neg)"), " ")
            .replace(Regex("(?i)cs\\s+page\\s+\\d+\\s+of\\s+\\d+\\s+gate\\s+2023.*"), "")
            .replace(Regex("(?i)cs\\s+gate\\s+2023.*"), "")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun tokenize(normalizedText: String): Set<String> {
        val stopWords = setOf(
            "the", "a", "an", "is", "are", "was", "were", "and", "or", "in", "on", "at", "to", "for", "of", "with", "by", "from", "that", "this", "which", "one", "more", "following", "given", "let", "consider"
        )
        return normalizedText.split(" ")
            .map { it.trim() }
            .filter { it.length > 2 && !stopWords.contains(it) }
            .toSet()
    }
}

class AiTutorRepository {

    val gateAiService = GateAiService()

    suspend fun askGateAiTutor(context: Context, prompt: String, subject: String? = null, subtopic: String? = null): Result<String> = withContext(Dispatchers.IO) {
        gateAiService.askGateAiTutor(context, prompt, subject, subtopic)
    }

    suspend fun askAcademicAiTutor(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.success("Please enter a question.")
        }

        // 1. Dedicated Academic Knowledge Dataset matching
        val academicAnswer = AcademicAiTutorData.findAnswer(trimmed)
        Result.success(academicAnswer)
    }

    suspend fun askAiTutor(prompt: String, subjectContext: String = "Academic Studies"): Result<String> = withContext(Dispatchers.IO) {
        askAcademicAiTutor(prompt)
    }

    suspend fun generatePracticeQuestions(mode: com.example.model.PreparationMode, subject: String, topic: String, count: Int = 5): Result<String> = withContext(Dispatchers.IO) {
        if (mode == com.example.model.PreparationMode.PROFESSIONAL_GATE) {
            val questionsResult = gateAiService.generateGatePracticeQuestions(subject, topic, count, "Medium")
            if (questionsResult.isSuccess) {
                val questions = questionsResult.getOrNull() ?: emptyList()
                val jsonArray = org.json.JSONArray()
                for (q in questions) {
                    val obj = org.json.JSONObject()
                    obj.put("question", q.question)
                    val opts = q.options.take(4)
                    obj.put("optionA", if (opts.isNotEmpty()) opts[0] else "")
                    obj.put("optionB", if (opts.size > 1) opts[1] else "")
                    obj.put("optionC", if (opts.size > 2) opts[2] else "")
                    obj.put("optionD", if (opts.size > 3) opts[3] else "")
                    val correctLetter = when (q.correctAnswer) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        3 -> "D"
                        else -> "A"
                    }
                    obj.put("correctAnswer", correctLetter)
                    obj.put("explanation", q.explanation)
                    obj.put("difficulty", q.difficulty)
                    obj.put("subject", q.subject)
                    obj.put("subtopic", q.topic)
                    jsonArray.put(obj)
                }
                val root = org.json.JSONObject()
                root.put("questions", jsonArray)
                return@withContext Result.success(root.toString())
            }
        }
        Result.failure(Exception("Failed to generate AI questions."))
    }
}

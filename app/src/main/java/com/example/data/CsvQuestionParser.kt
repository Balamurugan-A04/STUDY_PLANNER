package com.example.data

import android.content.Context
import com.example.model.PreparationMode
import com.example.model.QuestionData
import com.example.model.SourceType
import org.json.JSONArray
import org.json.JSONObject

object CsvQuestionParser {
    private var cachedAllGateQuestions: List<QuestionData>? = null

    fun parseAllGateQuestions(context: Context, userId: String = ""): List<QuestionData> {
        cachedAllGateQuestions?.let { return it }

        val questions = mutableListOf<QuestionData>()
        
        try {
            val jsonStream = try {
                context.assets.open("GATE_2023_CS_AI_Tutor.json")
            } catch (e: Exception) {
                context.assets.open("GATE_2023_CS_AI_Tutor.json.json")
            }
            val jsonText = jsonStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonText)

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val id = item.optString("id", "GATE_JSON_${i + 1}")
                val qNum = item.optInt("question_number", i + 1)
                val section = item.optString("section", "CS-1")
                val qType = item.optString("question_type", "MCQ")
                val marks = item.optInt("marks", 1)
                val qText = item.optString("question_text", "")
                val correctAns = item.optString("correct_answer", "").trim()
                val rawExplanation = item.optString("explanation", "").trim()

                val (subject, topic) = mapGateJsonQuestionToSubject(qNum, section, qText)

                val options = mutableListOf<String>()
                val optObj = item.optJSONObject("options")
                if (optObj != null && optObj.length() > 0) {
                    val a = optObj.optString("A", "")
                    val b = optObj.optString("B", "")
                    val c = optObj.optString("C", "")
                    val d = optObj.optString("D", "")
                    if (a.isNotBlank() || b.isNotBlank() || c.isNotBlank() || d.isNotBlank()) {
                        options.add(a)
                        options.add(b)
                        options.add(c)
                        options.add(d)
                    }
                } else {
                    val optArr = item.optJSONArray("options")
                    if (optArr != null && optArr.length() > 0) {
                        for (idx in 0 until optArr.length()) {
                            options.add(optArr.getString(idx))
                        }
                    }
                }

                val correctIndex = when {
                    correctAns.equals("A", ignoreCase = true) -> 0
                    correctAns.equals("B", ignoreCase = true) -> 1
                    correctAns.equals("C", ignoreCase = true) -> 2
                    correctAns.equals("D", ignoreCase = true) -> 3
                    correctAns.startsWith("A", ignoreCase = true) -> 0
                    correctAns.startsWith("B", ignoreCase = true) -> 1
                    correctAns.startsWith("C", ignoreCase = true) -> 2
                    correctAns.startsWith("D", ignoreCase = true) -> 3
                    else -> -1
                }

                val fullExplanation = when {
                    rawExplanation.isBlank() -> "Verified Correct Answer: $correctAns"
                    rawExplanation.startsWith("Verified", ignoreCase = true) || rawExplanation.startsWith("Correct Answer", ignoreCase = true) -> rawExplanation
                    else -> "Verified Correct Answer: $correctAns\n\n$rawExplanation"
                }

                val year = if (id.contains("2025")) "2025" else if (id.contains("2023")) "2023" else "2024"

                if (qText.isNotBlank()) {
                    questions.add(
                        QuestionData(
                            id = id,
                            year = year,
                            subject = subject,
                            topic = topic,
                            question = qText,
                            options = options,
                            correctAnswer = correctIndex,
                            explanation = fullExplanation,
                            marks = marks,
                            difficulty = if (marks > 1) "Hard" else "Medium",
                            sourceType = SourceType.PYQ,
                            isVerified = true,
                            mode = PreparationMode.PROFESSIONAL_GATE,
                            userId = userId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedAllGateQuestions = questions
        return questions
    }

    fun parse(context: Context, userId: String): List<QuestionData> {
        return parseAllGateQuestions(context, userId)
    }

    fun parseGate2024Mcqs(context: Context, userId: String = ""): List<QuestionData> {
        return parseAllGateQuestions(context, userId)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var currentPart = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '\"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(currentPart.toString().trim())
                    currentPart = StringBuilder()
                }
                else -> currentPart.append(char)
            }
        }
        result.add(currentPart.toString().trim())
        return result
    }

    fun mapTopicToSubject(topic: String): String {
        val t = topic.lowercase().trim()
        return when {
            t.contains("network") -> "Computer Networks"
            t.contains("operating system") || t.contains("os") -> "Operating Systems"
            t.contains("database") || t.contains("dbms") -> "Databases"
            t.contains("algorithm") -> "Algorithms"
            t.contains("structure") || t.contains("programming") -> "Programming and Data Structures"
            t.contains("logic") || t.contains("digital") -> "Digital Logic"
            t.contains("theory of computation") || t.contains("toc") || t.contains("automata") -> "Theory of Computation"
            t.contains("compiler") -> "Compiler Design"
            t.contains("organization") || t.contains("architecture") || t.contains("coa") -> "Computer Organization and Architecture"
            t.contains("mathematics") || t.contains("discrete") || t.contains("calculus") || t.contains("linear algebra") || t.contains("probability") -> "Engineering Mathematics"
            t.contains("aptitude") -> "General Aptitude"
            else -> "Computer Science Core"
        }
    }

    private fun mapGateJsonQuestionToSubject(questionNumber: Int, section: String, text: String): Pair<String, String> {
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
}

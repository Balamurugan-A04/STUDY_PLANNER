package com.example.data

import android.content.Context
import com.example.model.PreparationMode
import com.example.model.QuestionData
import com.example.model.SourceType
import org.json.JSONArray
import org.json.JSONObject

object CsvQuestionParser {
    private var cachedAllGateQuestions: List<QuestionData>? = null

    fun normalizeQuestionText(text: String): String {
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
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun parseAllGateQuestions(context: Context, userId: String = ""): List<QuestionData> {
        cachedAllGateQuestions?.let { return it }

        val questions = mutableListOf<QuestionData>()
        val seenIds = mutableSetOf<String>()
        val seenNormalizedTexts = mutableSetOf<String>()

        // 1. Existing baseline 65 questions first
        // 2. Uploaded previous 3-year GATE question datasets
        val datasetFiles = listOf(
            "GATE_2023_CS_AI_Tutor.json.json",
            "GATE_2024_CS.json",
            "GATE_2023_CS.json",
            "GATE_2022_CS.json"
        )

        for (filename in datasetFiles) {
            try {
                val jsonStream = try {
                    context.assets.open(filename)
                } catch (e: Exception) {
                    if (filename == "GATE_2023_CS_AI_Tutor.json.json") {
                        context.assets.open("GATE_2023_CS_AI_Tutor.json")
                    } else {
                        continue
                    }
                }
                val jsonText = jsonStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonText)

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val id = item.optString("id", "${filename}_Q${i + 1}")
                    val qText = if (item.has("question_text")) item.optString("question_text", "").trim() else item.optString("question", "").trim()
                    if (qText.isBlank()) continue

                    val normText = normalizeQuestionText(qText)
                    if (seenIds.contains(id)) continue
                    if (normText.length >= 15 && seenNormalizedTexts.contains(normText)) continue

                    seenIds.add(id)
                    if (normText.length >= 15) {
                        seenNormalizedTexts.add(normText)
                    }

                    val qNum = item.optInt("question_number", i + 1)
                    val section = item.optString("section", "CS-1")
                    val qType = if (item.has("question_type")) item.optString("question_type") else item.optString("type", "MCQ")
                    val marks = item.optInt("marks", 1)
                    val correctAns = (if (item.has("correct_answer")) item.optString("correct_answer") else item.optString("answer", "")).trim()
                    val rawExplanation = item.optString("explanation", "").trim()

                    val (subject, topic) = mapGateJsonQuestionToSubject(filename, id, qNum, section, qText)

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
                                val optVal = optArr.optString(idx, "")
                                if (optVal.isNotBlank()) {
                                    options.add(optVal)
                                }
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

                    val year = if (item.has("year") && item.optString("year").isNotBlank()) {
                        item.optString("year")
                    } else {
                        when {
                            id.contains("2025") -> "2025"
                            id.contains("2024") -> "2024"
                            id.contains("2023") -> "2023"
                            id.contains("2022") -> "2022"
                            filename.contains("2024") -> "2024"
                            filename.contains("2023") -> "2023"
                            filename.contains("2022") -> "2022"
                            else -> "2024"
                        }
                    }

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
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    fun mapGateJsonQuestionToSubject(
        filename: String,
        id: String,
        questionNumber: Int,
        section: String,
        text: String
    ): Pair<String, String> {
        if (section.equals("GA", ignoreCase = true) || section.equals("General Aptitude", ignoreCase = true) || questionNumber in 1..10) {
            return Pair("General Aptitude", "General Aptitude")
        }

        if (filename.contains("AI_Tutor", ignoreCase = true) || id.startsWith("GATE2025", ignoreCase = true)) {
            return mapGate2025Question(questionNumber, text)
        }
        if (filename.contains("2024") || id.contains("2024")) {
            return mapGate2024Question(questionNumber, text)
        }
        if (filename.contains("2023") || id.contains("2023")) {
            return mapGate2023Question(questionNumber, text)
        }
        if (filename.contains("2022") || id.contains("2022")) {
            return mapGate2022Question(questionNumber, text)
        }

        return fallbackKeywordMapping(text)
    }

    fun mapGateJsonQuestionToSubject(questionNumber: Int, section: String, text: String): Pair<String, String> {
        return mapGateJsonQuestionToSubject("GATE_2023_CS_AI_Tutor.json.json", "GATE2025_CS1_Q$questionNumber", questionNumber, section, text)
    }

    private fun mapGate2025Question(questionNumber: Int, text: String): Pair<String, String> {
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
            else -> fallbackKeywordMapping(text)
        }
    }

    private fun mapGate2024Question(questionNumber: Int, text: String): Pair<String, String> {
        return when (questionNumber) {
            11 -> Pair("Computer Organization and Architecture", "DMA Controller & Data Transfer")
            12 -> Pair("Engineering Mathematics", "Propositional Logic")
            13 -> Pair("Programming and Data Structures", "C Functions & Parameter Evaluation")
            14 -> Pair("Computer Organization and Architecture", "IEEE 754 Floating Point")
            15 -> Pair("Algorithms", "Recurrence Relations & Asymptotic Analysis")
            16 -> Pair("Engineering Mathematics", "Calculus & Definite Integrals")
            17 -> Pair("Engineering Mathematics", "Linear Algebra & Graph Adjacency Matrix")
            18 -> Pair("Engineering Mathematics", "Probability & Combinatorics")
            19 -> Pair("Databases", "Transactions & Durability")
            20 -> Pair("Databases", "ER Model & Weak Entity Sets")
            21 -> Pair("Compiler Design", "Phases of Compiler & Outputs")
            22 -> Pair("Theory of Computation", "DFA & Regular Expressions")
            23 -> Pair("Computer Networks", "TCP/IP & Packet Forwarding")
            24 -> Pair("Operating Systems", "Virtual Memory Management Unit")
            25 -> Pair("Operating Systems", "Process States & Context Switching")
            26 -> Pair("Databases", "File Organization & Storage Efficiency")
            27 -> Pair("Databases", "Two-Phase Locking (2PL) Protocol")
            28 -> Pair("Computer Networks", "IPv4 Datagram Fragmentation")
            29 -> Pair("Compiler Design", "Syntax-Directed Definitions & Attributes")
            30 -> Pair("Digital Logic", "Boolean Algebra Identities")
            31 -> Pair("Computer Organization and Architecture", "Instruction Formats & Data Hazards")
            32 -> Pair("Computer Networks", "IPv4 Header Fields & Routing")
            33 -> Pair("Programming and Data Structures", "Pointers & String Manipulation in C")
            34 -> Pair("Engineering Mathematics", "Partial Orders & Linear Extensions")
            35 -> Pair("Algorithms", "Array Distance & Longest Subsequence")
            36 -> Pair("Programming and Data Structures", "Pointers & Array Arithmetic in C")
            37 -> Pair("Operating Systems", "CPU Scheduling SRTF & SJF")
            38 -> Pair("Computer Networks", "IPv4 Addressing & CIDR Prefixes")
            39 -> Pair("Programming and Data Structures", "Binary Search Trees & Traversals")
            40 -> Pair("Compiler Design", "LL(1) Parsing Table Construction")
            41 -> Pair("Theory of Computation", "NFA to Regular Expression")
            42 -> Pair("Programming and Data Structures", "Array Algorithms & Sliding Window")
            43 -> Pair("Compiler Design", "Intermediate Code & Triples Representation")
            44 -> Pair("Engineering Mathematics", "Random Variables & Expectations")
            45 -> Pair("Databases", "Relational Algebra & Cross Products")
            46 -> Pair("Operating Systems", "Concurrency & Semaphore Deadlocks")
            47 -> Pair("Engineering Mathematics", "Linear Algebra & Matrix Determinants")
            48 -> Pair("Programming and Data Structures", "Stack Operations & Permutations")
            49 -> Pair("Digital Logic", "Number Systems & Radix Conversion")
            50 -> Pair("Digital Logic", "Boolean Functions, Minterms & Maxterms")
            51 -> Pair("Algorithms", "Graph Theory & Spanning Tree Weights")
            52 -> Pair("Theory of Computation", "Context-Free Grammars & Derivations")
            53 -> Pair("Operating Systems", "Disk Access Latency & Transfer Time")
            54 -> Pair("Computer Networks", "TCP Congestion Control & CWND")
            55 -> Pair("Computer Networks", "Ethernet Collision Detection & Frame Size")
            56 -> Pair("Databases", "Functional Dependencies")
            57 -> Pair("Computer Organization and Architecture", "Instruction Formats & Opcode Encoding")
            58 -> Pair("Computer Organization and Architecture", "Pipeline Execution & Speedup")
            59 -> Pair("Algorithms", "Minimum Spanning Trees")
            60 -> Pair("Engineering Mathematics", "Graph Theory & Chromatic Number")
            61 -> Pair("Computer Organization and Architecture", "Processor Instruction Encoding")
            62 -> Pair("Theory of Computation", "Formal Languages & String Counting")
            63 -> Pair("Engineering Mathematics", "Group Theory & Modular Arithmetic")
            64 -> Pair("Operating Systems", "Two-Level Page Tables & Memory Allocation")
            65 -> Pair("Compiler Design", "SLR Grammar & LR Items")
            else -> fallbackKeywordMapping(text)
        }
    }

    private fun mapGate2023Question(questionNumber: Int, text: String): Pair<String, String> {
        return when (questionNumber) {
            11 -> Pair("Compiler Design", "Compiler Front-End & Back-End Phases")
            12 -> Pair("Algorithms", "Binary Heaps & Max-Heap Property")
            13 -> Pair("Programming and Data Structures", "Linked List Deletion Time Complexity")
            14 -> Pair("Theory of Computation", "DFA & Regular Languages")
            15 -> Pair("Engineering Mathematics", "Lucas Sequence & Recurrence Relations")
            16 -> Pair("Databases", "Relational Model & Arity/Degree")
            17 -> Pair("Computer Networks", "Stop-and-Wait Protocol & Efficiency")
            18 -> Pair("Engineering Mathematics", "Matrix Determinants & Row Operations")
            19 -> Pair("Theory of Computation", "NFA with Epsilon Transitions & Identifiers")
            20 -> Pair("Algorithms", "Hashing & Universal Hashing")
            21 -> Pair("Digital Logic", "Multiplexers & D-Latch Equivalence")
            22 -> Pair("Operating Systems", "Thread Context Switching & Saved Registers")
            23 -> Pair("Operating Systems", "User Mode to Kernel Mode Transitions")
            24 -> Pair("Theory of Computation", "Closure Properties of Formal Languages")
            25 -> Pair("Computer Networks", "OSPF Routing Protocol")
            26 -> Pair("Engineering Mathematics", "First-Order Predicate Logic")
            27 -> Pair("Operating Systems", "CPU Scheduling & Starvation")
            28 -> Pair("Engineering Mathematics", "Calculus, Maxima & Minima")
            29 -> Pair("Algorithms", "Asymptotic Notations & Complexities")
            30 -> Pair("Engineering Mathematics", "Adjacency Matrix & Eigenvalues")
            31 -> Pair("Engineering Mathematics", "Multiple Integrals & Definite Integration")
            32 -> Pair("Digital Logic", "Radix Number Systems & Base Conversion")
            33 -> Pair("Computer Organization and Architecture", "Pipelining Execution Time")
            34 -> Pair("Computer Organization and Architecture", "I/O Polling vs Interrupt Overhead")
            35 -> Pair("Programming and Data Structures", "C Static Variables & Function Calls")
            36 -> Pair("Compiler Design", "Activation Records & Call Trees")
            37 -> Pair("Compiler Design", "Control Flow Graphs & Live Variables")
            38 -> Pair("Operating Systems", "Semaphores & Concurrency Race Conditions")
            39 -> Pair("Theory of Computation", "Context-Free Grammars & Regularity")
            40 -> Pair("Theory of Computation", "Pushdown Automata & Acceptance by Empty Stack")
            41 -> Pair("Computer Organization and Architecture", "Assembly Code & Loop Translation")
            42 -> Pair("Computer Organization and Architecture", "Memory Decoders & Base Addresses")
            43 -> Pair("Digital Logic", "Sequential Circuits & State Transitions")
            44 -> Pair("Digital Logic", "Multiplexers & Boolean Logic Implementation")
            45 -> Pair("Computer Organization and Architecture", "IEEE 754 Floating-Point Multiplication")
            46 -> Pair("Algorithms", "Priority Queue & Max-Heap Operations")
            47 -> Pair("Programming and Data Structures", "Binary Trees & Subtree Sums")
            48 -> Pair("Engineering Mathematics", "Combinatorics & Permutations")
            49 -> Pair("Engineering Mathematics", "Equivalence Relations & Functions")
            50 -> Pair("Computer Networks", "TCP Sequence Numbers & Bandwidth-Delay")
            51 -> Pair("Engineering Mathematics", "Abstract Algebra & Symmetric Difference Group")
            52 -> Pair("Computer Networks", "HTTP Latency, DNS & Persistent Connections")
            53 -> Pair("Engineering Mathematics", "Probability & Independent Events")
            54 -> Pair("Algorithms", "Asymptotic Analysis of Nested Loops")
            55 -> Pair("Engineering Mathematics", "Graph Coloring & Greedy Coloring Bound")
            56 -> Pair("Algorithms", "Breadth-First Search Orderings on Hypercube")
            57 -> Pair("Operating Systems", "Page Replacement LRU & Page Faults")
            58 -> Pair("Operating Systems", "Multi-Level Page Table Levels")
            59 -> Pair("Programming and Data Structures", "Stack and Queue Sequence Simulation")
            60 -> Pair("Compiler Design", "Syntax-Directed Translation & Attribute Evaluation")
            61 -> Pair("Databases", "SQL Query Evaluation & Selection")
            62 -> Pair("Databases", "B+ Tree Index File Block Accesses")
            63 -> Pair("Theory of Computation", "Minimal DFA Construction")
            64 -> Pair("Computer Organization and Architecture", "Set-Associative Cache Tag Bits")
            65 -> Pair("Computer Networks", "IP Routing & Longest Prefix Match")
            else -> fallbackKeywordMapping(text)
        }
    }

    private fun mapGate2022Question(questionNumber: Int, text: String): Pair<String, String> {
        return when (questionNumber) {
            11 -> Pair("Algorithms", "Asymptotic Analysis & Polynomial Growth")
            12 -> Pair("Theory of Computation", "Finite Automata & Regular Expressions")
            13 -> Pair("Compiler Design", "LR Parsers & Grammar Classes")
            14 -> Pair("Databases", "Relational Schema & BCNF Normalization")
            15 -> Pair("Algorithms", "Linked List Reversal Time Complexity")
            16 -> Pair("Algorithms", "Hash Tables & Expected Key Distribution")
            17 -> Pair("Computer Organization and Architecture", "Direct Memory Access (DMA) Transfer")
            18 -> Pair("Digital Logic", "Two's Complement Representation & Arithmetic Overflow")
            19 -> Pair("Operating Systems", "Semaphore Synchronization & Process Ordering")
            20 -> Pair("Engineering Mathematics", "Linear Algebra & Matrix Trace Properties")
            21 -> Pair("Programming and Data Structures", "C Pointers & Memory Mutation")
            22 -> Pair("Computer Networks", "Network Topology & Subnet Identification")
            23 -> Pair("Theory of Computation", "Decidability & Formal Language Hierarchy")
            24 -> Pair("Computer Organization and Architecture", "Cache Policies (Write-Back vs Write-Through)")
            25 -> Pair("Databases", "Relational Algebra & Relational Division")
            26 -> Pair("Operating Systems", "Deadlocks & Resource Allocation Graphs")
            27 -> Pair("Engineering Mathematics", "Group Theory & Commutative Groups")
            28 -> Pair("Programming and Data Structures", "Binary Search Trees & Array Storage")
            29 -> Pair("Compiler Design", "LR Item Sets & Goto Operations")
            30 -> Pair("Engineering Mathematics", "Graph Theory & Disconnected Simple Graphs")
            31 -> Pair("Databases", "Functional Dependencies & Superkeys")
            32 -> Pair("Engineering Mathematics", "Combinatorics & Integer Partitions")
            33 -> Pair("Computer Organization and Architecture", "Memory Hierarchy & Average Memory Access Time")
            34 -> Pair("Engineering Mathematics", "Calculus & Limits (L'Hopital's Rule)")
            35 -> Pair("Computer Networks", "Iterative DNS Resolution & Query Pairs")
            36 -> Pair("Engineering Mathematics", "Generating Functions & Closed Forms")
            37 -> Pair("Engineering Mathematics", "Graph Adjacency Matrices & Cycles")
            38 -> Pair("Operating Systems", "Page Tables, TLB & Memory Access Latency")
            39 -> Pair("Databases", "Transaction Schedules & Conflict Serializability")
            40 -> Pair("Digital Logic", "Decoders, Multiplexers & Memory Address Lines")
            41 -> Pair("Computer Organization and Architecture", "IEEE 754 Floating-Point Arithmetic")
            42 -> Pair("Operating Systems", "CPU Scheduling & Round Robin Simulation")
            43 -> Pair("Programming and Data Structures", "Multidimensional Arrays & Pointer Offsets in C")
            44 -> Pair("Programming and Data Structures", "Bitwise Operations & ASCII Manipulation")
            45 -> Pair("Engineering Mathematics", "Linear Algebra & LU Decomposition")
            46 -> Pair("Theory of Computation", "Turing Machines & Undecidable Problems")
            47 -> Pair("Theory of Computation", "Regular and Context-Free Languages")
            48 -> Pair("Theory of Computation", "Context-Free Languages & Complements")
            49 -> Pair("Algorithms", "Minimum Spanning Trees & Distinct Edge Weights")
            50 -> Pair("Engineering Mathematics", "Graph Theory & Petersen Graph Properties")
            51 -> Pair("Algorithms", "Recurrence Relations & Binary Recurrence")
            52 -> Pair("Engineering Mathematics", "Graph Theory & Adjacency Matrix Properties")
            53 -> Pair("Engineering Mathematics", "Linear Algebra & Eigenvectors")
            54 -> Pair("Computer Organization and Architecture", "Cache Mapping & Direct-Mapped Conflicts")
            55 -> Pair("Computer Networks", "CIDR Route Aggregation & Subnets")
            56 -> Pair("Databases", "SQL Queries with EXCEPT and NOT EXISTS")
            57 -> Pair("Computer Networks", "Distance Vector Routing & Count-to-Infinity")
            58 -> Pair("Engineering Mathematics", "Matrix-Tree Theorem & Directed Spanning Trees")
            59 -> Pair("Computer Networks", "Transmission vs Propagation Delay")
            60 -> Pair("Computer Networks", "TCP Sequence Number Wrap-around & MSL")
            61 -> Pair("Computer Organization and Architecture", "Instruction Pipelining & Branch Prediction")
            62 -> Pair("Programming and Data Structures", "Queue Data Structures & Reversal Operations")
            63 -> Pair("Operating Systems", "File System Allocation (Contiguous vs Linked)")
            64 -> Pair("Operating Systems", "Demand Paging & LRU Page Fault Ratio")
            65 -> Pair("Compiler Design", "Syntax-Directed Translation & Precedence")
            else -> fallbackKeywordMapping(text)
        }
    }

    private fun fallbackKeywordMapping(text: String): Pair<String, String> {
        val lower = text.lowercase()
        return when {
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

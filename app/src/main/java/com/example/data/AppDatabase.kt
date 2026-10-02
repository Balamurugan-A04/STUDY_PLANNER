package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        SyllabusData::class,
        NoteData::class,
        QuestionData::class,
        MockTest::class,
        StudyPlan::class,
        SubjectData::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun syllabusDao(): SyllabusDao
    abstract fun noteDao(): NoteDao
    abstract fun questionDao(): QuestionDao
    abstract fun mockTestDao(): MockTestDao
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun subjectDao(): SubjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_study_planner_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subject_data ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE syllabus_topics ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE questions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE study_plans ADD COLUMN completedGoals TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE study_plans ADD COLUMN missedGoals TEXT NOT NULL DEFAULT ''")
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            // Seed GATE Syllabus
            val gateSyllabus = listOf(
                SyllabusData("s1", "Engineering Mathematics", "Linear Algebra", "Matrices, Determinants, Systems of Linear Equations, Eigenvalues", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s2", "Engineering Mathematics", "Calculus", "Limits, Continuity, Derivatives, Integration", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s3", "Engineering Mathematics", "Discrete Mathematics", "Propositional Logic, Sets, Relations, Functions, Graph Theory", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s4", "Digital Logic", "Boolean Algebra", "Combinational Circuits, Minimization, K-Maps", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s5", "Digital Logic", "Sequential Circuits", "Flip-Flops, Counters, Registers", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s6", "Computer Organization", "Machine Instructions", "Addressing Modes, ALU, Data Path, Control Unit", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s7", "Computer Organization", "Memory Hierarchy", "Cache Memory, Virtual Memory, Paging", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s8", "Programming & Data Structures", "C Programming", "Recursion, Arrays, Pointers, Structures", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s9", "Programming & Data Structures", "Data Structures", "Linked Lists, Stacks, Queues, Binary Trees, Heaps", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s10", "Algorithms", "Algorithm Analysis", "Asymptotic Analysis, Time & Space Complexity", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s11", "Algorithms", "Design Techniques", "Greedy, Divide and Conquer, Dynamic Programming", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s12", "Theory of Computation", "Automata Theory", "DFA, NFA, Regular Expressions, Pumping Lemma", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s13", "Compiler Design", "Parsing Techniques", "Lexical Analysis, LL(1), LR(1) Parsers, Syntax Trees", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s14", "Operating Systems", "Process Management", "CPU Scheduling, Synchronization, Deadlocks, Threads", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s15", "Databases", "Relational Model", "ER Model, Relational Algebra, SQL, Normalization (1NF to BCNF)", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s16", "Computer Networks", "Network Layers", "OSI & TCP/IP Model, IPv4/IPv6 Addressing, Routing, TCP/UDP", PreparationMode.PROFESSIONAL_GATE),
                SyllabusData("s17", "General Aptitude", "Verbal & Quantitative", "Reading Comprehension, Data Interpretation, Spatial Aptitude", PreparationMode.PROFESSIONAL_GATE)
            )

            db.syllabusDao().insertAll(gateSyllabus)

            // Seed GATE Notes
            val gateNotes = listOf(
                NoteData(
                    id = "n1",
                    subject = "Algorithms",
                    topic = "Dynamic Programming",
                    subtopic = "Core Principles",
                    content = "Dynamic Programming (DP) solves complex problems by breaking them into overlapping subproblems and storing subproblem answers (memoization or tabulation).\n\nKey Properties:\n1. Overlapping Subproblems: Same subproblems solved repeatedly.\n2. Optimal Substructure: Optimal solution to the problem contains optimal solutions to subproblems.\n\nClassic Examples:\n- 0/1 Knapsack\n- Longest Common Subsequence (LCS)\n- Matrix Chain Multiplication",
                    mode = PreparationMode.PROFESSIONAL_GATE
                ),
                NoteData(
                    id = "n2",
                    subject = "Operating Systems",
                    topic = "Process Synchronization",
                    subtopic = "Semaphores and Mutexes",
                    content = "Semaphores are integer variables used to solve the critical section problem.\n\nTwo Atomic Operations:\n1. wait(S) or P(S): Decrements semaphore value. If negative, process blocks.\n2. signal(S) or V(S): Increments semaphore value. Unblocks waiting processes.\n\nTypes:\n- Binary Semaphore (0 or 1): Functions like a Mutex lock.\n- Counting Semaphore: Controls access to finite resources.",
                    mode = PreparationMode.PROFESSIONAL_GATE
                ),
                NoteData(
                    id = "n3",
                    subject = "Computer Networks",
                    topic = "IP Addressing",
                    subtopic = "Subnetting & CIDR",
                    content = "CIDR (Classless Inter-Domain Routing) uses variable length subnet masking (VLSM).\n\nRepresentation: 192.168.1.0/24 means the first 24 bits are network address and remaining 8 bits are host addresses.\n\nCalculations:\n- Total IPs in /24 = 2^(32-24) = 256.\n- Usable Hosts = 256 - 2 = 254 (excluding Network ID and Broadcast ID).",
                    mode = PreparationMode.PROFESSIONAL_GATE
                ),
                NoteData(
                    id = "n4",
                    subject = "Digital Logic",
                    topic = "K-Maps",
                    subtopic = "Boolean Simplification",
                    content = "Karnaugh Maps (K-Maps) visually simplify Boolean algebra expressions up to 4 or 5 variables.\n\nRules:\n- Group adjacent cells in powers of 2 (1, 2, 4, 8, 16).\n- Make groups as large as possible.\n- Overlapping groups are allowed.\n- Don't Care conditions ('X') can be used as '1' or '0' to expand groups.",
                    mode = PreparationMode.PROFESSIONAL_GATE
                )
            )

            db.noteDao().insertAll(gateNotes)

            // Seed Questions (PYQs and Practice)
            val questions = listOf(
                QuestionData(
                    id = "q1",
                    year = "2024",
                    subject = "Algorithms",
                    topic = "Dynamic Programming",
                    question = "What is the worst-case time complexity of finding the Longest Common Subsequence of two strings of length m and n using dynamic programming?",
                    options = listOf("O(m + n)", "O(m * n)", "O(2^(m+n))", "O(m log n)"),
                    correctAnswer = 1,
                    explanation = "Dynamic programming fills an (m+1) x (n+1) matrix, yielding a time complexity of O(m * n).",
                    marks = 2,
                    difficulty = "Medium",
                    sourceType = SourceType.PYQ,
                    isVerified = true,
                    mode = PreparationMode.PROFESSIONAL_GATE
                ),
                QuestionData(
                    id = "q2",
                    year = "2023",
                    subject = "Operating Systems",
                    topic = "Process Scheduling",
                    question = "Which scheduling algorithm can cause starvation for longer processes?",
                    options = listOf("First Come First Served (FCFS)", "Round Robin (RR)", "Shortest Job First (SJF)", "Priority Scheduling without Aging"),
                    correctAnswer = 2,
                    explanation = "Shortest Job First (SJF) prioritizes shorter jobs, so continuous arrival of short jobs causes long jobs to suffer starvation.",
                    marks = 1,
                    difficulty = "Easy",
                    sourceType = SourceType.PYQ,
                    isVerified = true,
                    mode = PreparationMode.PROFESSIONAL_GATE
                ),
                QuestionData(
                    id = "q3",
                    year = "2023",
                    subject = "Computer Networks",
                    topic = "TCP/IP",
                    question = "In the TCP header, which flag is used to initiate a connection handshake?",
                    options = listOf("ACK", "SYN", "FIN", "RST"),
                    correctAnswer = 1,
                    explanation = "The SYN flag is sent in the 3-way handshake (SYN, SYN-ACK, ACK) to establish a TCP connection.",
                    marks = 1,
                    difficulty = "Easy",
                    sourceType = SourceType.PYQ,
                    isVerified = true,
                    mode = PreparationMode.PROFESSIONAL_GATE
                ),
                QuestionData(
                    id = "q4",
                    year = "2022",
                    subject = "Digital Logic",
                    topic = "Boolean Algebra",
                    question = "The minimum number of 2-input NAND gates required to implement a 2-input XOR gate is:",
                    options = listOf("3", "4", "5", "6"),
                    correctAnswer = 1,
                    explanation = "A 2-input XOR gate requires exactly 4 NAND gates.",
                    marks = 2,
                    difficulty = "Medium",
                    sourceType = SourceType.PYQ,
                    isVerified = true,
                    mode = PreparationMode.PROFESSIONAL_GATE
                ),
                QuestionData(
                    id = "q5",
                    year = "2024",
                    subject = "Theory of Computation",
                    topic = "Automata",
                    question = "Which of the following formal languages is NOT context-free?",
                    options = listOf("L = { a^n b^n | n >= 0 }", "L = { a^n b^n c^n | n >= 0 }", "L = { w w^R | w in {a,b}* }", "L = { a^m b^n | m, n >= 0 }"),
                    correctAnswer = 1,
                    explanation = "L = { a^n b^n c^n } requires matching three independent counts simultaneously, which cannot be done with a single pushdown stack, making it context-sensitive.",
                    marks = 2,
                    difficulty = "Hard",
                    sourceType = SourceType.PYQ,
                    isVerified = true,
                    mode = PreparationMode.PROFESSIONAL_GATE
                ),
                QuestionData(
                    id = "q6",
                    year = "AI Generated",
                    subject = "Databases",
                    topic = "SQL & Normalization",
                    question = "A relation R is in BCNF if for every functional dependency X -> Y:",
                    options = listOf("X is a superkey", "Y is a prime attribute", "X is a candidate key or Y is prime", "R is in 3NF only"),
                    correctAnswer = 0,
                    explanation = "Boyce-Codd Normal Form (BCNF) strictly requires that for every non-trivial functional dependency X -> Y, X must be a superkey.",
                    marks = 1,
                    difficulty = "Medium",
                    sourceType = SourceType.AI_PRACTICE,
                    isVerified = true,
                    mode = PreparationMode.PROFESSIONAL_GATE
                )
            )

            db.questionDao().insertAll(questions)

            // Seed initial study plans
            val gateStudyPlan = StudyPlan(
                mode = PreparationMode.PROFESSIONAL_GATE,
                dailyHours = 4,
                targetExam = "GATE CS 2026",
                dailyGoals = listOf("Algorithms - Dynamic Programming", "Solve 10 PYQs on OS", "Review Digital Logic notes"),
                weeklySchedule = listOf("Mon: Algorithms", "Tue: Operating Systems", "Wed: Computer Networks", "Thu: Databases", "Fri: Theory of Computation", "Sat: Mock Test", "Sun: Revision")
            )

            db.studyPlanDao().insertOrUpdate(gateStudyPlan)
        }
    }
}

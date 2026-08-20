package com.example.data

data class AcademicKnowledgeItem(
    val id: String,
    val question: String,
    val answer: String,
    val keywords: List<String>,
    val aliases: List<String>
)

object AcademicAiTutorData {

    val items: List<AcademicKnowledgeItem> = listOf(
        // 1. Define Mean?
        AcademicKnowledgeItem(
            id = "acad_qa_1",
            question = "Define Mean?",
            answer = "Mean is the average value of a set of numbers, calculated by dividing the sum of all values by the total number of values.\n\nFormula:\nMean = (Sum of all values) / (Total number of values)\nx̄ = Σx / n",
            keywords = listOf("mean", "average", "arithmetic mean", "calculate mean", "formula for mean", "find mean"),
            aliases = listOf(
                "define mean",
                "what is mean",
                "what is the mean",
                "explain mean",
                "meaning of mean",
                "calculate mean",
                "formula of mean",
                "what do you mean by mean",
                "tell me about mean",
                "mean definition"
            )
        ),
        // 2. What is Median?
        AcademicKnowledgeItem(
            id = "acad_qa_2",
            question = "What is Median?",
            answer = "Median is the middle value of a data set when arranged in ascending or descending order.\n\nIf n is odd:\nMedian = value at position (n+1)/2\n\nIf n is even:\nMedian = average of values at positions n/2 and (n/2)+1",
            keywords = listOf("median", "middle value", "calculate median", "find median", "formula for median", "odd", "even"),
            aliases = listOf(
                "what is median",
                "define median",
                "explain median",
                "meaning of median",
                "how to find median",
                "calculate median",
                "formula of median",
                "what do you mean by median",
                "tell me about median",
                "median definition"
            )
        ),
        // 3. Define MAD (Mobile Application Development)?
        AcademicKnowledgeItem(
            id = "acad_qa_3",
            question = "Define MAD (Mobile Application Development)?",
            answer = "MAD is the process of creating software applications that run on mobile devices like smartphones and tablets. It involves designing, coding, testing, and deploying apps for platforms like Android and iOS.\n\nThis is a conceptual topic and has no formula.",
            keywords = listOf("mad", "mobile application development", "define mad", "what is mad", "mobile app development", "mad definition"),
            aliases = listOf(
                "define mad",
                "what is mad",
                "define mad (mobile application development)",
                "define mobile application development",
                "what is mobile application development",
                "explain mad",
                "explain mobile application development",
                "what do you mean by mad",
                "mad definition",
                "mobile application development definition",
                "tell me about mad"
            )
        ),
        // 4. Uses of Cloud Computing?
        AcademicKnowledgeItem(
            id = "acad_qa_4",
            question = "Uses of Cloud Computing?",
            answer = "Cloud computing is used for data storage, backup, and online software services without needing local hardware. It also enables scalable computing power for businesses, apps, and websites.\n\nThis is a conceptual topic and has no formula.",
            keywords = listOf("cloud computing", "uses of cloud computing", "cloud storage", "cloud services", "applications of cloud computing", "cloud use"),
            aliases = listOf(
                "uses of cloud computing",
                "what are the uses of cloud computing",
                "why is cloud computing used",
                "where is cloud computing used",
                "applications of cloud computing",
                "benefits of cloud computing",
                "use of cloud computing",
                "explain uses of cloud computing",
                "how is cloud computing used",
                "cloud computing uses"
            )
        ),
        // 5. Define Android?
        AcademicKnowledgeItem(
            id = "acad_qa_5",
            question = "Define Android?",
            answer = "Android is an open-source operating system developed by Google, mainly used for mobile devices. It allows users to run apps, customize interfaces, and access Google services.\n\nThis is a conceptual topic and has no formula.",
            keywords = listOf("android", "define android", "what is android", "android os", "android operating system", "android definition"),
            aliases = listOf(
                "define android",
                "what is android",
                "what do you mean by android",
                "explain android",
                "meaning of android",
                "about android",
                "what is android os",
                "android definition",
                "tell me about android"
            )
        ),
        // 6. Uses of MAD (Mobile Application Development)?
        AcademicKnowledgeItem(
            id = "acad_qa_6",
            question = "Uses of MAD (Mobile Application Development)?",
            answer = "MAD is used to build apps for communication, entertainment, business, and education on mobile devices. It helps businesses reach users directly through smartphones with convenient, on-the-go services.",
            keywords = listOf("uses of mad", "uses of mobile application development", "mad uses", "applications of mad", "why mad is used", "purpose of mad", "mobile app uses"),
            aliases = listOf(
                "uses of mad",
                "uses of mad (mobile application development)",
                "uses of mobile application development",
                "what are the uses of mad",
                "what are the uses of mobile application development",
                "why is mad used",
                "why is mobile application development used",
                "where is mad used",
                "applications of mad",
                "benefits of mad",
                "benefits of mobile application development",
                "how is mad used",
                "mad uses"
            )
        )
    )

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private val stopWords = setOf(
        "what", "is", "the", "define", "explain", "of", "in", "and", "a", "an", "for", "to",
        "how", "do", "you", "mean", "by", "are", "tell", "me", "about", "describe", "can", "please"
    )

    fun findAnswer(userQuery: String): String {
        val trimmed = userQuery.trim()
        if (trimmed.isBlank()) {
            return "Please enter a question."
        }

        val cleanQuery = normalize(trimmed)
        if (cleanQuery.isEmpty()) {
            return "Please enter a valid question."
        }

        // Specific disambiguation between "Define MAD" vs "Uses of MAD"
        val isUsesQuery = cleanQuery.contains("use") || cleanQuery.contains("uses") ||
                cleanQuery.contains("application") || cleanQuery.contains("applications") ||
                cleanQuery.contains("benefit") || cleanQuery.contains("benefits") ||
                cleanQuery.contains("why") || cleanQuery.contains("purpose") || cleanQuery.contains("where")

        val isMadQuery = cleanQuery.contains("mad") ||
                cleanQuery.contains("mobile application") ||
                cleanQuery.contains("mobile app")

        if (isMadQuery) {
            return if (isUsesQuery) {
                formatItemResponse(items[5]) // Uses of MAD
            } else {
                formatItemResponse(items[2]) // Define MAD
            }
        }

        // Specific check for Cloud Computing
        if (cleanQuery.contains("cloud")) {
            return formatItemResponse(items[3]) // Uses of Cloud Computing
        }

        // Specific check for Android
        if (cleanQuery.contains("android")) {
            return formatItemResponse(items[4]) // Define Android
        }

        // Specific check for Median
        if (cleanQuery.contains("median")) {
            return formatItemResponse(items[1]) // What is Median?
        }

        // Specific check for Mean
        if (cleanQuery.contains("mean") && !cleanQuery.contains("meaning of") && !cleanQuery.contains("do you mean")) {
            return formatItemResponse(items[0]) // Define Mean?
        }
        if (cleanQuery == "mean" || cleanQuery == "define mean" || cleanQuery == "what is mean" || cleanQuery == "formula for mean" || cleanQuery.endsWith(" mean")) {
            return formatItemResponse(items[0]) // Define Mean?
        }

        // 1. Direct alias match
        for (item in items) {
            for (alias in item.aliases) {
                val cleanAlias = normalize(alias)
                if (cleanQuery == cleanAlias) {
                    return formatItemResponse(item)
                }
            }
        }

        // 2. Contains alias match
        for (item in items) {
            for (alias in item.aliases) {
                val cleanAlias = normalize(alias)
                if (cleanQuery.contains(cleanAlias) || cleanAlias.contains(cleanQuery)) {
                    if (cleanQuery.length >= 4 && cleanAlias.length >= 4) {
                        return formatItemResponse(item)
                    }
                }
            }
        }

        // 3. Keyword scoring match
        val queryTokens = cleanQuery.split(" ").filter { it.length > 2 && it !in stopWords }.toSet()
        var bestItem: AcademicKnowledgeItem? = null
        var maxScore = 0

        for (item in items) {
            var score = 0
            for (kw in item.keywords) {
                val cleanKw = normalize(kw)
                val kwTokens = cleanKw.split(" ").filter { it.isNotBlank() }
                if (cleanQuery.contains(cleanKw)) {
                    score += 5
                }
                val overlap = kwTokens.count { it in queryTokens }
                score += overlap * 2
            }
            if (score > maxScore) {
                maxScore = score
                bestItem = item
            }
        }

        if (bestItem != null && maxScore >= 4) {
            return formatItemResponse(bestItem)
        }

        // Unknown question
        return "I don't have this question in the current Academic AI Tutor dataset. Please ask a question related to the available Academic topics."
    }

    private fun formatItemResponse(item: AcademicKnowledgeItem): String {
        return buildString {
            append("Answer:\n")
            append(item.answer)
        }.trim()
    }
}

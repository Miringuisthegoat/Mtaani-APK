package com.benjamin.mtaani.ui.screens.reports

import com.benjamin.mtaani.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.withTimeout

object GeminiHelper {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    private const val TIMEOUT_MS = 15_000L

    // ─── Public API ───────────────────────────────────────────

    suspend fun generateCountyEmail(
        category: String,
        description: String,
        location: String
    ): GeminiResult {
        val prompt = """
            You are a civic reporting assistant for Mtaani, a community issue reporting app in Kenya.

            A citizen has reported the following issue:
            - Category: $category
            - Location: $location
            - Description: $description

            Generate a formal, professional email to the relevant Kenya county government department.

            The email should:
            1. Have a clear subject line
            2. Be addressed to the correct department based on the category:
               - Potholes/Roads → Roads and Transport Department
               - Garbage → Environment and Sanitation Department
               - Street Lights → Energy and Lighting Department
               - Water Leakage → Water and Sanitation Department
               - Drainage → Public Works Department
               - Other → County Executive Committee
            3. Clearly describe the issue in detail
            4. Include the exact location
            5. Mention this was reported via Mtaani Civic App
            6. Request prompt action and follow-up
            7. Be signed off as "Mtaani Civic Reporting Platform"
            8. Be polite but firm and urgent in tone

            Use this EXACT format and nothing else:
            SUBJECT: [subject here]

            BODY:
            [email body here]
        """.trimIndent()

        return generate(prompt) { text ->
            // Validate that the response has the expected markers
            if (!text.contains("SUBJECT:") || !text.contains("BODY:")) {
                GeminiResult.Error("Unexpected response format from AI. Please try again.")
            } else {
                GeminiResult.Success(text)
            }
        }
    }

    suspend fun improveDescription(rawDescription: String, category: String): GeminiResult {
        val prompt = """
            A Kenyan citizen is reporting a $category issue using the Mtaani civic app.
            Their raw description is: "$rawDescription"

            Rewrite this into a clear, detailed, professional issue report in 2-3 sentences.
            Keep it factual and specific. Do not add information that wasn't mentioned.
            Return only the improved description, nothing else.
        """.trimIndent()

        return generate(prompt)
    }

    suspend fun detectCategory(description: String): GeminiResult {
        val validCategories = setOf(
            "Garbage", "Potholes", "Street Lights",
            "Water Leakage", "Drainage", "Other"
        )
        val prompt = """
            Based on this issue description: "$description"

            Which ONE category does it belong to?
            Choose ONLY from: ${validCategories.joinToString(", ")}

            Return ONLY the category name, nothing else.
        """.trimIndent()

        return generate(prompt) { text ->
            val matched = validCategories.firstOrNull {
                text.trim().equals(it, ignoreCase = true)
            }
            if (matched != null) GeminiResult.Success(matched)
            else GeminiResult.Success("Other") // safe fallback if Gemini goes off-script
        }
    }

    suspend fun estimateSeverity(description: String, category: String): GeminiResult {
        val validLevels = setOf("Low", "Medium", "High", "Critical")
        val prompt = """
            Based on this $category issue in Kenya: "$description"

            Rate the severity level.
            Choose ONLY from: ${validLevels.joinToString(", ")}

            Return ONLY the severity level, nothing else.
        """.trimIndent()

        return generate(prompt) { text ->
            val matched = validLevels.firstOrNull {
                text.trim().equals(it, ignoreCase = true)
            }
            if (matched != null) GeminiResult.Success(matched)
            else GeminiResult.Success("Medium")
        }
    }

    // ─── Internal helpers ─────────────────────────────────────

    private suspend fun generate(
        prompt: String,
        validate: ((String) -> GeminiResult)? = null
    ): GeminiResult {
        return try {
            withTimeout(TIMEOUT_MS) {
                val response = model.generateContent(prompt)
                val text = response.text?.trim()
                if (text.isNullOrBlank()) {
                    GeminiResult.Error("AI returned an empty response. Please try again.")
                } else {
                    validate?.invoke(text) ?: GeminiResult.Success(text)
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            GeminiResult.Error("Request timed out. Check your connection and try again.")
        } catch (e: Exception) {
            GeminiResult.Error("AI error: ${e.message ?: "Unknown error"}")
        }
    }
}

sealed class GeminiResult {
    data class Success(val text: String) : GeminiResult()
    data class Error(val message: String) : GeminiResult()

    /** Convenience: unwrap or return a fallback */
    fun getOrDefault(default: String): String = when (this) {
        is Success -> text
        is Error -> default
    }
}
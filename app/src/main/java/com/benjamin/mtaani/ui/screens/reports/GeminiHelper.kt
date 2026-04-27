package com.benjamin.mtaani.ui.screens.reports

import com.benjamin.mtaani.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel

object GeminiHelper {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    // Analyse issue and generate formal email to county government
    suspend fun generateCountyEmail(
        category: String,
        description: String,
        location: String
    ): String {
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
            6. Request prompt action and follow up
            7. Be signed off as "Mtaani Civic Reporting Platform"
            8. Be polite but firm and urgent in tone
            
            Use this EXACT format and nothing else:
            SUBJECT: [subject here]
            
            BODY:
            [email body here]
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            response.text ?: "Failed to generate email. Please try again."
        } catch (e: Exception) {
            "Error generating email: ${e.message}"
        }
    }

    // Auto improve the user's description
    suspend fun improveDescription(rawDescription: String, category: String): String {
        val prompt = """
            A Kenyan citizen is reporting a $category issue using the Mtaani civic app.
            Their raw description is: "$rawDescription"
            
            Rewrite this into a clear, detailed, professional issue report in 2-3 sentences.
            Keep it factual and specific. Do not add information that wasn't mentioned.
            Return only the improved description, nothing else.
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            response.text?.trim() ?: rawDescription
        } catch (e: Exception) {
            rawDescription
        }
    }

    // Auto detect category from description
    suspend fun detectCategory(description: String): String {
        val prompt = """
            Based on this issue description: "$description"
            
            Which ONE category does it belong to?
            Choose ONLY from: Garbage, Potholes, Street Lights, Water Leakage, Drainage, Other
            
            Return ONLY the category name, nothing else.
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            response.text?.trim() ?: "Other"
        } catch (e: Exception) {
            "Other"
        }
    }

    // Estimate severity of the issue
    suspend fun estimateSeverity(description: String, category: String): String {
        val prompt = """
            Based on this $category issue in Kenya: "$description"
            
            Rate the severity level.
            Choose ONLY from: Low, Medium, High, Critical
            
            Return ONLY the severity level, nothing else.
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            response.text?.trim() ?: "Medium"
        } catch (e: Exception) {
            "Medium"
        }
    }
}
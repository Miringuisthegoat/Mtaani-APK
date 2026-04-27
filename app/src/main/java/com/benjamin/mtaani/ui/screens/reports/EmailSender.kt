package com.benjamin.mtaani.ui.screens.reports

import android.content.Intent
import android.net.Uri

object EmailSender {

    // General county government email for now
    private const val COUNTY_EMAIL = "info@nairobi.go.ke"

    // Parse subject from Gemini response
    fun parseSubject(geminiResponse: String): String {
        return try {
            val subjectLine = geminiResponse
                .lines()
                .firstOrNull { it.startsWith("SUBJECT:") }
            subjectLine?.removePrefix("SUBJECT:")?.trim()
                ?: "Community Issue Report — Mtaani App"
        } catch (e: Exception) {
            "Community Issue Report — Mtaani App"
        }
    }

    // Parse body from Gemini response
    fun parseBody(geminiResponse: String): String {
        return try {
            val bodyIndex = geminiResponse.indexOf("BODY:")
            if (bodyIndex != -1) {
                geminiResponse.substring(bodyIndex + 5).trim()
            } else {
                geminiResponse
            }
        } catch (e: Exception) {
            geminiResponse
        }
    }

    // Opens device email app with pre-filled content
    fun getEmailIntent(
        subject: String,
        body: String
    ): Intent {
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(COUNTY_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
    }
}
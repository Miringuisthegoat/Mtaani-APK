package com.benjamin.mtaani.ui.screens.reports

import android.content.Intent
import android.net.Uri

object EmailSender {

    private const val DEFAULT_COUNTY_EMAIL = "info@nairobi.go.ke"
    private const val DEFAULT_SUBJECT = "Community Issue Report — Mtaani App"

    // Category → specific department email (extend as you get real addresses)
    private val departmentEmails = mapOf(
        "Potholes"      to "roads@nairobi.go.ke",
        "Garbage"       to "environment@nairobi.go.ke",
        "Street Lights" to "energy@nairobi.go.ke",
        "Water Leakage" to "water@nairobi.go.ke",
        "Drainage"      to "publicworks@nairobi.go.ke"
    )

    data class ParsedEmail(
        val subject: String,
        val body: String,
        val recipientEmail: String
    )

    fun parse(geminiResponse: String, category: String = ""): ParsedEmail {
        val subject = extractSubject(geminiResponse)
        val body = extractBody(geminiResponse)
        val recipient = departmentEmails[category] ?: DEFAULT_COUNTY_EMAIL
        return ParsedEmail(subject, body, recipient)
    }

    fun getEmailIntent(parsed: ParsedEmail): Intent {
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(parsed.recipientEmail))
            putExtra(Intent.EXTRA_SUBJECT, parsed.subject)
            putExtra(Intent.EXTRA_TEXT, parsed.body)
        }
    }

    // ─── Parsing helpers ──────────────────────────────────────

    private fun extractSubject(response: String): String {
        val lines = response.lines()
        for (line in lines) {
            val trimmed = line.trim()
            // Match "SUBJECT:" with optional whitespace and any case
            if (trimmed.startsWith("SUBJECT:", ignoreCase = true)) {
                val value = trimmed.substringAfter(":").trim()
                if (value.isNotBlank()) return value
            }
        }
        // Fallback: first non-blank line that isn't a BODY marker
        return lines
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() && !it.startsWith("BODY:", ignoreCase = true) }
            ?: DEFAULT_SUBJECT
    }

    private fun extractBody(response: String): String {
        val bodyMarkerIndex = response.lines().indexOfFirst {
            it.trim().startsWith("BODY:", ignoreCase = true)
        }
        return if (bodyMarkerIndex != -1) {
            // Everything after the BODY: line
            response.lines()
                .drop(bodyMarkerIndex + 1)
                .joinToString("\n")
                .trim()
                .ifBlank { response.trim() }
        } else {
            // No BODY: marker — strip the SUBJECT line and use the rest
            response.lines()
                .filter { !it.trim().startsWith("SUBJECT:", ignoreCase = true) }
                .joinToString("\n")
                .trim()
        }
    }
}
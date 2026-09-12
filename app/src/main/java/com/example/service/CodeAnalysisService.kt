package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream

data class CodeProblem(
    val file: String,
    val line: Int,
    val error: String,
    val severity: String, // "CRITICAL", "ERROR", "WARNING", "INFO"
    val cause: String,
    val explanation: String,
    val suggestedFix: String
) {
    fun toStructuredReport(): String {
        return buildString {
            append("FILE: ").append(file).append("\n")
            append("LINE: ").append(line).append("\n")
            append("ERROR: ").append(error).append("\n")
            append("SEVERITY: ").append(severity).append("\n")
            append("CAUSE: ").append(cause).append("\n")
            append("EXPLANATION: ").append(explanation).append("\n")
            append("SUGGESTED FIX: ").append(suggestedFix).append("\n")
        }
    }
}

data class CodeAnalysisReport(
    val language: String,
    val totalProblems: Int,
    val problems: List<CodeProblem>,
    val fixedCode: String? = null,
    val summary: String,
    val zipManifestFound: Boolean = false,
    val zipGradleFound: Boolean = false,
    val scannedFilesCount: Int = 1
)

interface CodeAnalysisService {
    suspend fun analyzeCode(
        code: String,
        fileName: String = "snippet.kt",
        languageHint: String? = null,
        aiService: AIService? = null
    ): CodeAnalysisReport

    suspend fun fixCode(
        code: String,
        problems: List<CodeProblem>,
        languageHint: String? = null,
        aiService: AIService? = null
    ): String

    suspend fun analyzeZipProject(
        zipInputStream: InputStream,
        aiService: AIService? = null
    ): CodeAnalysisReport
}

class DefaultCodeAnalysisService : CodeAnalysisService {

    override suspend fun analyzeCode(
        code: String,
        fileName: String,
        languageHint: String?,
        aiService: AIService?
    ): CodeAnalysisReport = withContext(Dispatchers.IO) {
        val detectedLang = languageHint ?: inferLanguage(fileName, code)
        val problems = mutableListOf<CodeProblem>()
        val lines = code.lines()

        // 1. Static Rule Analysis
        for (i in lines.indices) {
            val lineNum = i + 1
            val line = lines[i]

            // Hardcoded Secrets / API Keys Check
            if (line.contains("(?i)(api[_-]?key|secret|password|auth_token)\\s*[:=]\\s*[\"'][a-zA-Z0-9_\\-]{12,}[\"']".toRegex())) {
                problems.add(
                    CodeProblem(
                        file = fileName,
                        line = lineNum,
                        error = "Hardcoded Secret / API Key detected",
                        severity = "CRITICAL",
                        cause = "Private credential stored in cleartext source code.",
                        explanation = "Hardcoding API keys inside source code risks credential compromise when decompiled or committed to version control.",
                        suggestedFix = "Move key to environment variable or BuildConfig: val key = BuildConfig.API_KEY"
                    )
                )
            }

            // Android Cleartext Traffic in Manifest / XML
            if (line.contains("android:usesCleartextTraffic=\"true\"")) {
                problems.add(
                    CodeProblem(
                        file = fileName,
                        line = lineNum,
                        error = "Cleartext HTTP traffic enabled",
                        severity = "WARNING",
                        cause = "android:usesCleartextTraffic set to true.",
                        explanation = "Cleartext traffic allows unencrypted HTTP connections that can be intercepted or manipulated via man-in-the-middle attacks.",
                        suggestedFix = "Enforce HTTPS traffic or configure a Network Security Config XML."
                    )
                )
            }

            // Exported receiver/activity without permission
            if (line.contains("android:exported=\"true\"") && (line.contains("<receiver") || line.contains("<service"))) {
                problems.add(
                    CodeProblem(
                        file = fileName,
                        line = lineNum,
                        error = "Exported component without permission requirement",
                        severity = "WARNING",
                        cause = "Component is exported without an android:permission attribute.",
                        explanation = "Any third-party application on the device could trigger or bind to this component.",
                        suggestedFix = "Set android:exported=\"false\" unless cross-app interaction is required, or specify an android:permission."
                    )
                )
            }

            // Deprecated Android syntaxes & Kotlin common pitfalls
            if (line.contains("AsyncTask<") || line.contains("AsyncTask.")) {
                problems.add(
                    CodeProblem(
                        file = fileName,
                        line = lineNum,
                        error = "Deprecated API: AsyncTask",
                        severity = "ERROR",
                        cause = "AsyncTask has been deprecated in Android API 30.",
                        explanation = "AsyncTask suffers from memory leaks, configuration change cancellation issues, and thread starvation.",
                        suggestedFix = "Refactor to Kotlin Coroutines (withContext(Dispatchers.IO)) or WorkManager."
                    )
                )
            }

            // Gradle deprecated configurations
            if (line.trim().startsWith("compile ") || line.trim().startsWith("testCompile ")) {
                problems.add(
                    CodeProblem(
                        file = fileName,
                        line = lineNum,
                        error = "Deprecated Gradle configuration: compile",
                        severity = "ERROR",
                        cause = "The 'compile' configuration was removed in Gradle 7+.",
                        explanation = "Using obsolete Gradle keyword prevents modern build system synchronization.",
                        suggestedFix = "Replace 'compile' with 'implementation' or 'api'."
                    )
                )
            }

            // Kotlin unsafe null assertion
            if (line.contains("!!") && !line.contains("//")) {
                problems.add(
                    CodeProblem(
                        file = fileName,
                        line = lineNum,
                        error = "Unsafe double-bang (!!) null assertion",
                        severity = "WARNING",
                        cause = "Explicit null assertion operator '!!' used.",
                        explanation = "Throws a NullPointerException at runtime if the operand evaluates to null.",
                        suggestedFix = "Use safe call '?.' with Elvis operator '?:' or proper null-check."
                    )
                )
            }

            // XML unclosed tags
            if (detectedLang == "xml" && line.contains("<") && !line.contains(">") && !line.endsWith("\\")) {
                problems.add(
                    CodeProblem(
                        file = fileName,
                        line = lineNum,
                        error = "Malformed XML tag syntax",
                        severity = "ERROR",
                        cause = "Tag opened with '<' without corresponding closing '>'.",
                        explanation = "XML parsers require well-formed element opening and closing brackets.",
                        suggestedFix = "Ensure proper closing angle bracket '>' on tag."
                    )
                )
            }

            // GitHub Actions security: run unpinned action
            if (detectedLang == "yaml" && line.contains("uses: actions/") && line.contains("@master")) {
                problems.add(
                    CodeProblem(
                        file = fileName,
                        line = lineNum,
                        error = "Mutable GitHub Actions reference (@master)",
                        severity = "WARNING",
                        cause = "Action pinned to mutable branch '@master'.",
                        explanation = "Mutable branch tags can change unexpectedly or introduce breaking supply-chain changes.",
                        suggestedFix = "Pin to a specific version release tag (e.g., @v4) or full commit SHA."
                    )
                )
            }
        }

        // Bracket balance check
        val openBraces = code.count { it == '{' }
        val closeBraces = code.count { it == '}' }
        if (openBraces != closeBraces && (detectedLang in listOf("kotlin", "java", "javascript", "css", "json"))) {
            problems.add(
                CodeProblem(
                    file = fileName,
                    line = lines.size,
                    error = "Unbalanced curly braces ({: $openBraces, }: $closeBraces)",
                    severity = "ERROR",
                    cause = "Mismatched curly braces detected in block structure.",
                    explanation = "A code block was opened with '{' but was never closed, or closed excessively.",
                    suggestedFix = "Balance closing curly braces '}' to close all opened class or function scopes."
                )
            )
        }

        // Generate summary
        val summary = if (problems.isEmpty()) {
            "No immediate syntax or security violations found in $fileName. Code adheres to standard conventions."
        } else {
            "Found ${problems.size} potential problem(s) in $fileName (${problems.count { it.severity == "CRITICAL" || it.severity == "ERROR" }} errors/critical, ${problems.count { it.severity == "WARNING" }} warnings)."
        }

        CodeAnalysisReport(
            language = detectedLang,
            totalProblems = problems.size,
            problems = problems,
            fixedCode = null,
            summary = summary
        )
    }

    override suspend fun fixCode(
        code: String,
        problems: List<CodeProblem>,
        languageHint: String?,
        aiService: AIService?
    ): String = withContext(Dispatchers.IO) {
        if (problems.isEmpty()) return@withContext code

        // If AI service is available with an active key, request intelligent fix preserving semantics
        if (aiService != null) {
            val fixPrompt = buildString {
                append("Fix the following code by addressing these detected issues:\n\n")
                for (p in problems) {
                    append("- Line ${p.line}: ${p.error} (${p.explanation})\n")
                }
                append("\nReturn ONLY the complete fixed code wrapped in a single markdown code block with appropriate language tag:\n\n")
                append(code)
            }
            val aiResult = aiService.generateResponse(
                prompt = fixPrompt,
                systemInstruction = "You are an expert software engineer. Provide corrected, working, clean code preserving user intent without omitting parts."
            )
            if (aiResult.codeSnippet != null && !aiResult.isError) {
                return@withContext aiResult.codeSnippet
            }
        }

        // Rule-based fallback fix transformations
        var fixed = code
        // Replace compile with implementation
        fixed = fixed.replace("(?m)^(\\s*)compile\\s+".toRegex(), "$1implementation ")
        // Replace !! with safe calls
        fixed = fixed.replace("!!.", "?.")
        // Replace usesCleartextTraffic with false
        fixed = fixed.replace("android:usesCleartextTraffic=\"true\"", "android:usesCleartextTraffic=\"false\"")

        fixed
    }

    override suspend fun analyzeZipProject(
        zipInputStream: InputStream,
        aiService: AIService?
    ): CodeAnalysisReport = withContext(Dispatchers.IO) {
        val problems = mutableListOf<CodeProblem>()
        var fileCount = 0
        var foundManifest = false
        var foundGradle = false
        var foundSettings = false

        try {
            val zis = ZipInputStream(zipInputStream)
            var entry = zis.nextEntry

            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory) {
                    fileCount++
                    val lower = name.lowercase()
                    if (lower.endsWith("androidmanifest.xml")) foundManifest = true
                    if (lower.endsWith("build.gradle") || lower.endsWith("build.gradle.kts")) foundGradle = true
                    if (lower.endsWith("settings.gradle") || lower.endsWith("settings.gradle.kts")) foundSettings = true

                    // Read sample content (up to 32KB per file for analysis)
                    if (lower.endsWith(".gradle") || lower.endsWith(".gradle.kts") || lower.endsWith(".xml") || lower.endsWith(".kt") || lower.endsWith(".java")) {
                        val buffer = ByteArray(32768)
                        val bytesRead = zis.read(buffer)
                        if (bytesRead > 0) {
                            val content = String(buffer, 0, bytesRead)
                            val subReport = analyzeCode(content, name, inferLanguage(name, content))
                            problems.addAll(subReport.problems)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        } catch (e: Exception) {
            problems.add(
                CodeProblem(
                    file = "project.zip",
                    line = 0,
                    error = "ZIP Reading Error: ${e.message}",
                    severity = "ERROR",
                    cause = "Failed to extract or traverse ZIP stream.",
                    explanation = "The archive file may be corrupted or encrypted.",
                    suggestedFix = "Provide a valid, unencrypted standard ZIP file."
                )
            )
        }

        // Structural project checks
        if (!foundManifest && fileCount > 0) {
            problems.add(
                CodeProblem(
                    file = "AndroidManifest.xml",
                    line = 1,
                    error = "Missing AndroidManifest.xml",
                    severity = "CRITICAL",
                    cause = "No AndroidManifest.xml found in project root or app module.",
                    explanation = "Every Android project must declare an AndroidManifest.xml to define components, permissions, and app entry points.",
                    suggestedFix = "Create an AndroidManifest.xml file in app/src/main/AndroidManifest.xml."
                )
            )
        }

        if (!foundGradle && fileCount > 0) {
            problems.add(
                CodeProblem(
                    file = "build.gradle.kts",
                    line = 1,
                    error = "Missing Gradle build script",
                    severity = "CRITICAL",
                    cause = "No build.gradle or build.gradle.kts detected in archive.",
                    explanation = "Android projects require Gradle build files to resolve dependencies and compile code.",
                    suggestedFix = "Include project-level and app-level build.gradle.kts files."
                )
            )
        }

        val summary = "Scanned $fileCount files in project archive. Found ${problems.size} diagnostic finding(s). " +
                "Manifest: ${if (foundManifest) "Present" else "Missing"}, Gradle: ${if (foundGradle) "Present" else "Missing"}, Settings: ${if (foundSettings) "Present" else "Missing"}."

        CodeAnalysisReport(
            language = "Android Project Archive (ZIP)",
            totalProblems = problems.size,
            problems = problems,
            summary = summary,
            zipManifestFound = foundManifest,
            zipGradleFound = foundGradle,
            scannedFilesCount = fileCount
        )
    }

    private fun inferLanguage(fileName: String, code: String): String {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".kt") || lowerName.endsWith(".kts") -> "kotlin"
            lowerName.endsWith(".java") -> "java"
            lowerName.endsWith(".xml") -> "xml"
            lowerName.endsWith(".gradle") -> "gradle"
            lowerName.endsWith(".js") || lowerName.endsWith(".ts") -> "javascript"
            lowerName.endsWith(".html") -> "html"
            lowerName.endsWith(".css") -> "css"
            lowerName.endsWith(".py") -> "python"
            lowerName.endsWith(".json") -> "json"
            lowerName.endsWith(".yml") || lowerName.endsWith(".yaml") -> "yaml"
            lowerName.endsWith(".sh") || lowerName.endsWith(".bash") -> "shell"
            code.contains("package ") && code.contains("fun ") -> "kotlin"
            code.contains("public class ") -> "java"
            code.trim().startsWith("<?xml") || code.trim().startsWith("<") -> "xml"
            code.trim().startsWith("{") || code.trim().startsWith("[") -> "json"
            else -> "text"
        }
    }
}

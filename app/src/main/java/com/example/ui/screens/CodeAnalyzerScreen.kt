package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.CodeProblem
import com.example.ui.components.CodeBlockView
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeAnalyzerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val report by viewModel.codeReport.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingCode.collectAsState()

    var codeText by remember {
        mutableStateOf(
            """// Sample Kotlin file with issues
package com.rdc.example

import android.os.AsyncTask

class DataRepository {
    // Dangerous cleartext API key
    val apiKey: String = "AIzaSyD_SECRET_KEY_12345678"
    
    fun fetchData(url: String?) {
        val length = url!!.length // Unsafe null assertion
        println("Fetching: " + length)
    }
}"""
        )
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedLangHint by remember { mutableStateOf("kotlin") }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // File picker for source/log files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(it)
                val content = stream?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
                codeText = content
                Toast.makeText(context, "Loaded file into editor", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ZIP picker for Android project analysis
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(it)
                if (stream != null) {
                    viewModel.analyzeZipFile(stream)
                    Toast.makeText(context, "Analyzing project ZIP...", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to inspect ZIP: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Code Analyzer", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Diagnostics, security & auto-fix", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Action Toolbar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Button(
                    onClick = { viewModel.analyzeCodeSnippet(codeText, "Snippet.${getFileExtension(selectedLangHint)}") },
                    enabled = !isAnalyzing && codeText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4B2), contentColor = Color(0xFF00382E))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analyze Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Button(
                    onClick = {
                        viewModel.fixCurrentCode(codeText) { fixed ->
                            codeText = fixed
                            Toast.makeText(context, "Applied suggested fixes!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isAnalyzing && report != null && report!!.problems.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388BFD), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fix this code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload File / Log", fontSize = 12.sp)
                }
            }
            item {
                Button(
                    onClick = { zipPickerLauncher.launch("application/zip") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Project ZIP", fontSize = 12.sp)
                }
            }
        }

        // Tabs: Source Code vs Diagnostic Report
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Color(0xFF00D4B2)
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Code Editor") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Diagnostic Report")
                        if (report != null && report!!.totalProblems > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFA7970))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("${report!!.totalProblems}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            )
        }

        if (isAnalyzing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF00D4B2))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Scanning code syntax, dependencies and security...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (selectedTabIndex == 0) {
            // Code Editor
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Language selector chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    val langs = listOf("kotlin", "java", "xml", "gradle", "javascript", "python", "json", "yaml", "shell")
                    items(langs) { lang ->
                        FilterChip(
                            selected = selectedLangHint == lang,
                            onClick = { selectedLangHint = lang },
                            label = { Text(lang.uppercase(), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00D4B2).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF00D4B2)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0D1117),
                        unfocusedContainerColor = Color(0xFF0D1117),
                        focusedBorderColor = Color(0xFF00D4B2),
                        unfocusedBorderColor = Color(0xFF30363D),
                        focusedTextColor = Color(0xFFE6EDF3),
                        unfocusedTextColor = Color(0xFFE6EDF3)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        } else {
            // Diagnostic Report View
            val rep = report
            if (rep == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No analysis report yet.", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Click 'Analyze Code' or upload a project ZIP to run diagnostics.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4B2))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(rep.summary, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    if (rep.problems.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2EA043).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2EA043)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "All checks passed! No syntax errors, security flaws, or dependency conflicts detected.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF2EA043),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(rep.problems) { problem ->
                            ProblemItemCard(problem)
                        }
                    }

                    if (!rep.fixedCode.isNullOrBlank()) {
                        item {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text("Suggested Corrected Code:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4B2))
                                Spacer(modifier = Modifier.height(6.dp))
                                CodeBlockView(code = rep.fixedCode, language = rep.language)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProblemItemCard(problem: CodeProblem) {
    val severityColor = when (problem.severity) {
        "CRITICAL" -> Color(0xFFFA7970)
        "ERROR" -> Color(0xFFFA7970)
        "WARNING" -> Color(0xFFD29922)
        else -> Color(0xFF388BFD)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF161B22),
        border = androidx.compose.foundation.BorderStroke(1.dp, severityColor.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Severity badge + File:Line
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(severityColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(problem.severity, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = severityColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${problem.file} : line ${problem.line}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF8B949E)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ERROR
            Text(text = problem.error, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Spacer(modifier = Modifier.height(6.dp))

            // CAUSE & EXPLANATION
            StructuredField(label = "CAUSE", value = problem.cause)
            StructuredField(label = "EXPLANATION", value = problem.explanation)

            Spacer(modifier = Modifier.height(4.dp))

            // SUGGESTED FIX
            Surface(
                color = Color(0xFF0D1117),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("SUGGESTED FIX:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4B2))
                    Text(problem.suggestedFix, fontSize = 12.sp, color = Color(0xFFE6EDF3), fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun StructuredField(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "$label: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B949E))
        Text(text = value, fontSize = 12.sp, color = Color(0xFFC9D1D9))
    }
}

private fun getFileExtension(lang: String): String {
    return when (lang) {
        "kotlin" -> "kt"
        "java" -> "java"
        "xml" -> "xml"
        "gradle" -> "gradle.kts"
        "javascript" -> "js"
        "python" -> "py"
        "json" -> "json"
        "yaml" -> "yml"
        "shell" -> "sh"
        else -> "txt"
    }
}

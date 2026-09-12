package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.SearchResultItem
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSearchScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val searchResponse by viewModel.lastSearchResponse.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var queryText by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Live Web Search", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Real-time information & source verification", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Query Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        placeholder = { Text("Search topics, weather, news...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00D4B2)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00D4B2),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (queryText.isNotBlank()) {
                                viewModel.executeWebSearch(queryText)
                            }
                        },
                        enabled = queryText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF00D4B2))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF00382E), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Search", tint = Color(0xFF00382E))
                        }
                    }
                }

                // Sample topic chips
                LazyRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val suggestions = listOf("Today's Weather", "Latest Android 15 features", "Top Tech News Today", "Kotlin 2.2 features")
                    items(suggestions) { sugg ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                queryText = sugg
                                viewModel.executeWebSearch(sugg)
                            },
                            label = { Text(sugg, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Search Results Content
        val response = searchResponse
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF00D4B2))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Fetching live search data from verified web indexes...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (response == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Real-Time Web Intelligence", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Enter a query to retrieve current facts, news, and source links.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Summary Card
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D4B2).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Summary for \"${response.query}\"", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4B2))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(response.summary, fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Verified Source Facts
                if (response.verifiedFacts.isNotEmpty()) {
                    item {
                        Text("Verified Facts from Sources", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    items(response.verifiedFacts) { fact ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF161B22),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2EA043), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(fact, fontSize = 12.sp, color = Color(0xFFE6EDF3), lineHeight = 18.sp)
                            }
                        }
                    }
                }

                // Uncertainties / Warnings
                if (response.uncertainInfo.isNotEmpty()) {
                    item {
                        Text("Uncertain / Unconfirmed", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD29922))
                    }
                    items(response.uncertainInfo) { un ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2A2312),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD29922)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFFD29922), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(un, fontSize = 12.sp, color = Color(0xFFF0E6D2), lineHeight = 18.sp)
                            }
                        }
                    }
                }

                // Sources list
                item {
                    Text("Source Links", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                items(response.sources) { src ->
                    SearchResultCard(src) { uriHandler.openUri(src.url) }
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(item: SearchResultItem, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF161B22),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.sourceName.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388BFD))
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.OpenInNew, contentDescription = "Open Link", modifier = Modifier.size(14.dp), tint = Color(0xFF8B949E))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.snippet, fontSize = 12.sp, color = Color(0xFF8B949E), maxLines = 3)
            Spacer(modifier = Modifier.height(6.dp))
            Text(item.url, fontSize = 10.sp, color = Color(0xFF58A6FF), maxLines = 1)
        }
    }
}

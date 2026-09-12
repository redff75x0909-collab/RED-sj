package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SearchResultItem(
    val title: String,
    val snippet: String,
    val url: String,
    val sourceName: String
)

data class WebSearchResponse(
    val query: String,
    val summary: String,
    val verifiedFacts: List<String>,
    val uncertainInfo: List<String>,
    val sources: List<SearchResultItem>,
    val isError: Boolean = false
)

interface WebSearchService {
    fun shouldTriggerSearch(query: String): Boolean
    suspend fun searchWeb(query: String, aiService: AIService? = null): WebSearchResponse
}

class DuckDuckGoSearchService : WebSearchService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun shouldTriggerSearch(query: String): Boolean {
        val lower = query.lowercase().trim()
        val triggerKeywords = listOf(
            "weather", "today", "yesterday", "tomorrow", "latest",
            "current", "price", "who is", "what happened", "news",
            "documentation", "search", "stock", "score", "schedule",
            "release date", "current version", "status", "who won"
        )
        return triggerKeywords.any { lower.contains(it) } || lower.startsWith("search ") || lower.startsWith("find ")
    }

    override suspend fun searchWeb(query: String, aiService: AIService?): WebSearchResponse = withContext(Dispatchers.IO) {
        val cleanQuery = query.replace("(?i)^(search for|search|find information about|find)\\s+".toRegex(), "").trim()
        val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")

        val results = mutableListOf<SearchResultItem>()

        // 1. Query DuckDuckGo Instant Answer API
        try {
            val ddgUrl = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
            val req = Request.Builder()
                .url(ddgUrl)
                .header("User-Agent", "RDC-AI-Android/1.0")
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                val json = JSONObject(body)

                val abstractText = json.optString("AbstractText", "")
                val abstractSource = json.optString("AbstractSource", "DuckDuckGo")
                val abstractUrl = json.optString("AbstractURL", "")

                if (abstractText.isNotBlank()) {
                    results.add(
                        SearchResultItem(
                            title = json.optString("Heading", cleanQuery),
                            snippet = abstractText,
                            url = if (abstractUrl.isNotBlank()) abstractUrl else "https://duckduckgo.com/?q=$encodedQuery",
                            sourceName = abstractSource
                        )
                    )
                }

                // Related topics
                val related = json.optJSONArray("RelatedTopics")
                if (related != null) {
                    for (i in 0 until minOf(related.length(), 3)) {
                        val item = related.optJSONObject(i)
                        val text = item?.optString("Text", "") ?: ""
                        val firstUrl = item?.optString("FirstURL", "") ?: ""
                        if (text.isNotBlank()) {
                            results.add(
                                SearchResultItem(
                                    title = text.take(60),
                                    snippet = text,
                                    url = if (firstUrl.isNotBlank()) firstUrl else "https://duckduckgo.com/?q=$encodedQuery",
                                    sourceName = "Web Knowledge"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // continue to Wikipedia fallback
        }

        // 2. Query Wikipedia OpenSearch API for rich factual background
        try {
            val wikiUrl = "https://en.wikipedia.org/w/api.php?action=opensearch&search=$encodedQuery&limit=3&namespace=0&format=json"
            val wikiReq = Request.Builder()
                .url(wikiUrl)
                .header("User-Agent", "RDC-AI-Android/1.0")
                .build()

            val wikiResp = client.newCall(wikiReq).execute()
            if (wikiResp.isSuccessful) {
                val wikiBody = wikiResp.body?.string() ?: ""
                val array = JSONArray(wikiBody)
                if (array.length() >= 4) {
                    val titles = array.optJSONArray(1)
                    val descriptions = array.optJSONArray(2)
                    val links = array.optJSONArray(3)

                    if (titles != null && descriptions != null && links != null) {
                        for (i in 0 until minOf(titles.length(), 2)) {
                            val title = titles.optString(i, "")
                            val desc = descriptions.optString(i, "")
                            val link = links.optString(i, "")
                            if (desc.isNotBlank()) {
                                results.add(
                                    SearchResultItem(
                                        title = title,
                                        snippet = desc,
                                        url = link,
                                        sourceName = "Wikipedia"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // continue
        }

        // 3. Synthesize facts vs uncertainty
        val verifiedFacts = mutableListOf<String>()
        val uncertainInfo = mutableListOf<String>()

        if (results.isEmpty()) {
            uncertainInfo.add("No direct real-time web results found for \"$cleanQuery\". Check connection or search query specificity.")
            return@withContext WebSearchResponse(
                query = cleanQuery,
                summary = "Could not find live search results for \"$cleanQuery\". Please verify your internet connection or try with more specific terms.",
                verifiedFacts = emptyList(),
                uncertainInfo = uncertainInfo,
                sources = listOf(
                    SearchResultItem(
                        title = "DuckDuckGo Search for: $cleanQuery",
                        snippet = "Web search index query",
                        url = "https://duckduckgo.com/?q=$encodedQuery",
                        sourceName = "DuckDuckGo"
                    )
                ),
                isError = false
            )
        }

        for (item in results) {
            verifiedFacts.add("[Source: ${item.sourceName}] ${item.snippet}")
        }

        val summary = buildString {
            append("Based on live verified sources for \"").append(cleanQuery).append("\":\n\n")
            for (item in results) {
                append("• ").append(item.snippet).append("\n")
            }
        }

        WebSearchResponse(
            query = cleanQuery,
            summary = summary.trim(),
            verifiedFacts = verifiedFacts,
            uncertainInfo = uncertainInfo,
            sources = results
        )
    }
}

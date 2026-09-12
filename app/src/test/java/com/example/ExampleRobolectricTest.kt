package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RDC AI", appName)
  }

  @Test
  fun `test code analyzer static rule detection`() {
    val service = com.example.service.DefaultCodeAnalysisService()
    val testCode = """
      class MySecret {
        val apiKey = "AIzaSyD_TEST_SECRET_KEY_123456"
      }
    """.trimIndent()

    kotlinx.coroutines.runBlocking {
      val report = service.analyzeCode(testCode, "MySecret.kt")
      assertEquals(1, report.problems.size)
      assertEquals("CRITICAL", report.problems[0].severity)
    }
  }
}

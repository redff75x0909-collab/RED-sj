# RDC AI — Personal Android AI Assistant

**RDC AI** is a personal, modern, multi-language Android AI assistant built with Kotlin and Jetpack Compose (Material 3).

---

## Key Features

1. **AI Chat & Multimodal Intelligence**
   - Natural conversations with memory and context.
   - Multilingual support: **Bangla (বাংলা), English, Hindi (हिन्दी), Arabic (العربية), Urdu (اردو), Spanish (Español), French (Français), German (Deutsch), Chinese (中文), Japanese (日本語)** with automatic script detection and manual override.
   - Configurable AI backend powered by Google Gemini models (`gemini-3.5-flash`, `gemini-3.1-pro-preview`).
   - Image analysis, OCR, visual problem solving, and document understanding.

2. **Voice Assistant**
   - Real-time speech-to-text with partial feedback.
   - Natural Text-to-Speech (TTS) with female & male voice options, adjustable speed rate (0.5x–2.0x), and auto-speech readout.
   - "Hey RDC" wake-word toggle.

3. **Screen Understanding & Control**
   - Built on Android's `AccessibilityService` (`RdcAccessibilityService`).
   - Understands on-screen text and hierarchy ("Read my screen", "What is on my screen?").
   - Dispatches safe gestures, scrolling ("Scroll down", "Scroll up"), and element clicking.
   - Strictly respects privacy: password/banking fields are skipped; never monitors silently.

4. **Live Web Search & Fact Attribution**
   - Real-time web index search via DuckDuckGo and Wikipedia APIs.
   - Categorizes verified source facts vs. uncertain info with clickable reference links.

5. **Code Analyzer & Auto-Fix**
   - Analyzes Kotlin, Java, XML, Gradle, JavaScript, Python, JSON, YAML, Shell scripts, Android Manifest, and GitHub Actions.
   - Structured diagnostic reports: `FILE:`, `LINE:`, `ERROR:`, `SEVERITY:`, `CAUSE:`, `EXPLANATION:`, `SUGGESTED FIX:`.
   - "Fix this code" one-tap automated patch generator.
   - Upload single source/log files or full Android Project ZIP archives for deep inspection.

6. **Phone & SMS Assistant with Busy Mode**
   - Incoming call detection and busy mode rejection.
   - Incoming SMS logging with AI-suggested replies.
   - Customizable busy response: *"I am busy right now. Please leave a message."*

7. **Privacy & Security First**
   - Stored in local, on-device Room database (`chat_messages`, `call_logs`, `sms_logs`).
   - One-tap history deletion.
   - No tracking, ads, or monetization analytics.

---

## Building the APK via GitHub Actions

1. Push or fork this repository to your GitHub account.
2. *(Optional)* Add your `GEMINI_API_KEY` in GitHub Repository **Settings** > **Secrets and variables** > **Actions** > **New repository secret**.
3. Go to the **Actions** tab in your repository.
4. Select the **Build RDC AI APK** workflow and click **Run workflow** (or push a commit to `main`).
5. Once complete, click on the workflow run and download the **`RDC-AI-Debug-APK`** artifact from the summary page.

---

## Local Development & Configuration

- **Android Studio**: Android Studio Ladybug / Meerkat or later
- **JDK**: Java 17
- **Min SDK**: API 26 (Android 8.0)
- **Target SDK**: API 36 (Android 16)
- **API Key**: Create a `.env` file in the project root containing:
  ```properties
  GEMINI_API_KEY=your_gemini_api_key_here
  ```
  Or enter it directly inside the app under **Settings > AI Engine Configuration**.

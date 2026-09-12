package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiElement(
    val text: String,
    val contentDescription: String?,
    val viewId: String?,
    val className: String?,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val bounds: Rect
)

data class ScreenSnapshot(
    val packageName: String?,
    val elements: List<UiElement>,
    val fullText: String,
    val timestamp: Long = System.currentTimeMillis()
)

interface ScreenAssistantService {
    val isConnected: StateFlow<Boolean>
    fun getActiveScreenSnapshot(): ScreenSnapshot?
    fun tapElementWithText(targetText: String): Boolean
    fun tapElementWithId(viewId: String): Boolean
    fun scroll(forward: Boolean = true): Boolean
    fun enterText(targetText: String, textToEnter: String): Boolean
    fun performBack(): Boolean
    fun performHome(): Boolean
    fun describeScreen(): String
}

class RdcAccessibilityService : AccessibilityService(), ScreenAssistantService {

    companion object {
        @Volatile
        var instance: RdcAccessibilityService? = null
            private set

        private val _connectedFlow = MutableStateFlow(false)
        val connectedFlow: StateFlow<Boolean> = _connectedFlow.asStateFlow()
    }

    override val isConnected: StateFlow<Boolean>
        get() = connectedFlow

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _connectedFlow.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events are processed on-demand when commands are given
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _connectedFlow.value = false
    }

    override fun getActiveScreenSnapshot(): ScreenSnapshot? {
        val root = rootInActiveWindow ?: return null
        val elements = mutableListOf<UiElement>()
        val textBuilder = StringBuilder()

        traverseNode(root, elements, textBuilder)

        return ScreenSnapshot(
            packageName = root.packageName?.toString(),
            elements = elements,
            fullText = textBuilder.toString().trim()
        )
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo?,
        elements: MutableList<UiElement>,
        textBuilder: StringBuilder
    ) {
        if (node == null) return

        // Security check: Never read secure password fields
        if (node.isPassword) {
            return
        }

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString()
        val viewId = node.viewIdResourceName
        val className = node.className?.toString()
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val displayText = if (text.isNotBlank()) text else desc ?: ""
        if (displayText.isNotBlank() || node.isClickable || node.isEditable) {
            elements.add(
                UiElement(
                    text = displayText,
                    contentDescription = desc,
                    viewId = viewId,
                    className = className,
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    isScrollable = node.isScrollable,
                    bounds = bounds
                )
            )
            if (displayText.isNotBlank()) {
                textBuilder.append(displayText).append("\n")
            }
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), elements, textBuilder)
        }
    }

    override fun tapElementWithText(targetText: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val cleanTarget = targetText.lowercase().trim()

        val matchingNodes = root.findAccessibilityNodeInfosByText(targetText)
        for (node in matchingNodes) {
            if (performClickOnNodeOrParent(node)) {
                return true
            }
        }

        // Fuzzy match by traversing active window directly
        return findAndClickFuzzy(root, cleanTarget)
    }

    private fun findAndClickFuzzy(node: AccessibilityNodeInfo?, target: String): Boolean {
        if (node == null) return false
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (text.contains(target) || desc.contains(target)) {
            if (performClickOnNodeOrParent(node)) return true
        }
        for (i in 0 until node.childCount) {
            if (findAndClickFuzzy(node.getChild(i), target)) return true
        }
        return false
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    override fun tapElementWithId(viewId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        for (node in nodes) {
            if (performClickOnNodeOrParent(node)) return true
        }
        return false
    }

    override fun scroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        if (findAndScrollNode(root, action)) return true

        // Fallback to gesture swipe
        return dispatchScrollGesture(forward)
    }

    private fun findAndScrollNode(node: AccessibilityNodeInfo?, action: Int): Boolean {
        if (node == null) return false
        if (node.isScrollable && node.performAction(action)) {
            return true
        }
        for (i in 0 until node.childCount) {
            if (findAndScrollNode(node.getChild(i), action)) return true
        }
        return false
    }

    private fun dispatchScrollGesture(forward: Boolean): Boolean {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()

        val startX = width / 2
        val startY = if (forward) height * 0.7f else height * 0.3f
        val endY = if (forward) height * 0.3f else height * 0.7f

        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(startX, endY)

        val stroke = GestureDescription.StrokeDescription(path, 0, 300)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGesture(gesture, null, null)
    }

    override fun enterText(targetText: String, textToEnter: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val matchingNodes = root.findAccessibilityNodeInfosByText(targetText)
        val targetNode = matchingNodes.firstOrNull { it.isEditable }
            ?: findFirstEditable(root)
            ?: return false

        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToEnter)
        return targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val res = findFirstEditable(node.getChild(i))
            if (res != null) return res
        }
        return null
    }

    override fun performBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    override fun performHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun describeScreen(): String {
        val snapshot = getActiveScreenSnapshot()
        if (snapshot == null || snapshot.elements.isEmpty()) {
            return "Screen assistant cannot read the active window. Please make sure Accessibility Permission is enabled."
        }

        val buttons = snapshot.elements.filter { it.isClickable && it.text.isNotBlank() }.map { it.text }
        val editables = snapshot.elements.filter { it.isEditable }

        return buildString {
            append("Screen Summary (${snapshot.packageName ?: "Active App"}):\n")
            if (buttons.isNotEmpty()) {
                append("• Visible interactive elements: ").append(buttons.take(6).joinToString(", ")).append("\n")
            }
            if (editables.isNotEmpty()) {
                append("• Input fields detected: ").append(editables.size).append("\n")
            }
            append("• Visible text preview: ").append(snapshot.fullText.take(200).replace("\n", " "))
        }
    }
}

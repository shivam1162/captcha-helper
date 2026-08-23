package com.hamara.helper

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern

class CaptchaAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "CaptchaHelper"
        private const val TARGET_PACKAGE = "co.median.android.jrejze"
        
        // Regex pattern to extract two numbers and operator
        private val MATH_PATTERN = Pattern.compile("(\d{1,4})\s*([+\-*\/xX×÷])\s*(\d{1,4})")
    }

    private var lastSolvedEquation = ""
    private var lastSolvedTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName != TARGET_PACKAGE) return

        val rootNode = rootInActiveWindow ?: return
        try {
            processScreen(rootNode)
        } finally {
            rootNode.recycle()
        }
    }

    private fun processScreen(rootNode: AccessibilityNodeInfo) {
        val mathMatch = findMathEquation(rootNode) ?: return
        val equationStr = mathMatch.equationText
        val calculatedAnswer = mathMatch.result

        val now = System.currentTimeMillis()
        if (equationStr == lastSolvedEquation && (now - lastSolvedTime) < 3000) {
            return
        }

        val inputNode = findCaptchaInputNode(rootNode)
        if (inputNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    calculatedAnswer.toString()
                )
            }
            val success = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            inputNode.recycle()

            if (success) {
                lastSolvedEquation = equationStr
                lastSolvedTime = now
                Log.d(TAG, "Successfully auto-filled CAPTCHA: $equationStr = $calculatedAnswer")
            }
        }
    }

    private data class MathMatch(val equationText: String, val result: Int)

    private fun findMathEquation(node: AccessibilityNodeInfo?): MathMatch? {
        if (node == null) return null

        val text = node.text?.toString()
        if (!text.isNullOrBlank()) {
            val matcher = MATH_PATTERN.matcher(text)
            if (matcher.find()) {
                val num1 = matcher.group(1)?.toIntOrNull() ?: 0
                val op = matcher.group(2) ?: "+"
                val num2 = matcher.group(3)?.toIntOrNull() ?: 0
                val res = when (op) {
                    "+" -> num1 + num2
                    "-" -> num1 - num2
                    "*", "x", "X", "×" -> num1 * num2
                    "/", "÷" -> if (num2 != 0) num1 / num2 else 0
                    else -> 0
                }
                return MathMatch("$num1 $op $num2", res)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val match = findMathEquation(child)
            if (match != null) {
                child?.recycle()
                return match
            }
            child?.recycle()
        }
        return null
    }

    private fun findCaptchaInputNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null

        val editNodes = mutableListOf<AccessibilityNodeInfo>()
        collectEditableNodes(root, editNodes)

        if (editNodes.isEmpty()) return null

        // In the login form:
        // Input 0: Username
        // Input 1: Password
        // Input 2: Captcha Answer
        if (editNodes.size >= 3) {
            val captchaNode = editNodes[2]
            for (i in editNodes.indices) {
                if (i != 2) editNodes[i].recycle()
            }
            return captchaNode
        }

        val lastNode = editNodes.last()
        for (i in 0 until editNodes.size - 1) {
            editNodes[i].recycle()
        }
        return lastNode
    }

    private fun collectEditableNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return

        if (node.isEditable || node.className?.toString()?.contains("EditText", ignoreCase = true) == true) {
            list.add(AccessibilityNodeInfo.obtain(node))
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            collectEditableNodes(child, list)
            child?.recycle()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "CaptchaAccessibilityService interrupted")
    }
}

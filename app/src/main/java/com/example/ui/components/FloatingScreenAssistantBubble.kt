package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartScreen
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interactive Movable Circular Dotted Voice Assistant Bubble
 * ("গুল আকৃতির ফুটা ফুটা ভাসমান সহকারী - নড়ানো যায় এবং কথা বলার সময় ধাপ ধাপ করে পালস দেয়")
 */
@Composable
fun FloatingScreenAssistantBubble(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isVisible by viewModel.isFloatingBubbleVisible.collectAsState()
    if (!isVisible) return

    val isListening by viewModel.isVoiceListening.collectAsState()
    val isSpeaking by viewModel.isVoiceSpeaking.collectAsState()
    val screenSummary by viewModel.screenSummary.collectAsState()
    val recognizedSpeech by viewModel.recognizedSpeech.collectAsState()

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    // Initial position: Bottom right area
    var offsetX by remember { mutableFloatStateOf(screenWidthPx - with(density) { 100.dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx - with(density) { 240.dp.toPx() }) }

    var isExpanded by remember { mutableStateOf(false) }

    // Two-step heartbeat pulsation ("ধাপ ধাপ করে পালস") animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val isActive = isListening || isSpeaking

    val stepPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                1.0f at 0
                1.26f at 220 using FastOutSlowInEasing // ১ম ধাপ পালস
                1.08f at 400 using FastOutSlowInEasing
                1.35f at 650 using FastOutSlowInEasing // ২য় ধাপ পালস
                1.0f at 1100
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "step_pulse"
    )

    val waveRippleRadius by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0.8f at 0
                1.6f at 1100
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_radius"
    )

    val waveRippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0.7f at 0
                0.0f at 1100
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha"
    )

    val idleBreath by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0.96f at 0
                1.04f at 1000
                0.96f at 2000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_breath"
    )

    val effectiveScale = if (isActive) stepPulseScale else idleBreath

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Draggable container
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = offsetX + dragAmount.x
                        val newY = offsetY + dragAmount.y
                        // Clamp within screen boundaries
                        offsetX = newX.coerceIn(20f, screenWidthPx - with(density) { 90.dp.toPx() })
                        offsetY = newY.coerceIn(80f, screenHeightPx - with(density) { 140.dp.toPx() })
                    }
                }
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Quick Action / Live Status Popup when expanded or active
                AnimatedVisibility(
                    visible = isExpanded || isListening || isSpeaking,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F2522).copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00D4B2)),
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .padding(bottom = 8.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isActive) Color(0xFF00FFB2) else Color(0xFF80CBC4))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            isListening -> "শুনছি... (বলুন)"
                                            isSpeaking -> "উত্তর দিচ্ছি (ফিমেল কণ্ঠ)"
                                            else -> "RDC AI Assistant"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE6FFF9)
                                    )
                                }
                                IconButton(
                                    onClick = { isExpanded = false },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                }
                            }

                            if (recognizedSpeech.isNotBlank() && isListening) {
                                Text(
                                    text = recognizedSpeech,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 2,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            // Quick Action Buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                // 1. Mic conversation button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isListening) Color(0xFFFF5252) else Color(0xFF004D40),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (isListening) viewModel.stopVoiceInput()
                                            else viewModel.startVoiceFromFloatingBubble()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                            contentDescription = "Mic",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isListening) "থামুন" else "কথা বলুন", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // 2. Screen Analyzer button ("মোবাইলের স্ক্রিন দেখা যাবে")
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF00382E),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.analyzeCurrentScreen(speakDescription = true)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.SmartScreen, contentDescription = "Screen", tint = Color(0xFF00FFB2), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("স্ক্রিন দেখুন", fontSize = 10.sp, color = Color(0xFF00FFB2), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // The Circular Dotted Animated Assistant Bubble ("গুল আকৃতির ফুটা ফুটা পালস দেওয়া বাবল")
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
                            if (isSpeaking) {
                                viewModel.stopSpeaking()
                            } else if (isListening) {
                                viewModel.stopVoiceInput()
                            } else {
                                isExpanded = !isExpanded
                                if (isExpanded) {
                                    // Instantly analyze screen and listen for user query
                                    viewModel.startVoiceFromFloatingBubble()
                                }
                            }
                        }
                ) {
                    // Custom Canvas: Concentric Dotted / Perforated Rings ("ফুটা ফুটা") and Step Pulse
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val baseRadius = (size.minDimension / 2f) * 0.72f
                        val currentRadius = baseRadius * effectiveScale

                        // 1. Expanding Pulse Ripple Wave when speaking or listening ("ধাপ ধাপ করে পালস দিবে")
                        if (isActive) {
                            val rippleRadius = baseRadius * waveRippleRadius
                            drawCircle(
                                color = Color(0xFF00FFB2).copy(alpha = waveRippleAlpha.coerceIn(0f, 1f)),
                                radius = rippleRadius,
                                center = center,
                                style = Stroke(
                                    width = 2.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                                )
                            )
                        }

                        // 2. Outer Perforated Dotted Ring ("ফুটা ফুটা গোল আকৃতি")
                        val outerDotCount = 20
                        val outerDotRadius = currentRadius * 0.98f
                        val dotSize = if (isActive) 3.5f else 2.5f

                        for (i in 0 until outerDotCount) {
                            val angle = (2 * PI / outerDotCount) * i
                            val dotX = center.x + (outerDotRadius * cos(angle)).toFloat()
                            val dotY = center.y + (outerDotRadius * sin(angle)).toFloat()
                            val dotColor = when {
                                isListening -> if (i % 2 == 0) Color(0xFFFF5252) else Color(0xFFFFD54F)
                                isSpeaking -> if (i % 2 == 0) Color(0xFF00FFB2) else Color(0xFF00D4B2)
                                else -> Color(0xFF00D4B2).copy(alpha = 0.7f)
                            }
                            drawCircle(
                                color = dotColor,
                                radius = dotSize,
                                center = Offset(dotX, dotY)
                            )
                        }

                        // 3. Inner Secondary Dotted Ring
                        val innerDotCount = 12
                        val innerDotRadius = currentRadius * 0.70f
                        for (i in 0 until innerDotCount) {
                            val angle = (2 * PI / innerDotCount) * i + (PI / innerDotCount)
                            val dotX = center.x + (innerDotRadius * cos(angle)).toFloat()
                            val dotY = center.y + (innerDotRadius * sin(angle)).toFloat()
                            drawCircle(
                                color = if (isActive) Color(0xFFE6FFF9) else Color(0xFF80CBC4).copy(alpha = 0.5f),
                                radius = 1.8f,
                                center = Offset(dotX, dotY)
                            )
                        }

                        // 4. Circular Glowing Orb Core
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = when {
                                    isListening -> listOf(Color(0xFFFF5252).copy(alpha = 0.9f), Color(0xFF5C0000))
                                    isSpeaking -> listOf(Color(0xFF00D4B2).copy(alpha = 0.9f), Color(0xFF00382E))
                                    else -> listOf(Color(0xFF005143).copy(alpha = 0.95f), Color(0xFF00241E))
                                },
                                center = center,
                                radius = currentRadius * 0.65f
                            ),
                            radius = currentRadius * 0.65f,
                            center = center
                        )

                        // 5. Border around core
                        drawCircle(
                            color = if (isActive) Color(0xFF00FFB2) else Color(0xFF00D4B2),
                            radius = currentRadius * 0.65f,
                            center = center,
                            style = Stroke(width = 1.8f)
                        )
                    }

                    // Center Icon / Symbol
                    Icon(
                        imageVector = when {
                            isListening -> Icons.Default.GraphicEq
                            isSpeaking -> Icons.Default.VolumeUp
                            else -> Icons.Default.SmartScreen
                        },
                        contentDescription = "RDC Voice Assistant",
                        tint = when {
                            isListening -> Color.White
                            isSpeaking -> Color(0xFFE6FFF9)
                            else -> Color(0xFF00FFB2)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

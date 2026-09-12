package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.RdcApplication
import com.example.data.model.CallLogItem
import com.example.data.model.SmsLogItem
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSmsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val callLogs by viewModel.callLogs.collectAsState()
    val smsLogs by viewModel.smsLogs.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var busyMessageText by remember { mutableStateOf(settings.busyMessage) }
    val context = LocalContext.current
    val app = context.applicationContext as RdcApplication

    // Permission launcher for phone/sms features
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val grantedCount = perms.values.count { it }
        Toast.makeText(context, "$grantedCount permissions granted", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Phone & SMS Assistant", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Busy mode, call management & AI replies", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Busy Mode Master Card
        Surface(
            color = if (settings.busyMode) Color(0xFF381015) else MaterialTheme.colorScheme.surface,
            border = if (settings.busyMode) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFA7970)) else null,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DoNotDisturbOn,
                        contentDescription = null,
                        tint = if (settings.busyMode) Color(0xFFFA7970) else Color(0xFF00D4B2),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (settings.busyMode) "Busy Mode Active" else "Busy Mode Off",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (settings.busyMode) Color(0xFFFFA198) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Automated call rejection & custom SMS reply",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = settings.busyMode,
                        onCheckedChange = { enabled ->
                            viewModel.updateBusyMode(enabled)
                            if (enabled) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_PHONE_STATE,
                                        Manifest.permission.ANSWER_PHONE_CALLS,
                                        Manifest.permission.READ_CONTACTS,
                                        Manifest.permission.SEND_SMS
                                    )
                                )
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFA7970),
                            uncheckedThumbColor = Color(0xFF8B949E),
                            uncheckedTrackColor = Color(0xFF21262D)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Busy message editor
                OutlinedTextField(
                    value = busyMessageText,
                    onValueChange = {
                        busyMessageText = it
                        viewModel.updateBusyMessage(it)
                    },
                    label = { Text("Custom Busy Response") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D4B2),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = settings.autoReplySms,
                        onCheckedChange = { viewModel.updateAutoReplySms(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto-reply SMS to missed calls/messages", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Tabs: Call History vs SMS History
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Color(0xFF00D4B2)
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Call Logs (${callLogs.size})") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("SMS Logs (${smsLogs.size})") }
            )
        }

        // Logs Lists
        if (selectedTabIndex == 0) {
            // Call Logs
            if (callLogs.isEmpty()) {
                EmptyLogPlaceholder(icon = Icons.Default.Phone, title = "No Call Activity Logged", subtitle = "Incoming calls will appear here when detected.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(callLogs) { call ->
                        CallItemCard(call)
                    }
                }
            }
        } else {
            // SMS Logs
            if (smsLogs.isEmpty()) {
                EmptyLogPlaceholder(icon = Icons.Default.Sms, title = "No SMS Messages Logged", subtitle = "Incoming SMS and suggested replies will be logged here.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(smsLogs) { sms ->
                        SmsItemCard(sms = sms, onSendReply = { reply ->
                            val sent = app.phoneSmsService.sendSms(sms.senderNumber, reply)
                            if (sent) {
                                Toast.makeText(context, "Reply sent to ${sms.senderNumber}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "SMS send failed. Check SEND_SMS permission.", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun CallItemCard(call: CallLogItem) {
    val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(call.timestamp))
    val isBusyRejected = call.callType == "REJECTED_BUSY"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF161B22),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isBusyRejected) Color(0xFFFA7970).copy(alpha = 0.4f) else Color(0xFF30363D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isBusyRejected) Color(0xFFFA7970).copy(alpha = 0.2f) else Color(0xFF388BFD).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBusyRejected) Icons.Default.CallMissed else Icons.Default.Call,
                    contentDescription = null,
                    tint = if (isBusyRejected) Color(0xFFFA7970) else Color(0xFF388BFD),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.contactName ?: call.phoneNumber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (call.contactName != null) {
                    Text(text = call.phoneNumber, fontSize = 11.sp, color = Color(0xFF8B949E))
                }
                Text(
                    text = "$dateStr • ${if (isBusyRejected) "Auto-rejected (Busy)" else "Incoming Call"}",
                    fontSize = 11.sp,
                    color = if (isBusyRejected) Color(0xFFFFA198) else Color(0xFF8B949E)
                )
            }
        }
    }
}

@Composable
fun SmsItemCard(sms: SmsLogItem, onSendReply: (String) -> Unit) {
    val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(sms.timestamp))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF161B22),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sms.senderName ?: sms.senderNumber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(dateStr, fontSize = 11.sp, color = Color(0xFF8B949E))
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(sms.messageBody, fontSize = 13.sp, color = Color(0xFFE6EDF3))

            // AI suggested reply
            if (!sms.suggestedReply.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF0D1117),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D4B2).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Suggested Reply:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4B2))
                            Text(sms.suggestedReply, fontSize = 12.sp, color = Color(0xFFC9D1D9))
                        }
                        IconButton(onClick = { onSendReply(sms.suggestedReply) }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFF00D4B2))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLogPlaceholder(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

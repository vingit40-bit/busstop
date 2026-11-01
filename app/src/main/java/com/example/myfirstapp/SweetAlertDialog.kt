package com.example.myfirstapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

enum class SweetAlertType {
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
    CONFIRM
}

@Composable
fun SweetAlertDialog(
    type: SweetAlertType = SweetAlertType.SUCCESS,
    title: String,
    message: String,
    confirmText: String = if (type == SweetAlertType.CONFIRM || type == SweetAlertType.WARNING) "Yes, Proceed" else "OK",
    cancelText: String? = if (type == SweetAlertType.CONFIRM || type == SweetAlertType.WARNING) "Cancel" else null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val mainColor = when (type) {
        SweetAlertType.SUCCESS -> Color(0xFF116530)
        SweetAlertType.WARNING -> Color(0xFFE65100)
        SweetAlertType.ERROR -> Color(0xFFC62828)
        SweetAlertType.INFO -> Color(0xFF1565C0)
        SweetAlertType.CONFIRM -> Color(0xFF116530)
    }

    val badgeBgColor = when (type) {
        SweetAlertType.SUCCESS -> Color(0xFFE8F5E9)
        SweetAlertType.WARNING -> Color(0xFFFFF3E0)
        SweetAlertType.ERROR -> Color(0xFFFFEBEE)
        SweetAlertType.INFO -> Color(0xFFE3F2FD)
        SweetAlertType.CONFIRM -> Color(0xFFE8F5E9)
    }

    val badgeIcon = when (type) {
        SweetAlertType.SUCCESS -> Icons.Default.CheckCircle
        SweetAlertType.WARNING -> Icons.Default.Warning
        SweetAlertType.ERROR -> Icons.Default.Error
        SweetAlertType.INFO -> Icons.Default.Info
        SweetAlertType.CONFIRM -> Icons.AutoMirrored.Filled.Help
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Sweet Alert Circular Badge Icon
                Surface(
                    shape = CircleShape,
                    color = badgeBgColor,
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = mainColor,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!cancelText.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text(cancelText, color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = mainColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

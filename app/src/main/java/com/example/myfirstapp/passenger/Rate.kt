package com.example.myfirstapp.passenger

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.TransitGreen

data class DriverRating(
    val ratingId: String = "RATING-" + System.currentTimeMillis().toString().takeLast(6),
    val busId: String,
    val busNumber: String,
    val driverName: String,
    val routeName: String,
    val ratingStars: Int,
    val feedbackText: String,
    val commuterName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun CommuterRateDialog(
    busNumber: String,
    driverName: String,
    routeName: String,
    commuterName: String,
    onDismiss: () -> Unit,
    onRatingSubmitted: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedStars by remember { mutableIntStateOf(5) }
    var feedbackText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val quickTags = listOf(
        "Safe Driving 🚗",
        "Clean Bus 🧼",
        "Polite Driver 😊",
        "On Time ⏰",
        "Cold AC ❄️"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = TransitGreen,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Rate Your Trip & Driver",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                Text(
                    text = "Bus $busNumber • $routeName",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Driver Name Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF5F8F6),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = TransitGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Driver", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = driverName.ifBlank { "Assigned Driver" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Star Rating Selector
                Text("Tap stars to rate:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (star in 1..5) {
                        IconButton(
                            onClick = { selectedStars = star },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= selectedStars) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = "$star Stars",
                                tint = if (star <= selectedStars) Color(0xFFFFB300) else Color.LightGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (selectedStars) {
                        5 -> "Excellent! 🌟🌟🌟🌟🌟"
                        4 -> "Very Good! 👍"
                        3 -> "Average Trip 👌"
                        2 -> "Needs Improvement 😕"
                        else -> "Poor Experience 👎"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TransitGreen
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Tags
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickTags) { tag ->
                        val isSelected = feedbackText.contains(tag)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                feedbackText = if (isSelected) {
                                    feedbackText.replace("$tag ", "").replace(tag, "").trim()
                                } else {
                                    if (feedbackText.isBlank()) tag else "$feedbackText • $tag"
                                }
                            },
                            label = { Text(tag, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE8F5E9),
                                selectedLabelColor = TransitGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Feedback TextField
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = { Text("Write your review (optional)...", fontSize = 12.sp, color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TransitGreen,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    isSubmitting = true
                    val ratingObj = DriverRating(
                        busId = busNumber,
                        busNumber = busNumber,
                        driverName = driverName,
                        routeName = routeName,
                        ratingStars = selectedStars,
                        feedbackText = feedbackText.trim(),
                        commuterName = commuterName
                    )

                    DatabaseManager.submitDriverRating(
                        rating = ratingObj,
                        onSuccess = {
                            isSubmitting = false
                            Toast.makeText(context, "Thank you for rating Driver $driverName!", Toast.LENGTH_SHORT).show()
                            onRatingSubmitted()
                            onDismiss()
                        },
                        onFailure = { err ->
                            isSubmitting = false
                            Toast.makeText(context, "Thank you for your rating!", Toast.LENGTH_SHORT).show()
                            onRatingSubmitted()
                            onDismiss()
                        }
                    )
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = TransitGreen)
            ) {
                Text(if (isSubmitting) "Submitting..." else "Submit Rating", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.DarkGray)
            }
        },
        containerColor = Color.White
    )
}

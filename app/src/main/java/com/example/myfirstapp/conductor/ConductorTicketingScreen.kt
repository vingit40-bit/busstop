package com.example.myfirstapp.conductor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as GColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.BookingRecord
import com.example.myfirstapp.DatabaseManager
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun printRealThermalReceipt(context: Context, booking: BookingRecord) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    val printAdapter = object : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: Bundle?
        ) {
            callback?.onLayoutFinished(
                PrintDocumentInfo.Builder("TransitGo_Receipt_${booking.bookingId}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build(),
                true
            )
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback?
        ) {
            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(320, 550, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas
                val paint = Paint().apply {
                    color = GColor.BLACK
                    textSize = 11f
                    isAntiAlias = true
                }

                val boldPaint = Paint().apply {
                    color = GColor.BLACK
                    textSize = 12f
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                canvas.drawText("========================================", 10f, 25f, boldPaint)
                canvas.drawText("          TRANSITGO BUS SYSTEM          ", 10f, 45f, boldPaint)
                canvas.drawText("        OFFICIAL PASSENGER TICKET       ", 10f, 62f, paint)
                canvas.drawText("========================================", 10f, 80f, boldPaint)
                
                canvas.drawText("Ticket ID  : ${booking.bookingId}", 10f, 110f, paint)
                canvas.drawText("Date/Time  : $timeStr", 10f, 130f, paint)
                canvas.drawText("Bus ID     : ${booking.busId}", 10f, 150f, paint)
                canvas.drawText("Route      : ${booking.routeName}", 10f, 170f, paint)
                canvas.drawText("Seat(s)    : ${booking.seats}", 10f, 190f, boldPaint)
                canvas.drawText("Payment    : ${booking.paymentMethod}", 10f, 210f, paint)
                canvas.drawText("Status     : PAID / VERIFIED", 10f, 230f, boldPaint)

                canvas.drawText("----------------------------------------", 10f, 255f, paint)
                canvas.drawText("TOTAL FARE : ₱%.2f".format(booking.totalPrice), 10f, 280f, boldPaint)
                canvas.drawText("----------------------------------------", 10f, 300f, paint)

                canvas.drawText("     THANK YOU FOR RIDING WITH US!      ", 10f, 340f, paint)
                canvas.drawText("   Please keep this ticket for inspection ", 10f, 360f, paint)
                canvas.drawText("========================================", 10f, 390f, boldPaint)

                pdfDocument.finishPage(page)
                destination?.fileDescriptor?.let { fd ->
                    pdfDocument.writeTo(FileOutputStream(fd))
                }
                pdfDocument.close()
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback?.onWriteFailed(e.localizedMessage)
            }
        }
    }

    printManager?.print("TransitGo Receipt #${booking.bookingId}", printAdapter, null)
}

@Composable
fun ConductorTicketingScreen() {
    val context = LocalContext.current
    var bookings by remember { mutableStateOf<List<BookingRecord>>(emptyList()) }
    var ticketsIssuedCount by remember { mutableStateOf(148) }
    var totalRevenue by remember { mutableStateOf(7400.0) }

    var printedTicketBooking by remember { mutableStateOf<BookingRecord?>(null) }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToBookings(
            onUpdate = { list ->
                bookings = list
            },
            onError = {}
        )
    }

    val cashBookings = bookings.filter { it.paymentMethod == "Cash" && it.paymentStatus != "Paid - Cash Collected" }
    val onlineBookings = bookings.filter { it.paymentMethod != "Cash" && it.paymentStatus != "Verified & Approved" }

    if (printedTicketBooking != null) {
        val b = printedTicketBooking!!
        AlertDialog(
            onDismissRequest = { printedTicketBooking = null },
            title = { Text("🚌 Official Thermal Receipt", fontWeight = FontWeight.Bold, color = ConductorGreen) },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB)),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("================================", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                        Text("        TRANSITGO BUS SYSTEM    ", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("     OFFICIAL PASSENGER TICKET  ", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                        Text("================================", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ticket ID : ${b.bookingId}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                        Text("Bus ID    : ${b.busId}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                        Text("Route     : ${b.routeName}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                        Text("Seat(s)   : ${b.seats}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Payment   : ${b.paymentMethod}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                        Text("TOTAL     : ₱%.2f".format(b.totalPrice), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ConductorGreen)
                        Text("================================", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                        Text("  THANK YOU FOR RIDING WITH US! ", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            printRealThermalReceipt(context, b)
                            Toast.makeText(context, "Sending to thermal printer / PDF...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print Thermal PDF")
                    }

                    Button(
                        onClick = { printedTicketBooking = null },
                        colors = ButtonDefaults.buttonColors(containerColor = ConductorGreen)
                    ) {
                        Text("Done")
                    }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Passenger Ticketing & Payment Verification", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Collection & Ticketing Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ConductorGreen)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total Tickets Issued: $ticketsIssuedCount", fontSize = 14.sp)
                Text("Total Revenue Collected: ₱%.2f".format(totalRevenue), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ConductorGreen)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Online Payments (GCash / PayMaya)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ConductorGreen)
        Spacer(modifier = Modifier.height(8.dp))

        if (onlineBookings.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Text("No online payments pending verification.", modifier = Modifier.padding(16.dp), color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            onlineBookings.forEach { booking ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bus: ${booking.busId} • Seats: ${booking.seats}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF2196F3)) {
                                Text(booking.paymentMethod, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Route: ${booking.routeName}", fontSize = 13.sp, color = Color.DarkGray)
                        Text("Amount: ₱%.2f".format(booking.totalPrice), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ConductorGreen)
                        if (booking.receiptUrl.isNotBlank()) {
                            Text("Receipt Attached: Yes (Verified by Passenger)", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                DatabaseManager.updateBookingPaymentStatus(
                                    bookingId = booking.bookingId,
                                    newPaymentStatus = "Verified & Approved",
                                    onSuccess = {
                                        ticketsIssuedCount += booking.seats.split(",").size
                                        totalRevenue += booking.totalPrice
                                        printedTicketBooking = booking
                                        Toast.makeText(context, "Online payment verified & ticket generated!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ConductorGreen),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify Receipt & Generate Ticket", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Cash Payments (Pay to Conductor)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ConductorGreen)
        Spacer(modifier = Modifier.height(8.dp))

        if (cashBookings.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Text("No cash bookings pending collection.", modifier = Modifier.padding(16.dp), color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            cashBookings.forEach { booking ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bus: ${booking.busId} • Seats: ${booking.seats}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF4CAF50)) {
                                Text("Cash", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Route: ${booking.routeName}", fontSize = 13.sp, color = Color.DarkGray)
                        Text("Amount Due: ₱%.2f".format(booking.totalPrice), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ConductorGreen)

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                DatabaseManager.updateBookingPaymentStatus(
                                    bookingId = booking.bookingId,
                                    newPaymentStatus = "Paid - Cash Collected",
                                    onSuccess = {
                                        ticketsIssuedCount += booking.seats.split(",").size
                                        totalRevenue += booking.totalPrice
                                        printedTicketBooking = booking
                                        Toast.makeText(context, "Cash collected & Ticket Printed!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ConductorGreen),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Collect Cash & Print Ticket", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

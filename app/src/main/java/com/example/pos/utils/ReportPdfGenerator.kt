package com.example.pos.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.pos.data.ReportFinancialSummary
import com.example.pos.data.Shop
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportPdfGenerator {

    // Standard A4 dimensions in points (72 DPI)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    fun generatePdf(
        context: Context,
        shop: Shop,
        summary: ReportFinancialSummary
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var currentY = MARGIN

        // ==========================================
        // 1. TOP HEADER / LETTERHEAD
        // ==========================================
        // Top accent line
        paint.color = Color.parseColor("#10B981") // Emerald accent
        paint.style = Paint.Style.FILL
        canvas.drawRect(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY + 4f, paint)
        currentY += 16f

        // Shop Name
        paint.color = Color.parseColor("#0F172A") // Deep slate
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        val shopDisplayName = shop.name.ifBlank { "RetailSync Store" }
        canvas.drawText(shopDisplayName, MARGIN, currentY + 14f, paint)

        // Document Title on the right
        paint.textSize = 13f
        paint.color = Color.parseColor("#059669")
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("FINANCIAL STATEMENT", MARGIN + CONTENT_WIDTH, currentY + 12f, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 20f

        // Shop Metadata (Left) & Report Reference (Right)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = Color.parseColor("#475569")

        val ownerText = if (shop.ownerName.isNotBlank()) "Owner: ${shop.ownerName}  •  " else ""
        val phoneText = if (shop.phone.isNotBlank()) "Tel: ${shop.phone}  •  " else ""
        val emailText = if (shop.email.isNotBlank()) "Email: ${shop.email}" else ""
        canvas.drawText("$ownerText$phoneText$emailText", MARGIN, currentY + 8f, paint)

        // Right side info: Reference ID & Timestamp
        val refId = "REF: RPT-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}-${(1000..9999).random()}"
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(refId, MARGIN + CONTENT_WIDTH, currentY + 8f, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 13f

        val addressText = if (shop.address.isNotBlank()) "Address: ${shop.address}" else "Nairobi, Kenya"
        canvas.drawText(addressText, MARGIN, currentY + 8f, paint)

        val generatedDate = "Generated: ${SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}"
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(generatedDate, MARGIN + CONTENT_WIDTH, currentY + 8f, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 18f

        // Reporting Period Banner Box
        paint.color = Color.parseColor("#F1F5F9")
        val bannerRect = RectF(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY + 24f)
        canvas.drawRoundRect(bannerRect, 4f, 4f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        val periodText = "REPORTING PERIOD: ${summary.scope.displayName.uppercase()}  |  ${summary.dateRangeText}"
        canvas.drawText(periodText, MARGIN + 10f, currentY + 16f, paint)
        currentY += 34f

        // ==========================================
        // 2. EXECUTIVE KPI MATRIX (4 Boxes)
        // ==========================================
        val boxWidth = (CONTENT_WIDTH - 24f) / 4f
        val boxHeight = 44f

        val kpiList = listOf(
            Triple("TOTAL REVENUE", "KSh ${String.format("%,.0f", summary.revenue)}", "#059669"),
            Triple("COST OF GOODS", "KSh ${String.format("%,.0f", summary.cogs)}", "#475569"),
            Triple("TOTAL EXPENSES", "KSh ${String.format("%,.0f", summary.totalExpenses)}", "#DC2626"),
            Triple("NET PROFIT", "KSh ${String.format("%,.0f", summary.netProfit)} (${String.format("%.1f", summary.netProfitMargin)}%)", if (summary.netProfit >= 0) "#059669" else "#DC2626")
        )

        for (i in kpiList.indices) {
            val (title, value, colorHex) = kpiList[i]
            val boxX = MARGIN + (i * (boxWidth + 8f))
            val kpiRect = RectF(boxX, currentY, boxX + boxWidth, currentY + boxHeight)

            // Fill & Border
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(kpiRect, 6f, 6f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.8f
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawRoundRect(kpiRect, 6f, 6f, paint)

            // Title
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 7f
            paint.color = Color.parseColor("#64748B")
            canvas.drawText(title, boxX + 8f, currentY + 14f, paint)

            // Value
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor(colorHex)
            canvas.drawText(value, boxX + 8f, currentY + 30f, paint)
        }
        currentY += boxHeight + 16f

        // Secondary KPI Note
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        paint.color = Color.parseColor("#475569")
        val grossMarginText = "Gross Profit: KSh ${String.format("%,.0f", summary.grossProfit)} (${String.format("%.1f", summary.grossProfitMargin)}% margin)  •  Total Transactions: ${summary.transactionCount}  •  Avg. Basket Value: KSh ${String.format("%,.0f", summary.averageBasketSize)}"
        canvas.drawText(grossMarginText, MARGIN + 2f, currentY + 2f, paint)
        currentY += 16f

        // ==========================================
        // 3. REVENUE BREAKDOWN & PAYMENT METHODS
        // ==========================================
        drawSectionHeading(canvas, paint, "1. SALES & CASH FLOW BREAKDOWN", currentY)
        currentY += 14f

        val tableHeaderY = currentY
        drawTableHeader(canvas, paint, tableHeaderY, listOf("Payment Method", "Transactions", "Amount (KSh)", "Share of Sales"))
        currentY += 16f

        val totalRev = if (summary.revenue > 0) summary.revenue else 1.0
        val paymentChannels = listOf("Cash", "M-Pesa", "Credit")
        for (idx in paymentChannels.indices) {
            val channel = paymentChannels[idx]
            val amt = summary.paymentBreakdown[channel] ?: 0.0
            val count = summary.paymentCounts[channel] ?: 0
            val share = (amt / totalRev) * 100.0

            drawTableRow(
                canvas, paint, currentY, idx % 2 == 0,
                listOf(
                    channel,
                    "$count orders",
                    String.format("%,.0f", amt),
                    String.format("%.1f%%", share)
                )
            )
            currentY += 14f
        }
        // Total Row
        drawTableTotalRow(
            canvas, paint, currentY,
            listOf("Total Gross Sales", "${summary.transactionCount} orders", String.format("%,.0f", summary.revenue), "100.0%")
        )
        currentY += 22f

        // ==========================================
        // 4. OPERATING EXPENSES & DEBTORS (TWO COLUMN)
        // ==========================================
        val colWidth = (CONTENT_WIDTH - 16f) / 2f

        // Column 1: Operating Expenses
        val col1X = MARGIN
        drawSectionHeading(canvas, paint, "2. OPERATING EXPENSES", currentY, col1X)

        // Column 2: Credit & Receivables
        val col2X = MARGIN + colWidth + 16f
        drawSectionHeading(canvas, paint, "3. CREDIT & RECEIVABLES", currentY, col2X)
        currentY += 14f

        val subTableY = currentY
        // Expenses Table Header
        drawSubTableHeader(canvas, paint, subTableY, col1X, colWidth, listOf("Category", "Amount (KSh)"))
        // Credit Table Header
        drawSubTableHeader(canvas, paint, subTableY, col2X, colWidth, listOf("Metric", "Amount (KSh)"))
        currentY += 15f

        val topExpenses = summary.expensesByCategory.entries.toList().take(4)
        val creditRows = listOf(
            "New Credit Issued" to summary.creditSalesAmount,
            "Initial Repayments Collected" to summary.creditCollections,
            "Outstanding Debtors Balance" to summary.creditOutstandingBalance
        )

        val maxRows = maxOf(topExpenses.size.coerceAtLeast(1), creditRows.size)
        for (r in 0 until maxRows) {
            val isEven = r % 2 == 0
            // Col 1 Row
            if (r < topExpenses.size) {
                val entry = topExpenses[r]
                drawSubTableRow(canvas, paint, currentY, col1X, colWidth, isEven, listOf(entry.key, String.format("%,.0f", entry.value)))
            } else if (r == 0 && topExpenses.isEmpty()) {
                drawSubTableRow(canvas, paint, currentY, col1X, colWidth, isEven, listOf("No expenses recorded", "0"))
            }

            // Col 2 Row
            if (r < creditRows.size) {
                val (label, value) = creditRows[r]
                drawSubTableRow(canvas, paint, currentY, col2X, colWidth, isEven, listOf(label, String.format("%,.0f", value)))
            }
            currentY += 13.5f
        }
        // Total Expenses row
        drawSubTableTotalRow(canvas, paint, currentY, col1X, colWidth, listOf("Total Expenses", String.format("%,.0f", summary.totalExpenses)))
        currentY += 22f

        // ==========================================
        // 5. TOP PERFORMING PRODUCTS
        // ==========================================
        drawSectionHeading(canvas, paint, "4. TOP SELLING INVENTORY ITEMS", currentY)
        currentY += 14f

        drawTableHeader(canvas, paint, currentY, listOf("#", "Product Item", "Category", "Quantity Sold", "Sales Total (KSh)"))
        currentY += 16f

        if (summary.topSellingProducts.isEmpty()) {
            drawTableRow(canvas, paint, currentY, true, listOf("-", "No product sales in this reporting window", "-", "0", "0"))
            currentY += 14f
        } else {
            val itemsToDraw = summary.topSellingProducts.take(6)
            for (i in itemsToDraw.indices) {
                val prod = itemsToDraw[i]
                drawTableRow(
                    canvas, paint, currentY, i % 2 == 0,
                    listOf(
                        "${i + 1}",
                        prod.name,
                        prod.category,
                        "${prod.unitsSold} units",
                        String.format("%,.0f", prod.revenue)
                    )
                )
                currentY += 13.5f
            }
        }
        currentY += 24f

        // ==========================================
        // 6. OFFICIAL AUDIT SIGN-OFF & STAMP
        // ==========================================
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 0.8f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY, paint)
        currentY += 22f

        val signColWidth = (CONTENT_WIDTH - 30f) / 2f
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = Color.parseColor("#475569")

        // Left Sign
        canvas.drawLine(MARGIN, currentY + 16f, MARGIN + 180f, currentY + 16f, paint)
        canvas.drawText("Prepared by: Store Cashier / Attendant", MARGIN, currentY + 28f, paint)

        // Right Sign
        canvas.drawLine(MARGIN + signColWidth + 30f, currentY + 16f, MARGIN + signColWidth + 210f, currentY + 16f, paint)
        canvas.drawText("Verified & Approved by: Store Owner / Management", MARGIN + signColWidth + 30f, currentY + 28f, paint)
        currentY += 46f

        // ==========================================
        // 7. FOOTER
        // ==========================================
        paint.textSize = 7.5f
        paint.color = Color.parseColor("#94A3B8")
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Official Financial Summary  •  Generated by RetailSync POS  •  Page 1 of 1", PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, paint)

        pdfDocument.finishPage(page)

        // Save to file
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "RetailSync_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    private fun drawSectionHeading(canvas: Canvas, paint: Paint, title: String, y: Float, x: Float = MARGIN) {
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        paint.color = Color.parseColor("#0F172A")
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(title, x, y, paint)
    }

    private fun drawTableHeader(canvas: Canvas, paint: Paint, y: Float, columns: List<String>) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F172A") // Deep slate header
        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 14f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f

        val colXPositions = when (columns.size) {
            4 -> listOf(MARGIN + 6f, MARGIN + 150f, MARGIN + 320f, MARGIN + 430f)
            5 -> listOf(MARGIN + 6f, MARGIN + 30f, MARGIN + 210f, MARGIN + 330f, MARGIN + 430f)
            else -> listOf(MARGIN + 6f, MARGIN + 200f)
        }

        for (i in columns.indices) {
            val posX = colXPositions.getOrElse(i) { MARGIN }
            canvas.drawText(columns[i], posX, y + 10f, paint)
        }
    }

    private fun drawTableRow(canvas: Canvas, paint: Paint, y: Float, isEven: Boolean, values: List<String>) {
        if (isEven) {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 13.5f, paint)
        }

        paint.color = Color.parseColor("#1E293B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f

        val colXPositions = when (values.size) {
            4 -> listOf(MARGIN + 6f, MARGIN + 150f, MARGIN + 320f, MARGIN + 430f)
            5 -> listOf(MARGIN + 6f, MARGIN + 30f, MARGIN + 210f, MARGIN + 330f, MARGIN + 430f)
            else -> listOf(MARGIN + 6f, MARGIN + 200f)
        }

        for (i in values.indices) {
            val posX = colXPositions.getOrElse(i) { MARGIN }
            canvas.drawText(values[i], posX, y + 9.5f, paint)
        }
    }

    private fun drawTableTotalRow(canvas: Canvas, paint: Paint, y: Float, values: List<String>) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 14f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f

        val colXPositions = listOf(MARGIN + 6f, MARGIN + 150f, MARGIN + 320f, MARGIN + 430f)
        for (i in values.indices) {
            val posX = colXPositions.getOrElse(i) { MARGIN }
            canvas.drawText(values[i], posX, y + 10f, paint)
        }
    }

    private fun drawSubTableHeader(canvas: Canvas, paint: Paint, y: Float, startX: Float, width: Float, columns: List<String>) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1E293B")
        canvas.drawRect(startX, y, startX + width, y + 13.5f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f

        canvas.drawText(columns[0], startX + 6f, y + 9.5f, paint)
        canvas.drawText(columns[1], startX + width - 70f, y + 9.5f, paint)
    }

    private fun drawSubTableRow(canvas: Canvas, paint: Paint, y: Float, startX: Float, width: Float, isEven: Boolean, values: List<String>) {
        if (isEven) {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRect(startX, y, startX + width, y + 13.5f, paint)
        }

        paint.color = Color.parseColor("#1E293B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f

        canvas.drawText(values[0], startX + 6f, y + 9.5f, paint)
        canvas.drawText(values[1], startX + width - 70f, y + 9.5f, paint)
    }

    private fun drawSubTableTotalRow(canvas: Canvas, paint: Paint, y: Float, startX: Float, width: Float, values: List<String>) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(startX, y, startX + width, y + 13.5f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f

        canvas.drawText(values[0], startX + 6f, y + 9.5f, paint)
        canvas.drawText(values[1], startX + width - 70f, y + 9.5f, paint)
    }
}

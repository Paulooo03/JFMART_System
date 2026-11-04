package com.jfmart.JFMART.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/** Draws [content] as a black-on-white QR code, so it scans reliably in both light and dark themes. */
fun qrBitmap(content: String, size: Int = 768): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size) { index ->
        if (matrix.get(index % size, index / size)) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

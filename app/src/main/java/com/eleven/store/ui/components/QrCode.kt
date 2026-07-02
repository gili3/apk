package com.eleven.store.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * توليد Bitmap لرمز QR من نص معيّن — يطابق وظيفياً QRCodeSVG المستخدم في الموقع.
 */
private fun generateQrBitmap(content: String, sizePx: Int): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}

/**
 * بطاقة QR بنفس شكل الموقع: خلفية بيضاء + حواف مدورة + حدّ خفيف + ظل بسيط (إطار محاكاة).
 */
@Composable
fun InvoiceQrCode(
    value: String,
    sizeDp: androidx.compose.ui.unit.Dp = 110.dp,
) {
    val bitmap = remember(value) { generateQrBitmap(value, 300) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "رمز التحقق من الطلب",
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, com.eleven.store.ui.theme.Border, RoundedCornerShape(8.dp))
            .padding(14.dp)
            .size(sizeDp)
    )
}

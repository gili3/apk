package com.eleven.store.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

// ═══════════════════════════════════════════════════════════════
//  ELEVEN STORE — FONT (يقابله في الموقع: index.html
//  <link href="https://fonts.googleapis.com/css2?family=Inter:...">
//  و index.css: font-family: 'Inter', ...)
//
//  نفس خط الموقع (Inter) يُحمَّل هنا عبر Google Fonts Provider،
//  بنفس الأوزان المستخدمة بالموقع: 400 / 500 / 600 / 700.
//
//  ملاحظة: Font()/GoogleFont.Provider هنا من حزمة
//  androidx.compose.ui.text.googlefonts وليس androidx.compose.ui.text.font
//  (خلط الحزمتين هو ما كان يسبب فشل الترجمة "Unresolved reference: FontProvider").
// ═══════════════════════════════════════════════════════════════

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.eleven.store.R.array.com_google_android_gms_fonts_certs
)

private val interGoogleFont = GoogleFont("Inter")

val InterFontFamily = FontFamily(
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
)

// خط العناوين/الشعار — يقابل .eleven-title / .eleven-brand في index.css
// (font-family: 'Georgia', serif) — أقرب عائلة نظام متاحة بلا تحميل شبكة.
val ElevenSerifFontFamily = FontFamily.Serif

package com.eleven.store.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * أيقونات Lucide (نفس مكتبة الأيقونات المستخدمة في الموقع: lucide-react)
 * منسوخة كمسارات SVG مطابقة، بدلاً من الاعتماد على Material Icons التي
 * لها سماكة خط وتناسب مختلف تماماً — وهذا سبب "عدم توحيد" شكل الأيقونة
 * بين التطبيق والموقع رغم تشابه المعنى (Home مقابل Home مثلاً).
 *
 * كل الأيقونات: viewBox 24x24، stroke فقط (بلا تعبئة)، سماكة خط 2،
 * أطراف وزوايا مدوّرة — تماماً كإعدادات lucide الافتراضية.
 *
 * ⚠️ تنبيه: نسخت مسارات SVG هذه من ذاكرتي لأيقونات lucide المعروفة
 * (home / shopping-bag / heart / shopping-cart / user / menu / search / x /
 * bell / log-out / bar-chart-3 / phone / info)، وليس لدي وصول لشبكة
 * الإنترنت هنا للتحقق منها مقابل مصدر lucide الرسمي حرفياً.
 * ينصح بمراجعة الشكل بصرياً بعد البناء، ومقارنته بالموقع. إن كان هناك
 * أي فرق بسيط، أرسل لي لقطة شاشة بعد البناء وسأعدّل الإحداثيات.
 */
private fun ImageVector.Builder.strokePath(pathData: String) = addPath(
    pathData = addPathNodes(pathData),
    fill = null,
    stroke = SolidColor(Color.Black), // يُستبدل باللون الفعلي عبر tint في Icon()
    strokeLineWidth = 2f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
)

object LucideIcons {

    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideHome",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            strokePath("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8")
            strokePath("M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")
        }.build()
    }

    val ShoppingBag: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideShoppingBag",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            strokePath("M16 10a4 4 0 0 1-8 0")
            strokePath("M3.103 6.034h17.794")
            strokePath("M3.4 5.467a2 2 0 0 0-.4 1.2V20a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6.667a2 2 0 0 0-.4-1.2l-2-2.667A2 2 0 0 0 17 2H7a2 2 0 0 0-1.6.8z")
        }.build()
    }

    val Heart: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideHeart",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            strokePath("M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5")
        }.build()
    }

    val ShoppingCart: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideShoppingCart",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            strokePath("M8 21a1 1 0 1 0 0-2 1 1 0 0 0 0 2z")
            strokePath("M19 21a1 1 0 1 0 0-2 1 1 0 0 0 0 2z")
            strokePath("M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12")
        }.build()
    }

    val User: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideUser",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            strokePath("M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2")
            strokePath("M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z")
        }.build()
    }

    val Menu: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideMenu",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            strokePath("M4 12h16")
            strokePath("M4 18h16")
            strokePath("M4 6h16")
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideSearch",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            strokePath("M21 21l-4.3-4.3")
            strokePath("M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z")
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideX",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            strokePath("M18 6 6 18")
            strokePath("M6 6l12 12")
        }.build()
    }

    val Bell: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideBell",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            strokePath("M10.268 21a2 2 0 0 0 3.464 0")
            strokePath("M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326")
        }.build()
    }

    val LogOut: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideLogOut",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            strokePath("M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4")
            strokePath("M16 17l5-5-5-5")
            strokePath("M21 12H9")
        }.build()
    }

    val BarChart3: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideBarChart3",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            strokePath("M3 3v18h18")
            strokePath("M18 17V9")
            strokePath("M13 17V5")
            strokePath("M8 17v-3")
        }.build()
    }

    val Phone: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucidePhone",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            strokePath("M13.832 16.568a1 1 0 0 0 1.213-.303l.355-.465A2 2 0 0 1 17 15h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2A18 18 0 0 1 2 4a2 2 0 0 1 2-2h3a2 2 0 0 1 2 2v3a2 2 0 0 1-.8 1.6l-.468.351a1 1 0 0 0-.292 1.233 14 14 0 0 0 6.392 6.384")
        }.build()
    }

    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideInfo",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            strokePath("M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z")
            strokePath("M12 16v-4")
            strokePath("M12 8h.01")
        }.build()
    }
}

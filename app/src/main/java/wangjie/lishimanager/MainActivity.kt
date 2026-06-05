@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package wangjie.lishimanager

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 0
private const val PAGE_SUMMARY = 1
private const val SCREEN_MAIN = 0
private const val SCREEN_SHOP = 1
private const val SCREEN_ITEMS = 2

private val CountPageColor = Color(0xFFF2F2F7)
private val ReceiptPageColor = Color(0xFFEEEFF5)
private val ReceiptCardColor = Color.White
private val ReceiptRowAltColor = Color(0xFFF1F3F4)
private val ReceiptTextColor = Color(0xFF202124)
private val ReceiptSecondaryColor = Color(0xFF5F6368)
private val ReceiptAccentColor = Color(0xFF1A73E8)
private val ReceiptErrorColor = Color(0xFFB3261E)
private val ReceiptPositiveColor = Color(0xFF0B8043)

private class Item(
    val id: Long,
    initialName: String,
    initialQuantity: Int,
    initialSortIndex: Int
) {
    var name by mutableStateOf(initialName)
    var quantity by mutableIntStateOf(initialQuantity)
    var sortIndex by mutableIntStateOf(initialSortIndex)
}

private data class LiShiUiState(
    val screen: Int,
    val page: Int,
    val sortMode: Boolean,
    val items: List<Item>,
    val selectedItemIds: List<Long>,
    val summaryTitle: String,
    val dateText: String,
    val shopName: String,
    val shopContact: String,
    val shopPhone: String,
    val shopAddress: String,
    val activeDialog: LiShiDialog?
) {
    val selectedItems: List<Item>
        get() = items.filter { it.quantity > 0 }

    val hasContact: Boolean
        get() = shopName.isNotEmpty() || shopContact.isNotEmpty() || shopPhone.isNotEmpty() || shopAddress.isNotEmpty()
}

private sealed interface LiShiDialog {
    data object AddItem : LiShiDialog
    data object BatchImport : LiShiDialog
    data object BatchDelete : LiShiDialog
    data object ResetAll : LiShiDialog
    data object RenameTitle : LiShiDialog
    data class Message(val title: String, val message: String) : LiShiDialog
    data class CopyResult(val copiedText: String) : LiShiDialog
    data class RenameItem(val itemId: Long) : LiShiDialog
    data class DeleteItem(val itemId: Long) : LiShiDialog
}

private class LiShiActions(
    val navigateBack: () -> Unit = {},
    val selectPage: (Int) -> Unit = {},
    val openItems: () -> Unit = {},
    val openShop: () -> Unit = {},
    val showDialog: (LiShiDialog) -> Unit = {},
    val dismissDialog: () -> Unit = {},
    val copySummary: () -> Unit = {},
    val saveReceipt: () -> Unit = {},
    val startSort: () -> Unit = {},
    val finishSort: () -> Unit = {},
    val toggleSelectAll: () -> Unit = {},
    val toggleItemSelection: (Long) -> Unit = {},
    val moveItem: (Long, Int) -> Unit = { _, _ -> },
    val incrementQuantity: (Long) -> Unit = {},
    val decrementQuantity: (Long) -> Unit = {},
    val setQuantity: (Long, Int) -> Unit = { _, _ -> },
    val updateShopName: (String) -> Unit = {},
    val updateShopContact: (String) -> Unit = {},
    val updateShopPhone: (String) -> Unit = {},
    val updateShopAddress: (String) -> Unit = {},
    val saveShop: () -> Unit = {},
    val confirmAddItem: (String) -> Unit = {},
    val confirmRenameItem: (Long, String) -> Unit = { _, _ -> },
    val confirmDeleteItem: (Long) -> Unit = {},
    val confirmBatchImport: (String) -> Unit = {},
    val confirmBatchDelete: () -> Unit = {},
    val confirmResetAll: () -> Unit = {},
    val confirmRenameTitle: (String) -> Unit = {}
)

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_WRITE_IMAGES = 301

        private val COLOR_PAGE = AndroidColor.rgb(255, 251, 254)
        private val COLOR_SURFACE = AndroidColor.rgb(255, 251, 254)
        private val COLOR_ACCENT = AndroidColor.rgb(103, 80, 164)

        private val COLOR_RECEIPT_PAGE = AndroidColor.rgb(238, 239, 245)
        private val COLOR_RECEIPT_CARD = AndroidColor.WHITE
        private val COLOR_RECEIPT_ROW_ALT = AndroidColor.rgb(241, 243, 244)
        private val COLOR_RECEIPT_TEXT = AndroidColor.rgb(32, 33, 36)
        private val COLOR_RECEIPT_SECONDARY = AndroidColor.rgb(95, 99, 104)
        private val COLOR_RECEIPT_ACCENT = AndroidColor.rgb(26, 115, 232)
        private val COLOR_RECEIPT_DESTRUCTIVE = AndroidColor.rgb(179, 38, 30)
        private val COLOR_RECEIPT_POSITIVE = AndroidColor.rgb(11, 128, 67)
    }

    private val items = mutableStateListOf<Item>()
    private val selectedItemIds = mutableStateListOf<Long>()
    private lateinit var prefs: android.content.SharedPreferences

    private var currentScreen by mutableIntStateOf(SCREEN_MAIN)
    private var currentPage by mutableIntStateOf(PAGE_COUNT)
    private var sortMode by mutableStateOf(false)
    private var summaryTitle by mutableStateOf("物品清单")
    private var generatedAtMillis by mutableLongStateOf(System.currentTimeMillis())
    private var pendingSaveBitmap: Bitmap? = null
    private var activeDialog: LiShiDialog? by mutableStateOf(null)

    private var shopName by mutableStateOf("")
    private var shopContact by mutableStateOf("")
    private var shopPhone by mutableStateOf("")
    private var shopAddress by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("lishi_manager", MODE_PRIVATE)
        loadAll()
        setSystemBars()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })
        setContent {
            LiShiManagerApp(
                state = uiState(),
                actions = actions()
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBackNavigation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_IMAGES) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            val bitmap = pendingSaveBitmap
            if (granted && bitmap != null) {
                saveBitmapToAlbum(bitmap)
            } else {
                activeDialog = LiShiDialog.Message(
                    title = "无法保存图片",
                    message = "保存图片需要相册权限。"
                )
            }
            pendingSaveBitmap = null
        }
    }

    private fun uiState(): LiShiUiState {
        return LiShiUiState(
            screen = currentScreen,
            page = currentPage,
            sortMode = sortMode,
            items = items,
            selectedItemIds = selectedItemIds.toList(),
            summaryTitle = summaryTitle,
            dateText = dateText(),
            shopName = shopName,
            shopContact = shopContact,
            shopPhone = shopPhone,
            shopAddress = shopAddress,
            activeDialog = activeDialog
        )
    }

    private fun actions(): LiShiActions {
        return LiShiActions(
            navigateBack = { handleBackNavigation() },
            selectPage = { switchMainPage(it) },
            openItems = {
                currentScreen = SCREEN_ITEMS
                sortMode = false
                activeDialog = null
            },
            openShop = {
                currentScreen = SCREEN_SHOP
                activeDialog = null
            },
            showDialog = { activeDialog = it },
            dismissDialog = { activeDialog = null },
            copySummary = { copySummaryText() },
            saveReceipt = { requestSaveBitmap(renderReceiptBitmap()) },
            startSort = {
                if (items.isNotEmpty()) {
                    sortMode = true
                    selectedItemIds.clear()
                }
            },
            finishSort = { finishSortMode() },
            toggleSelectAll = { toggleSelectAllItems() },
            toggleItemSelection = { itemId -> findItem(itemId)?.let { toggleItemSelection(it) } },
            moveItem = { itemId, direction -> moveItem(itemId, direction) },
            incrementQuantity = { itemId ->
                findItem(itemId)?.let {
                    it.quantity += 1
                    haptic()
                    saveItems()
                }
            },
            decrementQuantity = { itemId ->
                findItem(itemId)?.let {
                    if (it.quantity > 0) {
                        it.quantity -= 1
                        haptic()
                        saveItems()
                    }
                }
            },
            setQuantity = { itemId, quantity ->
                findItem(itemId)?.let {
                    val newValue = maxOf(0, quantity)
                    if (it.quantity != newValue) {
                        it.quantity = newValue
                        saveItems()
                    }
                }
            },
            updateShopName = {
                shopName = it
                saveShopInfo(false)
            },
            updateShopContact = {
                shopContact = it
                saveShopInfo(false)
            },
            updateShopPhone = {
                shopPhone = it
                saveShopInfo(false)
            },
            updateShopAddress = {
                shopAddress = it
                saveShopInfo(false)
            },
            saveShop = {
                saveShopInfo()
                currentScreen = SCREEN_MAIN
                currentPage = PAGE_COUNT
            },
            confirmAddItem = { addItem(it) },
            confirmRenameItem = { itemId, name -> renameItem(itemId, name) },
            confirmDeleteItem = { itemId -> deleteItem(itemId) },
            confirmBatchImport = { input -> importBatch(input) },
            confirmBatchDelete = { batchDeleteSelectedItems() },
            confirmResetAll = { resetAllQuantities() },
            confirmRenameTitle = { value -> renameSummaryTitle(value) }
        )
    }

    private fun handleBackNavigation() {
        if (activeDialog != null) {
            activeDialog = null
            return
        }
        if (currentScreen == SCREEN_ITEMS && sortMode) {
            finishSortMode()
            return
        }
        if (currentScreen != SCREEN_MAIN) {
            currentScreen = SCREEN_MAIN
            currentPage = PAGE_COUNT
            sortMode = false
            selectedItemIds.clear()
            return
        }
        finish()
    }

    private fun setSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = COLOR_SURFACE
            window.navigationBarColor = COLOR_PAGE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            window.decorView.systemUiVisibility = flags
        }
    }

    private fun switchMainPage(page: Int) {
        if (currentPage == page) return
        currentPage = page
        if (page == PAGE_SUMMARY) {
            generatedAtMillis = System.currentTimeMillis()
        }
    }

    private fun addItem(input: String) {
        val name = input.trim()
        if (name.isEmpty()) return
        if (containsItemName(name, null)) {
            toast("「$name」已存在，请改个名字")
            return
        }
        items.forEach { it.sortIndex += 1 }
        items.add(Item(System.currentTimeMillis(), name, 0, 0))
        sortAndReindex()
        saveItems()
        activeDialog = null
    }

    private fun renameItem(itemId: Long, input: String) {
        val item = findItem(itemId) ?: return
        val name = input.trim()
        if (name.isEmpty()) {
            activeDialog = null
            return
        }
        if (containsItemName(name, item)) {
            toast("「$name」已存在，请改个名字")
            return
        }
        item.name = name
        saveItems()
        activeDialog = null
    }

    private fun deleteItem(itemId: Long) {
        val item = findItem(itemId) ?: return
        items.remove(item)
        selectedItemIds.remove(itemId)
        sortAndReindex()
        saveItems()
        activeDialog = null
    }

    private fun importBatch(input: String) {
        val result = parseBatchInput(input)
        if (result.newNames.isEmpty()) {
            toast("没有可导入的新物品")
            return
        }
        batchInsert(result.newNames)
        val skipped = result.alreadyExists.size + result.duplicatesInInput.size
        toast("导入 ${result.newNames.size} 个，跳过 $skipped 个")
        activeDialog = null
    }

    private fun resetAllQuantities() {
        if (items.none { it.quantity > 0 }) {
            toast("当前没有已登记数量")
            activeDialog = null
            return
        }
        items.forEach { it.quantity = 0 }
        saveItems()
        activeDialog = null
    }

    private fun renameSummaryTitle(input: String) {
        val value = input.trim()
        if (value.isNotEmpty()) {
            summaryTitle = value
        }
        activeDialog = null
    }

    private fun saveShopInfo(showToast: Boolean = true) {
        prefs.edit()
            .putString("shopName", shopName)
            .putString("shopContact", shopContact)
            .putString("shopPhone", shopPhone)
            .putString("shopAddress", shopAddress)
            .apply()
        if (showToast) toast("已保存")
    }

    private fun copySummaryText() {
        val text = summaryText()
        val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("物品清单", text))
        activeDialog = LiShiDialog.CopyResult(text)
    }

    private fun requestSaveBitmap(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingSaveBitmap = bitmap
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_IMAGES)
            return
        }
        saveBitmapToAlbum(bitmap)
    }

    private fun saveBitmapToAlbum(bitmap: Bitmap) {
        val name = "lishi_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".png"
        val resolver: ContentResolver = contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }

        var collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LiShiManager")
            values.put(MediaStore.Images.Media.IS_PENDING, 1)
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

        var uri: Uri? = null
        try {
            uri = resolver.insert(collection, values) ?: throw IOException("无法创建图片文件")
            resolver.openOutputStream(uri).use { stream: OutputStream? ->
                if (stream == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw IOException("图片写入失败")
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            }
            activeDialog = LiShiDialog.Message(
                title = "保存成功",
                message = "图片已保存到相册。"
            )
        } catch (error: Exception) {
            uri?.let { resolver.delete(it, null, null) }
            activeDialog = LiShiDialog.Message(
                title = "保存失败",
                message = error.message?.let { "保存失败：$it" } ?: "保存失败，请稍后再试。"
            )
        }
    }

    private fun renderReceiptBitmap(): Bitmap {
        val width = maxOf(dp(320), resources.displayMetrics.widthPixels - dp(32))
        val outer = dp(16)
        val edgeHeight = dp(10)
        val paperLeft = outer
        val paperRight = width - outer
        val paperWidth = paperRight - paperLeft
        val paddingHorizontal = dp(18)
        val paddingTop = dp(20)
        val paddingBottom = dp(20)
        val contentWidth = paperWidth - paddingHorizontal * 2
        val selected = selectedItems()

        val shopPaint = textPaint(28f, COLOR_RECEIPT_ACCENT, bold = true, align = Paint.Align.CENTER)
        val titlePaint = textPaint(25f, COLOR_RECEIPT_TEXT, bold = true, align = Paint.Align.CENTER)
        val datePaint = textPaint(16f, COLOR_RECEIPT_SECONDARY, align = Paint.Align.CENTER)
        val headerPaint = textPaint(18f, COLOR_RECEIPT_ACCENT, bold = true)
        val headerRightPaint = textPaint(18f, COLOR_RECEIPT_ACCENT, bold = true, align = Paint.Align.RIGHT)
        val itemNamePaint = textPaint(23f, COLOR_RECEIPT_TEXT)
        val itemNumberPaint = textPaint(14f, AndroidColor.WHITE, bold = true, align = Paint.Align.CENTER)
        val quantityPaint = textPaint(24f, COLOR_RECEIPT_TEXT, bold = true, align = Paint.Align.RIGHT)
        val totalPaint = textPaint(21f, COLOR_RECEIPT_TEXT, bold = true)
        val totalRightPaint = textPaint(21f, COLOR_RECEIPT_DESTRUCTIVE, bold = true, align = Paint.Align.RIGHT)
        val contactTitlePaint = textPaint(14f, COLOR_RECEIPT_ACCENT, align = Paint.Align.CENTER)
        val contactPaint = textPaint(15f, COLOR_RECEIPT_TEXT)
        val contactPhonePaint = textPaint(15f, COLOR_RECEIPT_POSITIVE, bold = true)
        val thanksPaint = textPaint(14f, COLOR_RECEIPT_DESTRUCTIVE, bold = true, align = Paint.Align.CENTER)
        val footerPaint = textPaint(10f, COLOR_RECEIPT_SECONDARY, align = Paint.Align.CENTER)
        val emptyPaint = textPaint(18f, COLOR_RECEIPT_SECONDARY, align = Paint.Align.CENTER)
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_RECEIPT_CARD }
        val rowAltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_RECEIPT_ROW_ALT }
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_RECEIPT_ACCENT }

        val itemNameWidth = contentWidth - dp(34) - dp(8) - dp(82)
        val itemLayouts = selected.map { staticLayout(it.name, itemNamePaint, itemNameWidth) }
        val contactLayouts = contactLines().map { (text, paint) -> staticLayout(text, paint, contentWidth) }
        val footerLayout = staticLayout("商务合作 · 软件开发 · 技术咨询\nwangjie7629@163.com", footerPaint, contentWidth, Layout.Alignment.ALIGN_CENTER)

        fun dividerHeight() = dp(14) + dp(1) + dp(12)
        fun thinDividerHeight() = dp(8) + dp(1) + dp(8)

        var contentHeight = paddingTop
        if (shopName.isNotEmpty()) contentHeight += dp(38)
        contentHeight += if (shopName.isEmpty()) dp(38) else dp(50)
        contentHeight += dp(32)
        contentHeight += dividerHeight()
        contentHeight += dp(30)
        contentHeight += thinDividerHeight()
        contentHeight += if (selected.isEmpty()) {
            dp(54)
        } else {
            itemLayouts.sumOf { maxOf(dp(46), it.height + dp(16)) }
        }
        contentHeight += dividerHeight()
        contentHeight += dp(36)
        if (hasContact()) {
            contentHeight += dividerHeight()
            contentHeight += dp(24)
            contactLayouts.forEach { layout -> contentHeight += dp(4) + maxOf(dp(22), layout.height) }
        }
        contentHeight += dividerHeight()
        contentHeight += dp(24)
        contentHeight += dp(10) + footerLayout.height
        contentHeight += paddingBottom

        val height = outer + edgeHeight + contentHeight + edgeHeight + outer
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(COLOR_RECEIPT_PAGE)

        var y = outer
        drawReceiptEdge(canvas, paperLeft, y, paperWidth, edgeHeight, top = true, whitePaint)
        y += edgeHeight
        canvas.drawRect(paperLeft.toFloat(), y.toFloat(), paperRight.toFloat(), (y + contentHeight).toFloat(), whitePaint)
        var contentY = y + paddingTop
        val centerX = width / 2f
        val contentLeft = paperLeft + paddingHorizontal
        val contentRight = paperRight - paddingHorizontal

        if (shopName.isNotEmpty()) {
            drawCenteredText(canvas, shopName, shopPaint, centerX, contentY, dp(38))
            contentY += dp(38)
        }
        if (shopName.isNotEmpty()) contentY += dp(12)
        drawCenteredText(canvas, "** $summaryTitle **", titlePaint, centerX, contentY, dp(38))
        contentY += dp(38)
        contentY += dp(8)
        drawCenteredText(canvas, dateText(), datePaint, centerX, contentY, dp(24))
        contentY += dp(24)
        contentY = drawDashedDivider(canvas, contentLeft, contentRight, contentY, COLOR_RECEIPT_ACCENT, topMargin = dp(14), bottomMargin = dp(12))

        drawTextInBox(canvas, "品名", headerPaint, contentLeft.toFloat(), contentY, dp(30))
        drawTextInBox(canvas, "数量", headerRightPaint, contentRight.toFloat(), contentY, dp(30))
        contentY += dp(30)
        contentY = drawDashedDivider(canvas, contentLeft, contentRight, contentY, COLOR_RECEIPT_SECONDARY, topMargin = dp(8), bottomMargin = dp(8))

        if (selected.isEmpty()) {
            drawCenteredText(canvas, "还没有登记数量", emptyPaint, centerX, contentY, dp(54))
            contentY += dp(54)
        } else {
            selected.forEachIndexed { index, item ->
                val layout = itemLayouts[index]
                val rowHeight = maxOf(dp(46), layout.height + dp(16))
                if (index % 2 == 0) {
                    canvas.drawRect(contentLeft.toFloat(), contentY.toFloat(), contentRight.toFloat(), (contentY + rowHeight).toFloat(), rowAltPaint)
                }
                val badgeTop = contentY + (rowHeight - dp(24)) / 2
                canvas.drawRoundRect(
                    contentLeft.toFloat(),
                    badgeTop.toFloat(),
                    (contentLeft + dp(34)).toFloat(),
                    (badgeTop + dp(24)).toFloat(),
                    dp(4).toFloat(),
                    dp(4).toFloat(),
                    badgePaint
                )
                drawCenteredText(canvas, String.format(Locale.CHINA, "%02d", index + 1), itemNumberPaint, contentLeft + dp(17f), badgeTop, dp(24))
                drawStaticLayout(canvas, layout, contentLeft + dp(42), contentY + (rowHeight - layout.height) / 2)
                drawTextInBox(canvas, "x${item.quantity}", quantityPaint, contentRight.toFloat(), contentY, rowHeight)
                contentY += rowHeight
            }
        }

        contentY = drawDashedDivider(canvas, contentLeft, contentRight, contentY, COLOR_RECEIPT_ACCENT, topMargin = dp(14), bottomMargin = dp(12))
        drawTextInBox(canvas, "合计", totalPaint, contentLeft.toFloat(), contentY, dp(36))
        drawTextInBox(canvas, "${selected.size} 种", totalRightPaint, contentRight.toFloat(), contentY, dp(36))
        contentY += dp(36)

        if (hasContact()) {
            contentY = drawDashedDivider(canvas, contentLeft, contentRight, contentY, COLOR_RECEIPT_SECONDARY, topMargin = dp(14), bottomMargin = dp(12))
            drawCenteredText(canvas, "— 联系方式 —", contactTitlePaint, centerX, contentY, dp(24))
            contentY += dp(24)
            contactLayouts.forEach { layout ->
                contentY += dp(4)
                drawStaticLayout(canvas, layout, contentLeft, contentY)
                contentY += maxOf(dp(22), layout.height)
            }
        }

        contentY = drawDashedDivider(canvas, contentLeft, contentRight, contentY, COLOR_RECEIPT_SECONDARY, topMargin = dp(14), bottomMargin = dp(12))
        drawCenteredText(canvas, "—— 谢谢惠顾 ——", thanksPaint, centerX, contentY, dp(24))
        contentY += dp(24)
        contentY += dp(10)
        drawStaticLayout(canvas, footerLayout, contentLeft, contentY)
        y += contentHeight
        drawReceiptEdge(canvas, paperLeft, y, paperWidth, edgeHeight, top = false, whitePaint)
        return bitmap
    }

    private fun contactLines(): List<Pair<String, TextPaint>> {
        val normal = textPaint(15f, COLOR_RECEIPT_TEXT)
        val phone = textPaint(15f, COLOR_RECEIPT_POSITIVE, bold = true)
        return buildList {
            if (shopContact.isNotEmpty()) add("姓名：$shopContact" to normal)
            if (shopPhone.isNotEmpty()) add("电话：$shopPhone" to phone)
            if (shopAddress.isNotEmpty()) add("地址：$shopAddress" to normal)
        }
    }

    private fun selectedItems() = items.filter { it.quantity > 0 }

    private fun hasContact() = shopName.isNotEmpty() || shopContact.isNotEmpty() || shopPhone.isNotEmpty() || shopAddress.isNotEmpty()

    private fun summaryText(): String {
        val builder = StringBuilder()
        builder.append(summaryTitle).append("（").append(dateText()).append("）\n\n")
        selectedItems().forEach { builder.append(it.name).append("  ").append(it.quantity).append("\n") }
        builder.append("\n共 ").append(selectedItems().size).append(" 种")
        if (hasContact()) {
            builder.append("\n\n—— 联系方式 ——")
            if (shopName.isNotEmpty()) builder.append("\n").append(shopName)
            if (shopContact.isNotEmpty()) builder.append("\n姓名：").append(shopContact)
            if (shopPhone.isNotEmpty()) builder.append("\n电话：").append(shopPhone)
            if (shopAddress.isNotEmpty()) builder.append("\n地址：").append(shopAddress)
        }
        return builder.toString()
    }

    private fun dateText(): String {
        return SimpleDateFormat("yyyy年M月d日 EEEE HH:mm", Locale.CHINA).format(generatedAtMillis)
    }

    private fun batchInsert(names: List<String>) {
        items.forEach { it.sortIndex += names.size }
        val now = System.currentTimeMillis()
        names.forEachIndexed { index, name -> items.add(Item(now + index, name, 0, index)) }
        sortAndReindex()
        saveItems()
    }

    private fun parseBatchInput(input: String): ParseResult {
        val result = ParseResult()
        val seen = linkedSetOf<String>()
        input.split(Regex("[,，\\n\\r]+")).forEach { raw ->
            val name = raw.trim()
            if (name.isEmpty()) return@forEach
            if (containsItemName(name, null)) {
                if (!result.alreadyExists.contains(name)) result.alreadyExists.add(name)
                return@forEach
            }
            if (seen.contains(name)) {
                if (!result.duplicatesInInput.contains(name)) result.duplicatesInInput.add(name)
                return@forEach
            }
            seen.add(name)
            result.newNames.add(name)
        }
        return result
    }

    private fun finishSortMode() {
        sortMode = false
        selectedItemIds.clear()
        saveItems()
    }

    private fun allItemsSelected(): Boolean {
        return items.isNotEmpty() && selectedItemIds.size == items.size
    }

    private fun toggleItemSelection(item: Item) {
        if (selectedItemIds.contains(item.id)) {
            selectedItemIds.remove(item.id)
        } else {
            selectedItemIds.add(item.id)
        }
        haptic()
    }

    private fun toggleSelectAllItems() {
        if (allItemsSelected()) {
            selectedItemIds.clear()
        } else {
            selectedItemIds.clear()
            selectedItemIds.addAll(items.map { it.id })
        }
        haptic()
    }

    private fun batchDeleteSelectedItems() {
        if (selectedItemIds.isEmpty()) return
        items.removeAll { selectedItemIds.contains(it.id) }
        selectedItemIds.clear()
        sortAndReindex()
        saveItems()
        if (items.isEmpty()) sortMode = false
        activeDialog = null
    }

    private fun moveItem(itemId: Long, direction: Int) {
        val from = items.indexOfFirst { it.id == itemId }
        if (from < 0) return
        val to = (from + direction).coerceIn(0, items.lastIndex)
        if (from == to) return
        val item = items.removeAt(from)
        items.add(to, item)
        reindexCurrentOrder()
        saveItems()
    }

    private fun containsItemName(name: String, except: Item?): Boolean {
        return items.any { it !== except && it.name == name }
    }

    private fun findItem(itemId: Long): Item? {
        return items.firstOrNull { it.id == itemId }
    }

    private fun loadAll() {
        shopName = prefs.getString("shopName", "").orEmpty()
        shopContact = prefs.getString("shopContact", "").orEmpty()
        shopPhone = prefs.getString("shopPhone", "").orEmpty()
        shopAddress = prefs.getString("shopAddress", "").orEmpty()
        items.clear()
        val raw = prefs.getString("items", "[]").orEmpty()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                items.add(Item(
                    obj.optLong("id", System.currentTimeMillis() + i),
                    obj.optString("name", ""),
                    obj.optInt("quantity", 0),
                    obj.optInt("sortIndex", i)
                ))
            }
        } catch (_: JSONException) {
            items.clear()
        }
        items.removeAll { it.name.trim().isEmpty() }
        sortAndReindex()
    }

    private fun saveItems() {
        reindexCurrentOrder()
        val array = JSONArray()
        items.forEach {
            val obj = JSONObject()
            try {
                obj.put("id", it.id)
                obj.put("name", it.name)
                obj.put("quantity", maxOf(0, it.quantity))
                obj.put("sortIndex", it.sortIndex)
                array.put(obj)
            } catch (_: JSONException) {
            }
        }
        prefs.edit().putString("items", array.toString()).apply()
    }

    private fun sortAndReindex() {
        val sorted = items.sortedBy { it.sortIndex }
        items.clear()
        items.addAll(sorted)
        reindexCurrentOrder()
    }

    private fun reindexCurrentOrder() {
        items.forEachIndexed { index, item -> item.sortIndex = index }
    }

    private fun haptic() {
        window.decorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun textPaint(sp: Float, color: Int, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = sp(sp)
            textAlign = align
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun staticLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(alignment)
            .setIncludePad(false)
            .build()
    }

    private fun drawStaticLayout(canvas: AndroidCanvas, layout: StaticLayout, x: Int, y: Int) {
        canvas.save()
        canvas.translate(x.toFloat(), y.toFloat())
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawCenteredText(canvas: AndroidCanvas, text: String, paint: Paint, centerX: Float, y: Int, height: Int) {
        val metrics = paint.fontMetrics
        val baseline = y + (height - metrics.bottom + metrics.top) / 2f - metrics.top
        canvas.drawText(text, centerX, baseline, paint)
    }

    private fun drawTextInBox(canvas: AndroidCanvas, text: String, paint: Paint, x: Float, y: Int, height: Int) {
        val metrics = paint.fontMetrics
        val baseline = y + (height - metrics.bottom + metrics.top) / 2f - metrics.top
        canvas.drawText(text, x, baseline, paint)
    }

    private fun drawDashedDivider(
        canvas: AndroidCanvas,
        left: Int,
        right: Int,
        y: Int,
        color: Int,
        topMargin: Int,
        bottomMargin: Int
    ): Int {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 1.5f
            alpha = 150
        }
        val lineY = y + topMargin
        var x = left.toFloat()
        while (x < right) {
            canvas.drawLine(x, lineY.toFloat(), minOf(x + dp(8), right.toFloat()), lineY.toFloat(), paint)
            x += dp(14)
        }
        return lineY + dp(1) + bottomMargin
    }

    private fun drawReceiptEdge(canvas: AndroidCanvas, left: Int, y: Int, width: Int, height: Int, top: Boolean, paint: Paint) {
        val tooth = height * 2f
        val path = AndroidPath()
        var x = left - tooth / 2f
        while (x < left + width + tooth) {
            path.reset()
            if (top) {
                path.moveTo(x, (y + height).toFloat())
                path.lineTo(x + tooth / 2f, y.toFloat())
                path.lineTo(x + tooth, (y + height).toFloat())
            } else {
                path.moveTo(x, y.toFloat())
                path.lineTo(x + tooth / 2f, (y + height).toFloat())
                path.lineTo(x + tooth, y.toFloat())
            }
            path.close()
            canvas.drawPath(path, paint)
            x += tooth
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
    }

    private fun dp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
    }

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
    }
}

private class ParseResult {
    val newNames = mutableListOf<String>()
    val duplicatesInInput = mutableListOf<String>()
    val alreadyExists = mutableListOf<String>()
}

@Composable
private fun LiShiManagerApp(state: LiShiUiState, actions: LiShiActions) {
    LiShiManagerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = state.screen,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val duration = 280
                    val push = initialState == SCREEN_MAIN && targetState != SCREEN_MAIN
                    val pop = initialState != SCREEN_MAIN && targetState == SCREEN_MAIN
                    when {
                        push -> {
                            (slideInHorizontally(animationSpec = tween(duration)) { width -> width } +
                                fadeIn(animationSpec = tween(duration))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(duration)) { width -> -width / 3 } +
                                    fadeOut(animationSpec = tween(duration / 2)))
                        }
                        pop -> {
                            (slideInHorizontally(animationSpec = tween(duration)) { width -> -width / 3 } +
                                fadeIn(animationSpec = tween(duration))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(duration)) { width -> width } +
                                    fadeOut(animationSpec = tween(duration / 2)))
                        }
                        else -> {
                            fadeIn(animationSpec = tween(120)) togetherWith fadeOut(animationSpec = tween(120))
                        }
                    }.using(SizeTransform(clip = false))
                },
                label = "RootScreenTransition"
            ) { screen ->
                when (screen) {
                    SCREEN_SHOP -> ShopInfoScreen(state, actions)
                    SCREEN_ITEMS -> ItemsManageScreen(state, actions)
                    else -> MainScreen(state, actions)
                }
            }
        }
        LiShiDialogHost(state, actions)
    }
}

@Composable
private fun LiShiManagerTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context)
    } else {
        lightColorScheme(
            primary = Color(0xFF6750A4),
            secondary = Color(0xFF625B71),
            surface = Color(0xFFFFFBFE),
            background = Color(0xFFFFFBFE),
            error = Color(0xFFB3261E)
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
private fun MainScreen(state: LiShiUiState, actions: LiShiActions) {
    val pagerState = rememberPagerState(initialPage = state.page) { 2 }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.page) {
        if (pagerState.currentPage != state.page) {
            pagerState.animateScrollToPage(state.page)
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        if (state.page != pagerState.settledPage) {
            actions.selectPage(pagerState.settledPage)
        }
    }

    Scaffold(
        topBar = {
            MainTabStrip(
                selectedPage = pagerState.currentPage,
                state = state,
                actions = actions,
                onSelectPage = { page ->
                    scope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                }
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            if (page == PAGE_COUNT) {
                CountPage(state, actions, PaddingValues(0.dp))
            } else {
                SummaryPage(state, actions, PaddingValues(0.dp))
            }
        }
    }
}

@Composable
private fun MainTabStrip(
    selectedPage: Int,
    state: LiShiUiState,
    actions: LiShiActions,
    onSelectPage: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 20.dp, end = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainToolbarActions(
                page = selectedPage,
                state = state,
                actions = actions
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(start = 20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                MainTextTab(
                    text = "登记",
                    selected = selectedPage == PAGE_COUNT,
                    onClick = { onSelectPage(PAGE_COUNT) }
                )
                MainTextTab(
                    text = "清单",
                    selected = selectedPage == PAGE_SUMMARY,
                    onClick = { onSelectPage(PAGE_SUMMARY) }
                )
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun MainTextTab(text: String, selected: Boolean, onClick: () -> Unit) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .width(92.dp)
            .height(48.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = text,
            color = if (selected) selectedColor else unselectedColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(9.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .background(
                    color = if (selected) selectedColor else Color.Transparent,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                )
        )
    }
}

@Composable
private fun MainToolbarActions(page: Int, state: LiShiUiState, actions: LiShiActions) {
    if (page == PAGE_COUNT) {
        ToolbarIconButton(
            iconRes = R.drawable.ic_inventory_24,
            label = "物品管理",
            contentDescription = "物品管理",
            onClick = actions.openItems
        )
        ToolbarIconButton(
            iconRes = R.drawable.ic_store_24,
            label = "店铺信息",
            contentDescription = "店铺信息",
            onClick = actions.openShop
        )
        ToolbarIconButton(
            iconRes = R.drawable.ic_restart_alt_24,
            label = "重置",
            contentDescription = "重新开始",
            enabled = state.items.any { it.quantity > 0 },
            onClick = { actions.showDialog(LiShiDialog.ResetAll) }
        )
    } else {
        ToolbarIconButton(
            iconRes = R.drawable.ic_save_alt_24,
            label = "保存",
            contentDescription = "保存到相册",
            onClick = actions.saveReceipt
        )
        ToolbarIconButton(
            iconRes = R.drawable.ic_content_copy_24,
            label = "复制",
            contentDescription = "复制文本",
            onClick = actions.copySummary
        )
    }
}

@Composable
private fun ToolbarIconButton(
    iconRes: Int,
    label: String,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Column(
        modifier = Modifier.width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(21.dp),
                tint = color
            )
        }
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CountPage(state: LiShiUiState, actions: LiShiActions, padding: PaddingValues) {
    if (state.items.isEmpty()) {
        EmptyState(
            title = "还没有物品",
            message = "先添加物品，再回来登记数量。",
            actionText = "去物品管理",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CountPageColor),
            onAction = actions.openItems
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(CountPageColor),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(state.items, key = { _, item -> item.id }) { _, item ->
            CountItemRow(item, actions)
        }
    }
}

@Composable
private fun CountItemRow(item: Item, actions: LiShiActions) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = item.name,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 22.sp,
                fontWeight = if (item.quantity > 0) FontWeight.Bold else FontWeight.Normal
            )
            OutlinedIconButton(
                onClick = { actions.decrementQuantity(item.id) },
                enabled = item.quantity > 0,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_remove_24),
                    contentDescription = "减少数量"
                )
            }
            OutlinedTextField(
                value = item.quantity.toString(),
                onValueChange = { raw ->
                    actions.setQuantity(item.id, raw.filter { it.isDigit() }.toIntOrNull() ?: 0)
                },
                modifier = Modifier.width(76.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            FilledIconButton(
                onClick = { actions.incrementQuantity(item.id) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_24),
                    contentDescription = "增加数量"
                )
            }
        }
    }
}

@Composable
private fun SummaryPage(state: LiShiUiState, actions: LiShiActions, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(ReceiptPageColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ReceiptCard(
            state = state,
            exportMode = false,
            onRenameTitle = { actions.showDialog(LiShiDialog.RenameTitle) }
        )
        Text(
            text = "保存的图片或复制的清单，可直接发送到微信",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReceiptCard(
    state: LiShiUiState,
    exportMode: Boolean,
    onRenameTitle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        ReceiptEdge(top = true)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ReceiptCardColor)
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            if (state.shopName.isNotEmpty()) {
                Text(
                    text = state.shopName,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = ReceiptAccentColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (exportMode) Modifier else Modifier.clickable(onClick = onRenameTitle)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "** ${state.summaryTitle} **",
                    textAlign = TextAlign.Center,
                    color = ReceiptTextColor,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!exportMode) {
                    IconButton(onClick = onRenameTitle) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit_24),
                            contentDescription = "编辑标题",
                            tint = ReceiptAccentColor
                        )
                    }
                }
            }
            Text(
                text = state.dateText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center,
                color = ReceiptSecondaryColor,
                fontSize = 16.sp
            )
            DashedDivider(color = ReceiptAccentColor, modifier = Modifier.padding(vertical = 13.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "品名",
                    modifier = Modifier.weight(1f),
                    color = ReceiptAccentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "数量",
                    color = ReceiptAccentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            DashedDivider(color = ReceiptSecondaryColor, modifier = Modifier.padding(vertical = 8.dp))
            if (state.selectedItems.isEmpty()) {
                Text(
                    text = "还没有登记数量",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    textAlign = TextAlign.Center,
                    color = ReceiptSecondaryColor,
                    fontSize = 18.sp
                )
            } else {
                state.selectedItems.forEachIndexed { index, item ->
                    ReceiptRow(item = item, index = index)
                }
            }
            DashedDivider(color = ReceiptAccentColor, modifier = Modifier.padding(vertical = 13.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "合计",
                    modifier = Modifier.weight(1f),
                    color = ReceiptTextColor,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.selectedItems.size} 种",
                    color = ReceiptErrorColor,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (state.hasContact) {
                DashedDivider(color = ReceiptSecondaryColor, modifier = Modifier.padding(vertical = 13.dp))
                Text(
                    text = "— 联系方式 —",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = ReceiptAccentColor,
                    fontSize = 14.sp
                )
                if (state.shopContact.isNotEmpty()) ContactLine("姓名：${state.shopContact}")
                if (state.shopPhone.isNotEmpty()) ContactLine("电话：${state.shopPhone}", color = ReceiptPositiveColor, bold = true)
                if (state.shopAddress.isNotEmpty()) ContactLine("地址：${state.shopAddress}")
            }
            DashedDivider(color = ReceiptSecondaryColor, modifier = Modifier.padding(vertical = 13.dp))
            Text(
                text = "—— 谢谢惠顾 ——",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = ReceiptErrorColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "商务合作 · 软件开发 · 技术咨询\nwangjie7629@163.com",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                textAlign = TextAlign.Center,
                color = ReceiptSecondaryColor,
                fontSize = 10.sp
            )
        }
        ReceiptEdge(top = false)
    }
}

@Composable
private fun ReceiptRow(item: Item, index: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (index % 2 == 0) ReceiptRowAltColor else ReceiptCardColor)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 24.dp)
                .background(ReceiptAccentColor, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format(Locale.CHINA, "%02d", index + 1),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = item.name,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            color = ReceiptTextColor,
            fontSize = 23.sp
        )
        Text(
            text = "x${item.quantity}",
            modifier = Modifier.widthIn(min = 72.dp),
            textAlign = TextAlign.End,
            color = ReceiptTextColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ContactLine(text: String, color: Color = ReceiptTextColor, bold: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        color = color,
        fontSize = 15.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
private fun ReceiptEdge(top: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val tooth = size.height * 2f
        var x = -tooth / 2f
        while (x < size.width + tooth) {
            val path = Path().apply {
                if (top) {
                    moveTo(x, size.height)
                    lineTo(x + tooth / 2f, 0f)
                    lineTo(x + tooth, size.height)
                } else {
                    moveTo(x, 0f)
                    lineTo(x + tooth / 2f, size.height)
                    lineTo(x + tooth, 0f)
                }
                close()
            }
            drawPath(path, ReceiptCardColor)
            x += tooth
        }
    }
}

@Composable
private fun DashedDivider(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color.copy(alpha = 0.65f),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
    }
}

@Composable
private fun ShopInfoScreen(state: LiShiUiState, actions: LiShiActions) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("店铺信息") },
                navigationIcon = { BackButton(actions.navigateBack) },
                actions = {
                    TextButton(onClick = actions.saveShop) {
                        Text("保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.shopName,
                onValueChange = actions.updateShopName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("店铺名称") },
                placeholder = { Text("如：王记礼事租赁") },
                singleLine = true
            )
            OutlinedTextField(
                value = state.shopContact,
                onValueChange = actions.updateShopContact,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("联系人") },
                placeholder = { Text("如：王老板") },
                singleLine = true
            )
            OutlinedTextField(
                value = state.shopPhone,
                onValueChange = actions.updateShopPhone,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("手机号") },
                placeholder = { Text("如：13800138000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )
            OutlinedTextField(
                value = state.shopAddress,
                onValueChange = actions.updateShopAddress,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("地址") },
                placeholder = { Text("如：XX镇XX村") },
                singleLine = true
            )
            Text(
                text = "这些信息会显示在生成的清单底部，方便客户保存联系方式。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ItemsManageScreen(state: LiShiUiState, actions: LiShiActions) {
    Scaffold(
        topBar = {
            ItemsTopBar(state, actions)
        },
        bottomBar = {
            if (state.sortMode && state.selectedItemIds.isNotEmpty()) {
                Surface(shadowElevation = 3.dp) {
                    Button(
                        onClick = { actions.showDialog(LiShiDialog.BatchDelete) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("删除选中（${state.selectedItemIds.size}）")
                    }
                }
            }
        }
    ) { padding ->
        if (state.items.isEmpty()) {
            EmptyState(
                title = "还没有物品",
                message = "点击右上角 + 添加。",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                actionText = "添加物品",
                onAction = { actions.showDialog(LiShiDialog.AddItem) }
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(state.items, key = { _, item -> item.id }) { _, item ->
                    ItemManageRow(
                        item = item,
                        sortMode = state.sortMode,
                        selected = state.selectedItemIds.contains(item.id),
                        actions = actions
                    )
                    HorizontalDivider()
                }
            }
            Text(
                text = if (state.sortMode) {
                    "点击左侧复选框多选，长按右侧手柄上下拖动调整顺序。"
                } else {
                    "点击物品可改名字，也可以用右上角进入排序和批量导入。"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ItemsTopBar(state: LiShiUiState, actions: LiShiActions) {
    TopAppBar(
        title = { Text(if (state.sortMode) "调整顺序" else "物品管理") },
        navigationIcon = { BackButton(actions.navigateBack) },
        actions = {
            if (state.sortMode && state.items.isNotEmpty()) {
                TextButton(onClick = actions.toggleSelectAll) {
                    Text(if (state.selectedItemIds.size == state.items.size) "取消全选" else "全选")
                }
            }
            if (state.items.isNotEmpty()) {
                IconButton(onClick = { if (state.sortMode) actions.finishSort() else actions.startSort() }) {
                    Icon(
                        painter = painterResource(if (state.sortMode) R.drawable.ic_check_24 else R.drawable.ic_sort_24),
                        contentDescription = if (state.sortMode) "完成" else "排序"
                    )
                }
            }
            if (!state.sortMode) {
                IconButton(onClick = { actions.showDialog(LiShiDialog.BatchImport) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_upload_file_24),
                        contentDescription = "批量导入"
                    )
                }
                IconButton(onClick = { actions.showDialog(LiShiDialog.AddItem) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_24),
                        contentDescription = "添加"
                    )
                }
            }
        }
    )
}

@Composable
private fun ItemManageRow(
    item: Item,
    sortMode: Boolean,
    selected: Boolean,
    actions: LiShiActions
) {
    val haptic = LocalHapticFeedback.current
    val thresholdPx = with(LocalDensity.current) { 52.dp.toPx() }
    ListItem(
        modifier = Modifier.clickable {
            if (sortMode) {
                actions.toggleItemSelection(item.id)
            } else {
                actions.showDialog(LiShiDialog.RenameItem(item.id))
            }
        },
        leadingContent = if (sortMode) {
            {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { actions.toggleItemSelection(item.id) }
                )
            }
        } else {
            null
        },
        headlineContent = {
            Text(
                text = item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            if (sortMode) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.pointerInput(item.id) {
                        var dragRemainder = 0f
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragRemainder = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = { dragRemainder = 0f },
                            onDragCancel = { dragRemainder = 0f },
                            onDrag = { change, dragAmount ->
                                dragRemainder += dragAmount.y
                                while (dragRemainder > thresholdPx) {
                                    actions.moveItem(item.id, 1)
                                    dragRemainder -= thresholdPx
                                }
                                while (dragRemainder < -thresholdPx) {
                                    actions.moveItem(item.id, -1)
                                    dragRemainder += thresholdPx
                                }
                            }
                        )
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_drag_handle_24),
                        contentDescription = "拖动排序"
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { actions.showDialog(LiShiDialog.RenameItem(item.id)) }) {
                        Text("修改")
                    }
                    TextButton(
                        onClick = { actions.showDialog(LiShiDialog.DeleteItem(item.id)) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("删除")
                    }
                }
            }
        }
    )
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back_24),
            contentDescription = "返回"
        )
    }
}

@Composable
private fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
private fun LiShiDialogHost(state: LiShiUiState, actions: LiShiActions) {
    when (val dialog = state.activeDialog) {
        null -> Unit
        LiShiDialog.AddItem -> TextInputDialog(
            title = "添加物品",
            message = "只输入名字，数量在“登记”页录入。",
            hint = "物品名字",
            confirmText = "添加",
            onDismiss = actions.dismissDialog,
            onConfirm = actions.confirmAddItem
        )
        LiShiDialog.BatchImport -> TextInputDialog(
            title = "批量导入",
            message = "支持中文「，」或英文「,」分隔，也可以换行分隔。",
            hint = "在这里粘贴或输入物品名字",
            confirmText = "导入",
            multiline = true,
            onDismiss = actions.dismissDialog,
            onConfirm = actions.confirmBatchImport
        )
        LiShiDialog.BatchDelete -> ConfirmDialog(
            title = "确认删除选中的 ${state.selectedItemIds.size} 个物品？",
            message = "被删除的物品数量会一并清零，此操作不可撤销。",
            confirmText = "确定删除",
            destructive = true,
            onDismiss = actions.dismissDialog,
            onConfirm = actions.confirmBatchDelete
        )
        LiShiDialog.ResetAll -> ConfirmDialog(
            title = "确定要重置吗？",
            message = "所有已登记数量将清零，此操作不可撤销。",
            confirmText = "重置",
            destructive = true,
            onDismiss = actions.dismissDialog,
            onConfirm = actions.confirmResetAll
        )
        LiShiDialog.RenameTitle -> TextInputDialog(
            title = "修改标题",
            hint = "标题",
            initialValue = state.summaryTitle,
            confirmText = "保存",
            onDismiss = actions.dismissDialog,
            onConfirm = actions.confirmRenameTitle
        )
        is LiShiDialog.Message -> MessageDialog(
            title = dialog.title,
            message = dialog.message,
            onDismiss = actions.dismissDialog
        )
        is LiShiDialog.CopyResult -> CopyResultDialog(
            copiedText = dialog.copiedText,
            onDismiss = actions.dismissDialog
        )
        is LiShiDialog.RenameItem -> {
            val item = state.items.firstOrNull { it.id == dialog.itemId }
            if (item == null) {
                LaunchedEffect(dialog) { actions.dismissDialog() }
            } else {
                TextInputDialog(
                    title = "修改名字",
                    hint = "物品名字",
                    initialValue = item.name,
                    confirmText = "保存",
                    onDismiss = actions.dismissDialog,
                    onConfirm = { actions.confirmRenameItem(dialog.itemId, it) }
                )
            }
        }
        is LiShiDialog.DeleteItem -> {
            val item = state.items.firstOrNull { it.id == dialog.itemId }
            if (item == null) {
                LaunchedEffect(dialog) { actions.dismissDialog() }
            } else {
                ConfirmDialog(
                    title = "确定要删除这个物品吗？",
                    message = "点击「确定删除」会移除「${item.name}」，并清空它的数量。",
                    confirmText = "确定删除",
                    destructive = true,
                    onDismiss = actions.dismissDialog,
                    onConfirm = { actions.confirmDeleteItem(dialog.itemId) }
                )
            }
        }
    }
}

@Composable
private fun MessageDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}

@Composable
private fun CopyResultDialog(
    copiedText: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("已复制") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("以下内容已复制到剪贴板：")
                Text(
                    text = copiedText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}

@Composable
private fun TextInputDialog(
    title: String,
    hint: String,
    confirmText: String,
    initialValue: String = "",
    message: String? = null,
    multiline: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(title, initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (message != null) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(hint) },
                    singleLine = !multiline,
                    minLines = if (multiline) 5 else 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (multiline) KeyboardType.Text else KeyboardType.Text
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (destructive) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Preview(name = "登记页", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CountScreenPreview() {
    val sampleItems = rememberPreviewItems()
    LiShiManagerApp(
        state = previewState(items = sampleItems, page = PAGE_COUNT),
        actions = LiShiActions()
    )
}

@Preview(name = "清单页", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SummaryScreenPreview() {
    val sampleItems = rememberPreviewItems()
    LiShiManagerApp(
        state = previewState(items = sampleItems, page = PAGE_SUMMARY),
        actions = LiShiActions()
    )
}

@Composable
private fun rememberPreviewItems(): List<Item> {
    return remember {
        listOf(
            Item(1, "圆桌", 8, 0),
            Item(2, "红色椅子", 64, 1),
            Item(3, "帐篷", 2, 2),
            Item(4, "音响", 0, 3)
        )
    }
}

private fun previewState(items: List<Item>, page: Int): LiShiUiState {
    return LiShiUiState(
        screen = SCREEN_MAIN,
        page = page,
        sortMode = false,
        items = items,
        selectedItemIds = emptyList(),
        summaryTitle = "物品清单",
        dateText = "2026年6月5日 星期五 10:30",
        shopName = "王记礼事租赁",
        shopContact = "王老板",
        shopPhone = "13800138000",
        shopAddress = "XX镇XX村",
        activeDialog = null
    )
}

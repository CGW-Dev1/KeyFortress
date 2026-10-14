package com.keyfortress.app.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ClipboardHelper {

    private var autoClearJob: Job? = null
    private var lastCopiedSecret: String? = null

    /**
     * Copies text to the system clipboard.
     * @param context Application/Activity context
     * @param label Human-readable label (e.g. "账号" or "密码")
     * @param text The text to copy
     * @param isSensitive If true, marks as sensitive on Android 13+ and schedules 30s auto-clear
     */
    fun copyToClipboard(
        context: Context,
        label: String,
        text: String,
        isSensitive: Boolean = false,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        onCleared: (() -> Unit)? = null
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)

        // Android 13+ (API 33) sensitive flag to hide preview popup
        if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            clip.description.extras = extras
        }

        clipboard.setPrimaryClip(clip)

        // Cancel previous auto-clear timer if any
        autoClearJob?.cancel()

        if (isSensitive) {
            lastCopiedSecret = text
            autoClearJob = scope.launch {
                delay(30_000L) // 30 seconds
                // Only clear if clipboard still holds the sensitive text
                val currentClip = clipboard.primaryClip
                if (currentClip != null && currentClip.itemCount > 0) {
                    val currentText = currentClip.getItemAt(0).text?.toString()
                    if (currentText == lastCopiedSecret) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            clipboard.clearPrimaryClip()
                        } else {
                            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                        }
                        lastCopiedSecret = null
                        onCleared?.invoke()
                    }
                }
            }
        }
    }
}

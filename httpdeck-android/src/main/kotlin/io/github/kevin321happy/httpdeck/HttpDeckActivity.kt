package io.github.kevin321happy.httpdeck

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import io.github.kevin321happy.httpdeck.core.HttpDeckRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HttpDeckActivity : Activity() {

    private var all: List<HttpDeckRecord> = emptyList()
    private var query: String = ""
    private lateinit var adapter: RecordAdapter
    private lateinit var countView: TextView

    private val listener: (List<HttpDeckRecord>) -> Unit = { records ->
        runOnUiThread {
            all = records
            render()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContentView(buildRoot())
        HttpDeck.addListener(listener)
    }

    override fun onDestroy() {
        HttpDeck.removeListener(listener)
        super.onDestroy()
    }

    private fun buildRoot(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F6F8"))
            setPadding(dp(16), dp(14), dp(16), dp(10))
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "HttpDeck"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#111827"))
        }
        countView = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#6B7280"))
            setPadding(dp(8), 0, 0, 0)
        }
        val spacer = View(this)
        val clear = TextView(this).apply {
            text = "Clear"
            textSize = 13f
            setTextColor(Color.parseColor("#2563EB"))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { HttpDeck.clear() }
        }
        val close = TextView(this).apply {
            text = "Close"
            textSize = 13f
            setTextColor(Color.parseColor("#2563EB"))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { finish() }
        }
        titleRow.addView(title)
        titleRow.addView(countView)
        titleRow.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        titleRow.addView(clear)
        titleRow.addView(close)
        root.addView(titleRow)

        val search = EditText(this).apply {
            hint = "Search URL or method"
            setHintTextColor(Color.parseColor("#9CA3AF"))
            setTextColor(Color.parseColor("#111827"))
            textSize = 13f
            setSingleLine(true)
            setPadding(dp(12), 0, dp(12), 0)
            setBackgroundColor(Color.WHITE)
        }
        val searchParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(42)
        ).apply { topMargin = dp(12) }
        root.addView(search, searchParams)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString()?.trim().orEmpty()
                render()
            }
        })

        adapter = RecordAdapter(this)
        val listView = ListView(this).apply {
            divider = android.graphics.drawable.ColorDrawable(Color.parseColor("#E5E7EB"))
            dividerHeight = 1
            setBackgroundColor(Color.WHITE)
            this.adapter = this@HttpDeckActivity.adapter
        }
        val listParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ).apply { topMargin = dp(12) }
        root.addView(listView, listParams)
        listView.setOnItemClickListener { _, _, position, _ ->
            filtered().getOrNull(position)?.let { showDetail(it) }
        }
        return root
    }

    private fun filtered(): List<HttpDeckRecord> {
        if (query.isBlank()) return all
        return all.filter {
            it.url.contains(query, ignoreCase = true) ||
                it.method.contains(query, ignoreCase = true)
        }
    }

    private fun render() {
        val items = filtered()
        adapter.submit(items)
        countView.text = "${items.size}/${all.size}"
    }

    private fun showDetail(record: HttpDeckRecord) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(8))
            setBackgroundColor(Color.WHITE)
        }
        val scroll = ScrollView(this)
        val body = TextView(this).apply {
            text = detailText(record)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#111827"))
            setTextIsSelectable(true)
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }
        actions.addView(action("Share") {
            shareRecord(record)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(action("cURL") {
            HttpDeck.copyText(this, "cURL", HttpDeck.curl(record))
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(action("Re-request") {
            confirmReplay(dialog, record)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(action("Close") {
            dialog.dismiss()
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(actions)

        dialog.setContentView(root)
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94f).toInt(),
            (resources.displayMetrics.heightPixels * 0.82f).toInt()
        )
        if (!BuildConfig.DEBUG) {
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun detailText(record: HttpDeckRecord): String {
        return buildString {
            appendLine("${record.method} ${HttpDeck.displayUrl(record.url)}")
            appendLine(
                "source=${HttpDeck.sourceLabel(record.source)} status=${record.statusCode ?: "-"} " +
                    "duration=${record.durationMs}ms"
            )
            appendLine()
            appendLine("--- Request headers ---")
            appendLine(HttpDeck.displayHeaders(record.requestHeaders))
            appendLine("--- Request body ---")
            appendLine(HttpDeck.displayBody(record.requestBody))
            appendLine()
            appendLine("--- Response headers ---")
            appendLine(HttpDeck.displayHeaders(record.responseHeaders))
            appendLine("--- Response body ---")
            appendLine(HttpDeck.displayBody(record.responseBody))
        }
    }

    private fun confirmReplay(detail: Dialog, record: HttpDeckRecord) {
        AlertDialog.Builder(this)
            .setTitle("Re-request ${record.method}")
            .setMessage("This sends a real request through the app's HTTP client. Write APIs may create duplicate data.")
            .setPositiveButton("Send") { _, _ -> doReplay(detail, record) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun doReplay(detail: Dialog, record: HttpDeckRecord) {
        val progress = AlertDialog.Builder(this)
            .setMessage("Sending...")
            .setCancelable(false)
            .create()
        progress.show()
        HttpDeck.replay(this, record) { success, message ->
            if (isFinishing) return@replay
            runCatching { progress.dismiss() }
            val dialog = AlertDialog.Builder(this)
                .setTitle(if (success) "Re-request result" else "Re-request failed")
                .setMessage(message.take(8000))
                .setPositiveButton("Close", null)
                .create()
            dialog.show()
            if (!BuildConfig.DEBUG) {
                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            detail.dismiss()
        }
    }

    private fun shareRecord(record: HttpDeckRecord) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "HttpDeck ${record.method}")
            putExtra(Intent.EXTRA_TEXT, HttpDeck.shareText(record))
        }
        startActivity(Intent.createChooser(intent, "Share with"))
    }

    private fun action(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#2563EB"))
            setPadding(dp(4), dp(8), dp(4), dp(8))
            setOnClickListener { onClick() }
        }
    }

    private fun dp(value: Int): Int {
        return (resources.displayMetrics.density * value + 0.5f).toInt()
    }

    private inner class RecordAdapter(private val activity: Activity) : BaseAdapter() {
        private var items: List<HttpDeckRecord> = emptyList()

        fun submit(records: List<HttpDeckRecord>) {
            items = records
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = items[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row: LinearLayout
            if (convertView == null) {
                row = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), dp(9), dp(10), dp(9))
                    setBackgroundColor(Color.WHITE)
                }
                val top = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val method = TextView(activity).apply {
                    textSize = 11f
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                }
                val path = TextView(activity).apply {
                    textSize = 13f
                    setTextColor(Color.parseColor("#111827"))
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
                val status = TextView(activity).apply {
                    textSize = 12f
                    setTypeface(typeface, Typeface.BOLD)
                }
                top.addView(method, LinearLayout.LayoutParams(dp(56), dp(22)))
                top.addView(path, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(8)
                    rightMargin = dp(8)
                })
                top.addView(status, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ))
                val meta = TextView(activity).apply {
                    textSize = 10f
                    setTextColor(Color.parseColor("#6B7280"))
                    setPadding(0, dp(4), 0, 0)
                }
                row.addView(top)
                row.addView(meta)
                row.tag = ViewHolder(method, path, status, meta)
            } else {
                row = convertView as LinearLayout
            }
            val holder = row.tag as ViewHolder
            val item = items[position]
            holder.method.text = item.method
            holder.method.setBackgroundColor(methodColor(item.method))
            holder.path.text = pathOf(item.url)
            holder.status.text = item.statusCode?.toString() ?: "ERR"
            holder.status.setTextColor(statusColor(item.statusCode, item.error))
            holder.meta.text = "${HttpDeck.sourceLabel(item.source)}  ${item.durationMs}ms  " +
                "${formatBytes((item.requestBody?.length ?: 0) + (item.responseBody?.length ?: 0))}"
            return row
        }
    }

    private class ViewHolder(
        val method: TextView,
        val path: TextView,
        val status: TextView,
        val meta: TextView
    )

    private fun pathOf(url: String): String {
        return runCatching {
            val uri = Uri.parse(url)
            uri.encodedPath ?: url
        }.getOrDefault(url)
    }

    private fun methodColor(method: String): Int {
        return when (method.uppercase(Locale.US)) {
            "GET" -> Color.parseColor("#2E7D32")
            "POST" -> Color.parseColor("#1565C0")
            "PUT" -> Color.parseColor("#6A1B9A")
            "DELETE" -> Color.parseColor("#C62828")
            else -> Color.parseColor("#455A64")
        }
    }

    private fun statusColor(status: Int?, error: String?): Int {
        if (status == null) return if (error.isNullOrBlank()) Color.GRAY else Color.parseColor("#DC2626")
        return when (status) {
            in 200..299 -> Color.parseColor("#16A34A")
            in 300..399 -> Color.parseColor("#2563EB")
            in 400..499 -> Color.parseColor("#D97706")
            in 500..599 -> Color.parseColor("#DC2626")
            else -> Color.GRAY
        }
    }

    private fun formatBytes(bytes: Int): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1fKB", bytes / 1024f)
            else -> String.format(Locale.US, "%.1fMB", bytes / 1024f / 1024f)
        }
    }

    @Suppress("unused")
    private fun formatTime(timestamp: Long): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
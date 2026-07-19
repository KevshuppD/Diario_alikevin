package calendario.kevshupp.diariokevinali

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.drawable.ColorDrawable
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import java.util.UUID

class MessageEditor(
    private val context: Context,
    private val coupleId: String,
    private val userId: String,
    private val userName: String,
    private val userImageUri: String
) {
    private var currentSelectedImageUrl: String? = null
    private var currentTheme = "Pixel Claro"
    private var currentDialogView: View? = null

    interface EditorCallback {
        fun onSave(message: Message)
        fun onPickImage(code: Int)
    }

    fun showEditDialog(edit: Message?, callback: EditorCallback) {
        val b = AlertDialog.Builder(context)
        val v = LayoutInflater.from(context).inflate(R.layout.dialog_edit_message, null)
        currentDialogView = v
        b.setView(v)

        if ("Pixel Oscuro" == currentTheme) {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel_dark)
            v.findViewById<TextView>(R.id.tvDialogTitle)?.setTextColor(Color.WHITE)
            v.findViewById<EditText>(R.id.etDialogTitle)?.apply {
                setTextColor(Color.WHITE)
                setHintTextColor(Color.LTGRAY)
            }
            v.findViewById<EditText>(R.id.etDialogMessage)?.apply {
                setTextColor(Color.WHITE)
                setHintTextColor(Color.LTGRAY)
                setBackgroundResource(R.drawable.bg_message_pixel_dark)
            }
            v.findViewById<Button>(R.id.btnSave)?.setTextColor(Color.WHITE)
            v.findViewById<Button>(R.id.btnCancel)?.setTextColor(Color.WHITE)
            v.findViewById<Button>(R.id.btnAddImage)?.setTextColor(Color.WHITE)
            v.findViewById<Button>(R.id.btnBold)?.setTextColor(Color.WHITE)
            v.findViewById<Button>(R.id.btnItalic)?.setTextColor(Color.WHITE)
            v.findViewById<Button>(R.id.btnColor)?.setTextColor(Color.WHITE)
        }

        val et = v.findViewById<EditText>(R.id.etDialogMessage)
        val etTitle = v.findViewById<EditText>(R.id.etDialogTitle)
        val ivSelectedImage = v.findViewById<ImageView>(R.id.ivSelectedImage)
        val imageContainer = v.findViewById<View>(R.id.imageContainer)

        if (edit != null) {
            if (edit.title != null) etTitle.setText(edit.title)
            et.setText(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    Html.fromHtml(edit.content ?: "", Html.FROM_HTML_MODE_COMPACT)
                else
                    Html.fromHtml(edit.content ?: "")
            )
            currentSelectedImageUrl = edit.imageUrl
            if (currentSelectedImageUrl != null) {
                imageContainer.visibility = View.VISIBLE
                Glide.with(context).load(currentSelectedImageUrl).into(ivSelectedImage)
            }
        }

        v.findViewById<View>(R.id.btnBold).setOnClickListener {
            applySpan(et, StyleSpan::class.java, StyleSpan(Typeface.BOLD))
        }
        v.findViewById<View>(R.id.btnItalic).setOnClickListener {
            applySpan(et, StyleSpan::class.java, StyleSpan(Typeface.ITALIC))
        }
        v.findViewById<View>(R.id.btnColor).setOnClickListener {
            val colors = intArrayOf(Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.BLACK, Color.GRAY)
            val names = arrayOf("Rojo", "Azul", "Verde", "Rosa", "Negro", "Gris")
            AlertDialog.Builder(context)
                .setItems(names) { _, w ->
                    applySpan(et, ForegroundColorSpan::class.java, ForegroundColorSpan(colors[w]))
                }.show()
        }

        v.findViewById<View>(R.id.btnAddImage).setOnClickListener {
            callback.onPickImage(3) // Code for Carta
        }
        v.findViewById<View>(R.id.btnRemoveImage).setOnClickListener {
            currentSelectedImageUrl = null
            imageContainer.visibility = View.GONE
        }

        val dialog = b.create()
        dialog.setOnDismissListener { currentDialogView = null }
        v.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        v.findViewById<View>(R.id.btnSave).setOnClickListener {
            var html = ""
            if (et.text != null) {
                html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    Html.toHtml(et.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                else
                    Html.toHtml(et.text)
            }

            val m = edit ?: Message(
                UUID.randomUUID().toString(),
                coupleId,
                userId,
                userName,
                userImageUri,
                html,
                ArrayList(),
                System.currentTimeMillis(),
                false
            )
            if (etTitle.text != null) m.title = etTitle.text.toString()
            if (edit != null) m.content = html
            m.imageUrl = currentSelectedImageUrl
            callback.onSave(m)
            dialog.dismiss()
        }
        dialog.show()
    }

    fun showMessageDetail(msg: Message) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val v = LayoutInflater.from(context).inflate(R.layout.dialog_view_message, null)
        dialog.setContentView(v)

        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            decorView.setPadding(0, 0, 0, 0)

            // Aplicar flags inmersivos
            val flags = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            decorView.systemUiVisibility = flags
        }

        val btnClose = v.findViewById<Button>(R.id.btnClose)

        if ("Pixel Oscuro" == currentTheme) {
            v.setBackgroundColor(Color.parseColor("#0D0D2B"))
            v.findViewById<TextView>(R.id.tvViewAuthor).setTextColor(Color.WHITE)
            v.findViewById<TextView>(R.id.tvViewTimestamp).setTextColor(Color.WHITE)
            v.findViewById<TextView>(R.id.tvViewContent).setTextColor(Color.WHITE)
            v.findViewById<TextView>(R.id.tvViewTitle)?.setTextColor(Color.WHITE)
            btnClose.setTextColor(Color.WHITE)
            btnClose.setBackgroundColor(Color.parseColor("#1A1A2E"))
        } else {
            v.setBackgroundColor(Color.parseColor("#F5F5F5"))
            v.findViewById<TextView>(R.id.tvViewTitle)?.setTextColor(Color.parseColor("#4A2511"))
            btnClose.setTextColor(Color.WHITE)
            btnClose.setBackgroundColor(Color.parseColor("#5D2E7A"))
        }

        val tvAuthor = v.findViewById<TextView>(R.id.tvViewAuthor)
        val tvTimestamp = v.findViewById<TextView>(R.id.tvViewTimestamp)
        tvAuthor.text = "De: ${msg.authorName}"

        val tvTitle = v.findViewById<TextView>(R.id.tvViewTitle)
        if (msg.title != null && msg.title?.trim()?.isNotEmpty() == true) {
            tvTitle.visibility = View.VISIBLE
            tvTitle.text = msg.title
        } else {
            tvTitle.visibility = View.GONE
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        tvTimestamp.text = sdf.format(Date(msg.timestamp))

        val tv = v.findViewById<TextView>(R.id.tvViewContent)
        tv.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(msg.content ?: "", Html.FROM_HTML_MODE_COMPACT)
        else
            Html.fromHtml(msg.content ?: "")

        val iv = v.findViewById<ImageView>(R.id.ivViewImage)
        if (msg.imageUrl != null) {
            iv.visibility = View.VISIBLE
            Glide.with(context).load(msg.imageUrl).centerCrop().into(iv)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun applySpan(et: EditText, spanClass: Class<*>, newSpan: Any) {
        val start = et.selectionStart
        val end = et.selectionEnd
        if (start == -1 || end == -1 || start == end) return
        val editable = et.text
        val existing = editable.getSpans(start, end, spanClass)
        for (s in existing) {
            editable.removeSpan(s)
        }
        editable.setSpan(newSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun setImageUrl(url: String) {
        this.currentSelectedImageUrl = url
        // Si el diálogo está abierto, queremos que la imagen se vea inmediatamente
        currentDialogView?.let { view ->
            val ivSelectedImage = view.findViewById<ImageView>(R.id.ivSelectedImage)
            val imageContainer = view.findViewById<View>(R.id.imageContainer)
            if (ivSelectedImage != null && imageContainer != null) {
                imageContainer.visibility = View.VISIBLE
                Glide.with(context).load(url).into(ivSelectedImage)
            }
        }
    }

    fun setTheme(theme: String) {
        this.currentTheme = theme
    }

    fun uploadImage(uri: Uri, callback: UploadCallback) {
        MediaManager.get().upload(uri)
            .callback(callback)
            .dispatch()
    }
}

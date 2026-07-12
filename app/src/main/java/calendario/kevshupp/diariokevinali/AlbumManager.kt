package calendario.kevshupp.diariokevinali

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.widget.*
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.util.*
import java.text.SimpleDateFormat
import android.net.Uri

class AlbumManager(
    private val context: Context,
    private val coupleId: String,
    private val userId: String,
    private val userName: String,
    private val userImageUri: String?
) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val currentAlbumImages = mutableListOf<String>()
    private var currentTheme = "Pixel Claro"
    private var previewAdapter: RecyclerView.Adapter<*>? = null
    private var deferredMessageId: String? = null
    private var allowDeferredUploads = false
    private var pendingAlbumUploads = 0
    private var activeSaveButton: Button? = null

    interface AlbumCallback {
        fun onPickImage()
        fun onMomentSaved()
    }

    fun setTheme(theme: String) {
        this.currentTheme = theme
    }


    fun showAddMomentDialog(callback: AlbumCallback) {
        val b = AlertDialog.Builder(context)
        val v = LayoutInflater.from(context).inflate(R.layout.dialog_edit_message, null)
        activeDialogView = v
        b.setView(v)
        v.findViewById<TextView>(R.id.tvDialogTitle).text = "Nuestro Álbum"
        v.findViewById<View>(R.id.formatToolbar).visibility = View.GONE
        v.findViewById<View>(R.id.etDialogTitle).visibility = View.GONE

        clearDeferredUploadTarget()

        val et = v.findViewById<EditText>(R.id.etDialogMessage)
        et.hint = "Cuéntame algo sobre este momento..."

        val rvPreview = v.findViewById<RecyclerView>(R.id.rvAlbumPreview)
        rvPreview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val pv = LayoutInflater.from(context).inflate(R.layout.item_album_preview, parent, false)
                return object : RecyclerView.ViewHolder(pv) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val url = currentAlbumImages[position]
                Glide.with(context).load(url).centerCrop().into(holder.itemView.findViewById(R.id.ivPreviewPhoto))
                holder.itemView.findViewById<View>(R.id.tvRemovePhoto).setOnClickListener {
                    currentAlbumImages.removeAt(position)
                    notifyDataSetChanged()
                    rvPreview.visibility = if (currentAlbumImages.isEmpty()) View.GONE else View.VISIBLE
                    updateSaveEnabled()
                }
            }

            override fun getItemCount(): Int = currentAlbumImages.size
        }
        previewAdapter = adapter
        rvPreview.adapter = adapter
        rvPreview.visibility = if (currentAlbumImages.isEmpty()) View.GONE else View.VISIBLE

        if ("Pixel Oscuro" == currentTheme) {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel_dark)
            v.findViewById<TextView>(R.id.tvDialogTitle).setTextColor(Color.WHITE)
            et.setTextColor(Color.WHITE)
            et.setHintTextColor(Color.LTGRAY)
            et.setBackgroundResource(R.drawable.bg_message_pixel_dark)
            v.findViewById<Button>(R.id.btnSave).setTextColor(Color.WHITE)
            v.findViewById<Button>(R.id.btnCancel).setTextColor(Color.WHITE)
            v.findViewById<Button>(R.id.btnAddImage).setTextColor(Color.WHITE)
        }

        currentAlbumImages.clear()
        v.findViewById<View>(R.id.btnAddImage).setOnClickListener { callback.onPickImage() }

        val dialog = b.create()
        val saved = booleanArrayOf(false)
        activeSaveButton = v.findViewById(R.id.btnSave)
        updateSaveEnabled()

        v.findViewById<View>(R.id.btnSave).setOnClickListener {
            val content = et.text.toString().trim()
            if (content.isEmpty() && currentAlbumImages.isEmpty()) return@setOnClickListener

            val m = Message(
                UUID.randomUUID().toString(), coupleId, userId, userName, userImageUri,
                "[ALBUM] $content", currentAlbumImages.toMutableList(), System.currentTimeMillis(), false
            )
            val docId = m.messageId ?: UUID.randomUUID().toString()
            db.collection("messages").document(docId).set(m).addOnSuccessListener {
                saved[0] = true
                setDeferredUploadTarget(docId)
                callback.onMomentSaved()
                dialog.dismiss()
            }
        }
        v.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener {
            activeSaveButton = null
            activeDialogView = null
            if (!saved[0]) {
                clearDeferredUploadTarget()
            }
        }
        dialog.show()
    }


    fun showAlbumDetail(msg: Message) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val v = LayoutInflater.from(context).inflate(R.layout.dialog_message_detail, null)
        dialog.setContentView(v)

        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            decorView.setPadding(0, 0, 0, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                insetsController?.let { controller ->
                    controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }
        }

        val btnEdit = v.findViewById<Button>(R.id.btnMessageDetailEdit)
        val btnClose = v.findViewById<Button>(R.id.btnMessageDetailClose)
        val btnMenu = v.findViewById<TextView>(R.id.btnMessageDetailMenu)

        if (msg.authorId == userId) {
            btnEdit.visibility = View.VISIBLE
        }

        if ("Pixel Oscuro" == currentTheme) {
            v.setBackgroundColor(Color.parseColor("#0D0D2B"))
            v.findViewById<TextView>(R.id.tvMessageDetailTitle).setTextColor(Color.WHITE)
            v.findViewById<TextView>(R.id.tvMessageDetailContent).setTextColor(Color.WHITE)
            btnMenu.setTextColor(Color.WHITE)
            btnEdit.setTextColor(Color.WHITE)
            btnEdit.setBackgroundColor(Color.parseColor("#1A1A2E"))
            btnClose.setTextColor(Color.WHITE)
            btnClose.setBackgroundColor(Color.parseColor("#1A1A2E"))
        } else {
            v.setBackgroundColor(Color.parseColor("#F5F5F5"))
            btnMenu.setTextColor(Color.parseColor("#4A2511"))
            btnEdit.setBackgroundColor(Color.parseColor("#5D2E7A"))
            btnClose.setBackgroundColor(Color.parseColor("#5D2E7A"))
        }

        v.findViewById<TextView>(R.id.tvMessageDetailTitle).text = "Detalle del Momento"
        val tvContent = v.findViewById<TextView>(R.id.tvMessageDetailContent)
        tvContent.text = msg.content?.replace("[ALBUM] ", "") ?: ""

        val ivMain = v.findViewById<ImageView>(R.id.ivMessageDetailImage)
        val rvPhotos = v.findViewById<RecyclerView>(R.id.rvMessageDetailPhotos)

        btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(context, view)
            popup.menu.add("2 columnas")
            popup.menu.add("3 columnas")
            popup.menu.add("4 columnas")
            popup.menu.add("5 columnas")
            popup.menu.add("6 columnas")
            popup.setOnMenuItemClickListener { item ->
                val cols = when (item.title) {
                    "2 columnas" -> 2
                    "3 columnas" -> 3
                    "4 columnas" -> 4
                    "5 columnas" -> 5
                    "6 columnas" -> 6
                    else -> 2
                }
                (rvPhotos.layoutManager as? GridLayoutManager)?.spanCount = cols
                rvPhotos.adapter?.notifyDataSetChanged()
                true
            }
            popup.show()
        }

        val urls = msg.imageUrls
        if (urls?.isNotEmpty() == true) {
            ivMain.visibility = View.GONE
            rvPhotos.visibility = View.VISIBLE
            rvPhotos.layoutManager = GridLayoutManager(context, 2)
            rvPhotos.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val gv = LayoutInflater.from(context).inflate(R.layout.item_album_gallery, parent, false)
                    return object : RecyclerView.ViewHolder(gv) {}
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val url = urls[position]
                    val iv = holder.itemView.findViewById<ImageView>(R.id.ivGalleryImage)
                    Glide.with(context).load(url).centerCrop().into(iv)
                    holder.itemView.setOnClickListener { showFullScreenImage(url, msg) }
                    holder.itemView.setOnLongClickListener {
                        if (msg.authorId == userId) {
                            AlertDialog.Builder(context).setTitle("Eliminar foto").setMessage("¿Deseas eliminar esta foto de este momento?")
                                .setPositiveButton("Eliminar") { _, _ ->
                                    urls.removeAt(position)
                                    val docId = msg.messageId
                                    if (docId != null) {
                                        if (urls.isEmpty()) {
                                            db.collection("messages").document(docId).delete()
                                        } else {
                                            msg.imageUrls = urls
                                            db.collection("messages").document(docId).set(msg)
                                        }
                                    }
                                    dialog.dismiss()
                                    showAlbumDetail(msg)
                                }.setNegativeButton("Cancelar", null).show()
                        }
                        true
                    }
                }
                override fun getItemCount(): Int = urls.size
            }
        }

        btnEdit.setOnClickListener {
            dialog.dismiss()
            showEditAlbumDialog(msg)
        }
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    fun showEditAlbumDialog(msg: Message) {
        val b = AlertDialog.Builder(context)
        val v = LayoutInflater.from(context).inflate(R.layout.dialog_edit_message, null)
        b.setView(v)
        v.findViewById<TextView>(R.id.tvDialogTitle).text = "Editar Momento"
        v.findViewById<View>(R.id.formatToolbar).visibility = View.GONE
        v.findViewById<View>(R.id.etDialogTitle).visibility = View.GONE

        val et = v.findViewById<EditText>(R.id.etDialogMessage)
        et.setText(msg.content?.replace("[ALBUM] ", "") ?: "")

        val rvPreview = v.findViewById<RecyclerView>(R.id.rvAlbumPreview)
        rvPreview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        
        currentAlbumImages.clear()
        msg.imageUrls?.let { currentAlbumImages.addAll(it) }

        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val pv = LayoutInflater.from(context).inflate(R.layout.item_album_preview, parent, false)
                return object : RecyclerView.ViewHolder(pv) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val url = currentAlbumImages[position]
                Glide.with(context).load(url).centerCrop().into(holder.itemView.findViewById(R.id.ivPreviewPhoto))
                holder.itemView.findViewById<View>(R.id.tvRemovePhoto).setOnClickListener {
                    currentAlbumImages.removeAt(position)
                    notifyDataSetChanged()
                    rvPreview.visibility = if (currentAlbumImages.isEmpty()) View.GONE else View.VISIBLE
                }
            }
            override fun getItemCount(): Int = currentAlbumImages.size
        }
        rvPreview.adapter = adapter
        rvPreview.visibility = if (currentAlbumImages.isEmpty()) View.GONE else View.VISIBLE

        v.findViewById<Button>(R.id.btnSave).setOnClickListener {
            msg.content = "[ALBUM] " + et.text.toString().trim()
            msg.imageUrls = currentAlbumImages.toMutableList()
            msg.messageId?.let { id ->
                db.collection("messages").document(id).set(msg)
            }
            it.rootView.handler?.post { Toast.makeText(context, "Momento actualizado", Toast.LENGTH_SHORT).show() }
        }

        val dialog = b.create()
        v.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    fun showFullScreenImage(url: String, parentMessage: Message? = null, onRefresh: (() -> Unit)? = null) {
        val d = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        
        // Root container
        val root = RelativeLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        }
        
        // ImageView
        val iv = ImageView(context).apply {
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        Glide.with(context).load(url).into(iv)
        root.addView(iv)
        
        // Parse metadata
        val isLocal = url.startsWith("content://")
        var fileName = ""
        var fileSize = 0L
        var fileDate = 0L
        if (isLocal) {
            try {
                val uri = Uri.parse(url)
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        val modIdx = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                        if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: ""
                        if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                        if (modIdx != -1) fileDate = cursor.getLong(modIdx)
                    }
                }
            } catch (e: Exception) {
                val doc = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, Uri.parse(url))
                fileName = doc?.name ?: ""
                fileSize = doc?.length() ?: 0L
                fileDate = doc?.lastModified() ?: 0L
            }
        } else {
            fileName = url.substringAfterLast("/").substringBefore("?")
        }
        if (fileName.isEmpty()) {
            fileName = "Foto_${System.currentTimeMillis()}"
        }
        
        // Load custom font vt323
        val vt323 = try {
            androidx.core.content.res.ResourcesCompat.getFont(context, R.font.vt323)
        } catch (e: Exception) {
            android.graphics.Typeface.DEFAULT
        }
        
        // Theme styling variables
        val isDark = currentTheme == "Pixel Oscuro"
        val isMono = currentTheme == "Pixel Monocromático"
        
        val barBgColor = when {
            isDark -> Color.parseColor("#E60D0D2B") // 90% opacity deep blue
            isMono -> Color.parseColor("#E6FFFFFF") // 90% opacity white
            else -> Color.parseColor("#E6FFFDF9")   // 90% opacity cream
        }
        
        val textColor = when {
            isDark -> Color.WHITE
            isMono -> Color.BLACK
            else -> Color.parseColor("#4A2511")
        }
        
        val buttonBgColor = when {
            isDark -> Color.parseColor("#1A1A2E")
            isMono -> Color.BLACK
            else -> Color.parseColor("#8B4513")
        }
        
        val buttonTextColor = Color.WHITE
        
        // Top Bar
        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(barBgColor)
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            gravity = Gravity.CENTER_VERTICAL
            val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
            }
            layoutParams = params
        }
        
        // Back Button
        val btnBack = TextView(context).apply {
            text = "✕ Volver"
            typeface = vt323
            textSize = 20f
            setTextColor(textColor)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(16), dpToPx(8))
            isClickable = true
            setOnClickListener { d.dismiss() }
        }
        topBar.addView(btnBack)
        
        // Filename Text
        val tvFileName = TextView(context).apply {
            text = fileName
            typeface = vt323
            textSize = 20f
            setTextColor(textColor)
            ellipsize = android.text.TextUtils.TruncateAt.END
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topBar.addView(tvFileName)
        root.addView(topBar)
        
        val resId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val navBarHeight = if (resId > 0) context.resources.getDimensionPixelSize(resId) else dpToPx(48)

        // Bottom Bar
        val bottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(barBgColor)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), navBarHeight + dpToPx(8))
            gravity = Gravity.CENTER
            val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            }
            layoutParams = params
        }
        
        // Helper function to create standard pixel buttons
        fun createPixelButton(label: String, onClick: () -> Unit): TextView {
            return TextView(context).apply {
                text = label
                typeface = vt323
                textSize = 20f
                setTextColor(buttonTextColor)
                gravity = Gravity.CENTER
                
                // Pixel/Retro border and background
                val borderDrawable = android.graphics.drawable.GradientDrawable().apply {
                    setColor(buttonBgColor)
                    setStroke(dpToPx(2), textColor)
                }
                background = borderDrawable
                
                val params = LinearLayout.LayoutParams(0, dpToPx(45), 1f).apply {
                    setMargins(dpToPx(6), 0, dpToPx(6), 0)
                }
                layoutParams = params
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
        }
        
        // Info Button
        val btnInfo = createPixelButton("Información") {
            val formattedDate = if (fileDate > 0L) {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fileDate))
            } else "Desconocida"
            
            val formattedSize = if (fileSize > 0L) {
                val kb = fileSize / 1024.0
                if (kb > 1024.0) {
                    String.format(Locale.getDefault(), "%.2f MB", kb / 1024.0)
                } else {
                    String.format(Locale.getDefault(), "%.2f KB", kb)
                }
            } else "Desconocido"
            
            val infoMsg = """
                Nombre: $fileName
                Fecha: $formattedDate
                Tamaño: $formattedSize
                Tipo: ${if (isLocal) "Foto Local" else "Foto en la Nube"}
            """.trimIndent()
            
            val alertB = AlertDialog.Builder(context)
            
            // Create a programmatic vertical layout to prevent overlapping in ConstraintLayout
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24))
                
                if (isDark) {
                    setBackgroundResource(R.drawable.bg_parchment_pixel_dark)
                } else if (isMono) {
                    setBackgroundColor(Color.WHITE)
                } else {
                    setBackgroundResource(R.drawable.bg_parchment_pixel)
                }
            }
            
            val dTitle = TextView(context).apply {
                text = "Detalles de la Foto"
                typeface = vt323
                textSize = 28f
                setTextColor(if (isDark) Color.WHITE else if (isMono) Color.BLACK else Color.parseColor("#4A2511"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dpToPx(16))
                }
            }
            layout.addView(dTitle)
            
            val contentTv = TextView(context).apply {
                text = infoMsg
                typeface = vt323
                textSize = 20f
                setTextColor(if (isDark) Color.WHITE else if (isMono) Color.BLACK else Color.parseColor("#4A2511"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dpToPx(20))
                }
            }
            layout.addView(contentTv)
            
            val closeBtn = TextView(context).apply {
                text = "Entendido"
                typeface = vt323
                textSize = 20f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                val borderDrawable = android.graphics.drawable.GradientDrawable().apply {
                    setColor(buttonBgColor)
                    setStroke(dpToPx(2), textColor)
                }
                background = borderDrawable
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(50))
                isClickable = true
                isFocusable = true
            }
            layout.addView(closeBtn)
            
            alertB.setView(layout)
            val infoDialog = alertB.create()
            closeBtn.setOnClickListener { infoDialog.dismiss() }
            infoDialog.show()
        }
        bottomBar.addView(btnInfo)
        
        // Share Button
        val btnShare = createPixelButton("Compartir") {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    if (url.startsWith("http")) {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    } else {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(url))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir foto"))
            } catch (e: Exception) {
                Toast.makeText(context, "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        bottomBar.addView(btnShare)
        
        // Edit Button (Only rename if local, or edit caption if it is a Cloudinary photo with parentMessage)
        if (isLocal || parentMessage != null) {
            val btnEdit = createPixelButton("Editar") {
                if (isLocal) {
                    val alertRename = AlertDialog.Builder(context)
                    val input = EditText(context).apply {
                        setText(fileName)
                        setSelection(fileName.length)
                        typeface = vt323
                        textSize = 20f
                        setTextColor(textColor)
                    }
                    
                    if (isDark) {
                        input.setBackgroundResource(R.drawable.bg_message_pixel_dark)
                    }
                    
                    val layout = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16))
                        addView(input)
                    }
                    
                    alertRename.setTitle("Renombrar Foto")
                        .setView(layout)
                        .setPositiveButton("Guardar") { _, _ ->
                            val newName = input.text.toString().trim()
                            if (newName.isNotEmpty() && newName != fileName) {
                                try {
                                    val uri = Uri.parse(url)
                                    android.provider.DocumentsContract.renameDocument(context.contentResolver, uri, newName)
                                    Toast.makeText(context, "Archivo renombrado", Toast.LENGTH_SHORT).show()
                                    tvFileName.text = newName
                                    fileName = newName
                                    onRefresh?.invoke()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al renombrar: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else if (parentMessage != null) {
                    d.dismiss()
                    showEditAlbumDialog(parentMessage)
                }
            }
            bottomBar.addView(btnEdit)
        }
        root.addView(bottomBar)
        
        // Tapping the image hides/shows the bars
        var barsVisible = true
        iv.setOnClickListener {
            barsVisible = !barsVisible
            topBar.visibility = if (barsVisible) View.VISIBLE else View.GONE
            bottomBar.visibility = if (barsVisible) View.VISIBLE else View.GONE
        }
        
        d.setContentView(root)
        d.show()
    }

    private var activeDialogView: View? = null

    fun addImageUrl(url: String) {
        currentAlbumImages.add(url)
        previewAdapter?.notifyDataSetChanged()
        activeDialogView?.findViewById<View>(R.id.rvAlbumPreview)?.visibility = View.VISIBLE
        
        // Si hay una subida diferida activa, actualizar inmediatamente Firestore
        if (allowDeferredUploads && deferredMessageId != null) {
            db.collection("messages").document(deferredMessageId!!)
                .update("imageUrls", com.google.firebase.firestore.FieldValue.arrayUnion(url))
        }
        updateSaveEnabled()
    }

    fun setDeferredUploadTarget(messageId: String) {
        this.deferredMessageId = messageId
        this.allowDeferredUploads = true
    }

    fun clearDeferredUploadTarget() {
        this.deferredMessageId = null
        this.allowDeferredUploads = false
        this.pendingAlbumUploads = 0
    }

    fun onAlbumUploadStarted() {
        pendingAlbumUploads++
        updateSaveEnabled()
    }

    fun onAlbumUploadFinished() {
        if (pendingAlbumUploads > 0) pendingAlbumUploads--
        updateSaveEnabled()
    }


    private fun updateSaveEnabled() {
        activeSaveButton?.let { btn ->
            btn.isEnabled = (pendingAlbumUploads == 0)
            btn.alpha = if (btn.isEnabled) 1.0f else 0.5f
            btn.text = if (btn.isEnabled) "Guardar" else "Subiendo..."
        }
    }

    fun getLocalPhotos(): List<LocalPhoto> {
        val prefs = context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val localFolderUriStr = prefs.getString("syncLocalFolderUri", null) ?: return emptyList()
        val fileList = mutableListOf<LocalPhoto>()
        val contentResolver = context.contentResolver
        try {
            val treeUri = Uri.parse(localFolderUriStr)
            val documentId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
            
            val projection = arrayOf(
                android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
                android.provider.DocumentsContract.Document.COLUMN_SIZE
            )
            
            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val docIdIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val lastModifiedIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val mimeTypeIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_SIZE)
                
                while (cursor.moveToNext()) {
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) else null
                    val docId = if (docIdIndex != -1) cursor.getString(docIdIndex) else null
                    val mimeType = if (mimeTypeIndex != -1) cursor.getString(mimeTypeIndex) ?: "" else ""
                    
                    if (!name.isNullOrEmpty() && !docId.isNullOrEmpty() && mimeType.startsWith("image/")) {
                        val fileUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        var lastModified = if (lastModifiedIndex != -1) cursor.getLong(lastModifiedIndex) else 0L
                        if (lastModified == 0L) {
                            try {
                                val docFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, fileUri)
                                lastModified = docFile?.lastModified() ?: 0L
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                        val size = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
                        fileList.add(LocalPhoto(fileUri.toString(), name, lastModified, size))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AlbumManager", "Error listing local photos: " + e.message)
        }
        fileList.sortByDescending { it.lastModified }
        return fileList
    }
}

data class LocalPhoto(val uri: String, val name: String, val lastModified: Long, val size: Long)

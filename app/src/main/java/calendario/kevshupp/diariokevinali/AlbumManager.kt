package calendario.kevshupp.diariokevinali

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.util.*

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
            val flags = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            decorView.systemUiVisibility = flags
        }

        val btnEdit = v.findViewById<Button>(R.id.btnMessageDetailEdit)
        val btnClose = v.findViewById<Button>(R.id.btnMessageDetailClose)

        if (msg.authorId == userId) {
            btnEdit.visibility = View.VISIBLE
        }

        if ("Pixel Oscuro" == currentTheme) {
            v.setBackgroundColor(Color.parseColor("#0D0D2B"))
            v.findViewById<TextView>(R.id.tvMessageDetailTitle).setTextColor(Color.WHITE)
            v.findViewById<TextView>(R.id.tvMessageDetailContent).setTextColor(Color.WHITE)
            btnEdit.setTextColor(Color.WHITE)
            btnEdit.setBackgroundColor(Color.parseColor("#1A1A2E"))
            btnClose.setTextColor(Color.WHITE)
            btnClose.setBackgroundColor(Color.parseColor("#1A1A2E"))
        } else {
            v.setBackgroundColor(Color.parseColor("#F5F5F5"))
            btnEdit.setBackgroundColor(Color.parseColor("#5D2E7A"))
            btnClose.setBackgroundColor(Color.parseColor("#5D2E7A"))
        }

        v.findViewById<TextView>(R.id.tvMessageDetailTitle).text = "Detalle del Momento"
        val tvContent = v.findViewById<TextView>(R.id.tvMessageDetailContent)
        tvContent.text = msg.content?.replace("[ALBUM] ", "") ?: ""

        val ivMain = v.findViewById<ImageView>(R.id.ivMessageDetailImage)
        val rvPhotos = v.findViewById<RecyclerView>(R.id.rvMessageDetailPhotos)

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
                    holder.itemView.setOnClickListener { showFullScreenImage(url) }
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

    private fun showFullScreenImage(url: String) {
        val d = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val iv = ImageView(context)
        iv.setBackgroundColor(Color.BLACK)
        Glide.with(context).load(url).into(iv)
        iv.setOnClickListener { d.dismiss() }
        d.setContentView(iv)
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
}

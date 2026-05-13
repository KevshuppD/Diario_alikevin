package calendario.kevshupp.diariokevinali;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AlbumManager {
    private final Context context;
    private final FirebaseFirestore db;
    private final String coupleId;
    private final String userId;
    private final String userName;
    private final String userImageUri;
    private List<String> currentAlbumImages = new ArrayList<>();
    private String currentTheme = "Pixel Claro";
    private RecyclerView.Adapter previewAdapter;
    private String deferredMessageId = null;
    private boolean allowDeferredUploads = false;
    private int pendingAlbumUploads = 0;
    private Button activeSaveButton;

    public interface AlbumCallback {
        void onPickImage();
        void onMomentSaved();
    }

    public AlbumManager(Context context, String coupleId, String userId, String userName, String userImageUri) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.coupleId = coupleId;
        this.userId = userId;
        this.userName = userName;
        this.userImageUri = userImageUri;
    }

    public void showAlbumOptions(AlbumCallback callback) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_album_options, null);
        b.setView(v);

        if ("Pixel Oscuro".equals(currentTheme)) {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            ((TextView) v.findViewById(R.id.tvOptionsTitle)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnOptionAdd)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnOptionView)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnOptionCancel)).setTextColor(Color.WHITE);
        }

        AlertDialog dialog = b.create();

        v.findViewById(R.id.btnOptionAdd).setOnClickListener(v1 -> {
            dialog.dismiss();
            showAddMomentDialog(callback);
        });
        v.findViewById(R.id.btnOptionView).setOnClickListener(v1 -> {
            dialog.dismiss();
            showSharedAlbum(callback);
        });
        v.findViewById(R.id.btnOptionCancel).setOnClickListener(v1 -> dialog.dismiss());
        dialog.show();
    }

    public void showAddMomentDialog(AlbumCallback callback) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_edit_message, null);
        b.setView(v);
        ((TextView) v.findViewById(R.id.tvDialogTitle)).setText("Nuestro Álbum");
        v.findViewById(R.id.formatToolbar).setVisibility(View.GONE);
        v.findViewById(R.id.etDialogTitle).setVisibility(View.GONE);

        // Reiniciar estado de subidas diferidas para un nuevo momento.
        clearDeferredUploadTarget();

        EditText et = v.findViewById(R.id.etDialogMessage);
        et.setHint("Cuéntame algo sobre este momento...");
        
        RecyclerView rvPreview = v.findViewById(R.id.rvAlbumPreview);
        rvPreview.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(context, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        
        previewAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
                View pv = LayoutInflater.from(context).inflate(R.layout.item_album_preview, p, false);
                return new RecyclerView.ViewHolder(pv) {};
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
                String url = currentAlbumImages.get(pos);
                com.bumptech.glide.Glide.with(context).load(url).centerCrop().into((ImageView) h.itemView.findViewById(R.id.ivPreviewPhoto));
                h.itemView.findViewById(R.id.tvRemovePhoto).setOnClickListener(v1 -> {
                    currentAlbumImages.remove(pos);
                    notifyDataSetChanged();
                    rvPreview.setVisibility(currentAlbumImages.isEmpty() ? View.GONE : View.VISIBLE);
                });
            }
            @Override public int getItemCount() { return currentAlbumImages.size(); }
        };
        rvPreview.setAdapter(previewAdapter);
        rvPreview.setVisibility(currentAlbumImages.isEmpty() ? View.GONE : View.VISIBLE);

        if ("Pixel Oscuro".equals(currentTheme)) {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            ((TextView) v.findViewById(R.id.tvDialogTitle)).setTextColor(Color.WHITE);
            et.setTextColor(Color.WHITE);
            et.setHintTextColor(Color.LTGRAY);
            et.setBackgroundResource(R.drawable.bg_message_pixel_dark);
            ((Button) v.findViewById(R.id.btnSave)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnCancel)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnAddImage)).setTextColor(Color.WHITE);
        }

        currentAlbumImages.clear();
        v.findViewById(R.id.btnAddImage).setOnClickListener(v1 -> callback.onPickImage());

        AlertDialog dialog = b.create();
        final boolean[] saved = {false};
        activeSaveButton = v.findViewById(R.id.btnSave);
        updateSaveEnabled();
        v.findViewById(R.id.btnSave).setOnClickListener(v1 -> {
            String content = et.getText().toString().trim();
            if (content.isEmpty() && currentAlbumImages.isEmpty()) return;

            Message m = new Message(UUID.randomUUID().toString(), coupleId, userId, userName, userImageUri, "[ALBUM] " + content, new ArrayList<>(currentAlbumImages), System.currentTimeMillis(), false);
            db.collection("messages").document(m.getMessageId()).set(m).addOnSuccessListener(aVoid -> {
                saved[0] = true;
                setDeferredUploadTarget(m.getMessageId());
                callback.onMomentSaved();
                dialog.dismiss();
            });
        });
        v.findViewById(R.id.btnCancel).setOnClickListener(v1 -> dialog.dismiss());
        dialog.setOnDismissListener(d -> {
            activeSaveButton = null;
            if (!saved[0]) {
                clearDeferredUploadTarget();
            }
        });
        dialog.show();
    }

    public void showSharedAlbum(AlbumCallback callback) {
        AlertDialog.Builder b = new AlertDialog.Builder(context, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_shared_album, null);
        b.setView(v);

        Button btnAdd = v.findViewById(R.id.btnAddMoment);

        if ("Pixel Oscuro".equals(currentTheme)) {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            ((TextView) v.findViewById(R.id.tvAlbumTitle)).setTextColor(Color.WHITE);
            ((TextView) v.findViewById(R.id.tvAlbumSubtitle)).setTextColor(Color.LTGRAY);
            btnAdd.setTextColor(Color.WHITE);
            btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1A1A2E")));
        } else {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel);
            btnAdd.setTextColor(Color.WHITE);
            btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#8B4513")));
        }

        RecyclerView rv = v.findViewById(R.id.rvAlbumPhotos);
        List<Message> moments = new ArrayList<>();
        AlbumGalleryAdapter adp = new AlbumGalleryAdapter(moments, new AlbumGalleryAdapter.OnMomentClickListener() {
            @Override public void onMomentClick(Message m) { showAlbumDetail(m); }
            @Override public void onMomentLongClick(View v, Message m) {
                if (m.getAuthorId().equals(userId)) {
                    android.widget.PopupMenu p = new android.widget.PopupMenu(context, v);
                    p.getMenu().add("Editar Momento");
                    p.getMenu().add("Eliminar momento completo");
                    p.setOnMenuItemClickListener(menuItem -> {
                        String title = menuItem.getTitle().toString();
                        if (title.equals("Editar Momento")) {
                            showEditAlbumDialog(m);
                        } else if (title.equals("Eliminar momento completo")) {
                            db.collection("messages").document(m.getMessageId()).delete();
                        }
                        return true;
                    });
                    p.show();
                }
            }
        });
        rv.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(context, 3));
        rv.setAdapter(adp);

        db.collection("messages").whereEqualTo("partnerId", coupleId)
            .addSnapshotListener((shots, error) -> {
                if (shots != null) {
                    moments.clear();
                    for (QueryDocumentSnapshot doc : shots) {
                        Message m = doc.toObject(Message.class);
                        if (m.getContent() != null && m.getContent().startsWith("[ALBUM]")) moments.add(m);
                    }
                    moments.sort((m1, m2) -> Long.compare(m2.getTimestamp(), m1.getTimestamp()));
                    adp.notifyDataSetChanged();
                }
            });

        final AlertDialog d = b.create();
        
        d.setOnShowListener(dialog -> {
            if (d.getWindow() != null) {
                d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        });

        if (btnAdd != null) btnAdd.setOnClickListener(v1 -> {
             showAddMomentDialog(callback);
        });
        d.show();
    }

    public void showAlbumDetail(Message msg) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_message_detail, null);
        dialog.setContentView(v);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);
            
            // Aplicar flags inmersivos para pantalla completamente full-screen
            int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            dialog.getWindow().getDecorView().setSystemUiVisibility(flags);
        }

        Button btnEdit = v.findViewById(R.id.btnMessageDetailEdit);
        Button btnClose = v.findViewById(R.id.btnMessageDetailClose);

        if (msg.getAuthorId().equals(userId)) {
            btnEdit.setVisibility(View.VISIBLE);
        }

        if ("Pixel Oscuro".equals(currentTheme)) {
            v.setBackgroundColor(Color.parseColor("#0D0D2B"));
            ((TextView) v.findViewById(R.id.tvMessageDetailTitle)).setTextColor(Color.WHITE);
            ((TextView) v.findViewById(R.id.tvMessageDetailContent)).setTextColor(Color.WHITE);
            btnEdit.setTextColor(Color.WHITE);
            btnEdit.setBackgroundColor(Color.parseColor("#1A1A2E"));
            btnClose.setTextColor(Color.WHITE);
            btnClose.setBackgroundColor(Color.parseColor("#1A1A2E"));
        } else {
            v.setBackgroundColor(Color.parseColor("#F5F5F5"));
            btnEdit.setBackgroundColor(Color.parseColor("#5D2E7A"));
            btnClose.setBackgroundColor(Color.parseColor("#5D2E7A"));
        }

        TextView tvTitle = v.findViewById(R.id.tvMessageDetailTitle);
        tvTitle.setText("Detalle del Momento");

        TextView tvContent = v.findViewById(R.id.tvMessageDetailContent);
        String content = msg.getContent();
        tvContent.setText(content != null ? content.replace("[ALBUM] ", "") : "");

        ImageView ivMain = v.findViewById(R.id.ivMessageDetailImage);
        RecyclerView rvPhotos = v.findViewById(R.id.rvMessageDetailPhotos);

        List<String> urls = msg.getImageUrls();
        if (urls != null && !urls.isEmpty()) {
            ivMain.setVisibility(View.GONE);
            rvPhotos.setVisibility(View.VISIBLE);

            rvPhotos.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(context, 2));
            rvPhotos.setHasFixedSize(true);

            rvPhotos.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
                    View gv = LayoutInflater.from(context).inflate(R.layout.item_album_gallery, p, false);
                    return new RecyclerView.ViewHolder(gv) {};
                }
                @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
                    String url = urls.get(pos);
                    ImageView iv = h.itemView.findViewById(R.id.ivGalleryImage);

                    com.bumptech.glide.Glide.with(context)
                            .load(url)
                            .centerCrop()
                            .into(iv);

                    h.itemView.setOnClickListener(v1 -> showFullScreenImage(url));
                    h.itemView.setOnLongClickListener(v1 -> {
                        if (msg.getAuthorId().equals(userId)) {
                            new AlertDialog.Builder(context).setTitle("Eliminar foto").setMessage("¿Deseas eliminar esta foto de este momento?")
                                .setPositiveButton("Eliminar", (dia, wh) -> {
                                    urls.remove(pos);
                                    if (urls.isEmpty()) db.collection("messages").document(msg.getMessageId()).delete();
                                    else { msg.setImageUrls(urls); db.collection("messages").document(msg.getMessageId()).set(msg); }
                                    dialog.dismiss();
                                    showAlbumDetail(msg);
                                }).setNegativeButton("Cancelar", null).show();
                        }
                        return true;
                    });
                }
                @Override public int getItemCount() { return urls.size(); }
            });
        } else {
            rvPhotos.setVisibility(View.GONE);
            ivMain.setVisibility(View.GONE);
        }

        btnEdit.setOnClickListener(v1 -> {
            dialog.dismiss();
            showEditAlbumDialog(msg);
        });
        btnClose.setOnClickListener(v1 -> dialog.dismiss());
        dialog.show();
    }

    private void showFullScreenImage(String url) {
        android.app.Dialog d = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView iv = new ImageView(context);
        iv.setBackgroundColor(Color.BLACK);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        com.bumptech.glide.Glide.with(context)
                .load(url)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .into(iv);
                
        d.setContentView(iv);
        iv.setOnClickListener(v -> d.dismiss());
        d.show();
    }

    public void showEditAlbumDialog(Message m) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_edit_message, null);
        b.setView(v);
        ((TextView) v.findViewById(R.id.tvDialogTitle)).setText("Editar Momento");
        v.findViewById(R.id.formatToolbar).setVisibility(View.GONE);
        v.findViewById(R.id.etDialogTitle).setVisibility(View.GONE);

        // Reiniciar estado de subidas diferidas para esta edicion.
        clearDeferredUploadTarget();

        EditText et = v.findViewById(R.id.etDialogMessage);
        String editContent = m.getContent();
        et.setText(editContent != null ? editContent.replace("[ALBUM] ", "") : "");
        
        currentAlbumImages.clear();
        if (m.getImageUrls() != null) {
            currentAlbumImages.addAll(m.getImageUrls());
        }

        RecyclerView rvPreview = v.findViewById(R.id.rvAlbumPreview);
        rvPreview.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        
        previewAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
                View pv = LayoutInflater.from(context).inflate(R.layout.item_album_preview, p, false);
                return new RecyclerView.ViewHolder(pv) {};
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
                String url = currentAlbumImages.get(pos);
                com.bumptech.glide.Glide.with(context).load(url).centerCrop().into((ImageView) h.itemView.findViewById(R.id.ivPreviewPhoto));
                h.itemView.findViewById(R.id.tvRemovePhoto).setOnClickListener(v1 -> {
                    currentAlbumImages.remove(pos);
                    notifyDataSetChanged();
                    rvPreview.setVisibility(currentAlbumImages.isEmpty() ? View.GONE : View.VISIBLE);
                });
            }
            @Override public int getItemCount() { return currentAlbumImages.size(); }
        };
        rvPreview.setAdapter(previewAdapter);
        rvPreview.setVisibility(currentAlbumImages.isEmpty() ? View.GONE : View.VISIBLE);

        if ("Pixel Oscuro".equals(currentTheme)) {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            ((TextView) v.findViewById(R.id.tvDialogTitle)).setTextColor(Color.WHITE);
            et.setTextColor(Color.WHITE);
            et.setHintTextColor(Color.LTGRAY);
            et.setBackgroundResource(R.drawable.bg_message_pixel_dark);
            ((Button) v.findViewById(R.id.btnSave)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnCancel)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnAddImage)).setTextColor(Color.WHITE);
        }

        // Para añadir más fotos al editar, necesitamos el callback de pickImage
        // Como AlbumManager no tiene el callback aquí, asumimos que MainActivity lo maneja
        v.findViewById(R.id.btnAddImage).setOnClickListener(v1 -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).pickImage(4); // 4 = PICK_IMAGE_ALBUM
            }
        });

        final AlertDialog d = b.create();
        final boolean[] saved = {false};
        activeSaveButton = v.findViewById(R.id.btnSave);
        updateSaveEnabled();
        v.findViewById(R.id.btnSave).setOnClickListener(v1 -> {
            String content = et.getText().toString().trim();
            m.setContent("[ALBUM] " + content);
            m.setImageUrls(new ArrayList<>(currentAlbumImages));
            db.collection("messages").document(m.getMessageId()).set(m).addOnSuccessListener(aVoid -> {
                saved[0] = true;
                setDeferredUploadTarget(m.getMessageId());
                d.dismiss();
            });
        });
        v.findViewById(R.id.btnCancel).setOnClickListener(v1 -> d.dismiss());
        d.setOnDismissListener(dialog -> {
            activeSaveButton = null;
            if (!saved[0]) {
                clearDeferredUploadTarget();
            }
        });
        d.show();
    }

    public void addImageUrl(String url) {
        currentAlbumImages.add(url);
        if (previewAdapter != null) {
            previewAdapter.notifyDataSetChanged();
        }
        if (allowDeferredUploads && deferredMessageId != null) {
            db.collection("messages").document(deferredMessageId)
                .update("imageUrls", FieldValue.arrayUnion(url));
        }
    }

    public void setTheme(String theme) {
        this.currentTheme = theme;
    }

    public void onAlbumUploadStarted() {
        pendingAlbumUploads++;
        updateSaveEnabled();
    }

    public void onAlbumUploadFinished() {
        if (pendingAlbumUploads > 0) pendingAlbumUploads--;
        updateSaveEnabled();
    }

    private void updateSaveEnabled() {
        if (activeSaveButton == null) return;
        boolean enabled = pendingAlbumUploads == 0;
        activeSaveButton.setEnabled(enabled);
        activeSaveButton.setAlpha(enabled ? 1f : 0.5f);
    }

    private void setDeferredUploadTarget(String messageId) {
        this.deferredMessageId = messageId;
        this.allowDeferredUploads = true;
    }

    private void clearDeferredUploadTarget() {
        this.deferredMessageId = null;
        this.allowDeferredUploads = false;
    }
}

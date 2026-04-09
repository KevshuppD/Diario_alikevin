package calendario.kevshupp.diariokevinali;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
        AlertDialog dialog = b.create();

        v.findViewById(R.id.btnOptionAdd).setOnClickListener(v1 -> {
            dialog.dismiss();
            showAddMomentDialog(callback);
        });
        v.findViewById(R.id.btnOptionView).setOnClickListener(v1 -> {
            dialog.dismiss();
            showSharedAlbum();
        });
        v.findViewById(R.id.btnOptionCancel).setOnClickListener(v1 -> dialog.dismiss());
        dialog.show();
    }

    private void showAddMomentDialog(AlbumCallback callback) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_edit_message, null);
        b.setView(v);
        ((TextView) v.findViewById(R.id.tvDialogTitle)).setText("Nuestro Álbum");
        v.findViewById(R.id.formatToolbar).setVisibility(View.GONE);
        
        EditText et = v.findViewById(R.id.etDialogMessage);
        
        currentAlbumImages.clear();
        
        v.findViewById(R.id.btnAddImage).setOnClickListener(v1 -> callback.onPickImage());

        AlertDialog dialog = b.create();
        v.findViewById(R.id.btnSave).setOnClickListener(v1 -> {
            String content = et.getText().toString().trim();
            if (content.isEmpty() && currentAlbumImages.isEmpty()) return;
            
            Message m = new Message(UUID.randomUUID().toString(), coupleId, userId, userName, userImageUri, "[ALBUM] " + content, new ArrayList<>(currentAlbumImages), System.currentTimeMillis(), false);
            db.collection("messages").document(m.getMessageId()).set(m).addOnSuccessListener(aVoid -> {
                callback.onMomentSaved();
                dialog.dismiss();
            });
        });
        v.findViewById(R.id.btnCancel).setOnClickListener(v1 -> dialog.dismiss());
        dialog.show();
    }

    public void showSharedAlbum() {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_shared_album, null);
        b.setView(v);
        RecyclerView rv = v.findViewById(R.id.rvAlbumPhotos);
        List<Message> moments = new ArrayList<>();
        MessageAdapter adp = new MessageAdapter(moments, userId, new MessageAdapter.OnMessageClickListener() {
            @Override public void onMessageClick(View v, Message msg) { showAlbumDetail(msg); }
            @Override public void onMessageLongClick(View v, Message msg) {
                if (msg.getAuthorId().equals(userId)) {
                    android.widget.PopupMenu p = new android.widget.PopupMenu(context, v);
                    p.getMenu().add("Eliminar");
                    p.setOnMenuItemClickListener(item -> {
                        db.collection("messages").document(msg.getMessageId()).delete();
                        return true;
                    });
                    p.show();
                }
            }
            @Override public void onDeleteClick(Message m) { db.collection("messages").document(m.getMessageId()).delete(); }
        });
        rv.setLayoutManager(new LinearLayoutManager(context));
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
        v.findViewById(R.id.btnCloseAlbum).setOnClickListener(v1 -> d.dismiss());
        d.show();
    }

    public void showAlbumDetail(Message msg) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_message_detail, null);
        b.setView(v);
        
        TextView tvTitle = v.findViewById(R.id.tvMessageDetailTitle);
        tvTitle.setText("Detalle del Momento");
        
        TextView tvContent = v.findViewById(R.id.tvMessageDetailContent);
        tvContent.setText(msg.getContent().replace("[ALBUM] ", ""));
        
        // El álbum suele tener varias imágenes, pero para el detalle mostramos la primera o implementamos un carrusel
        // Por ahora, usaremos el mismo layout de detalle que las cartas si hay imagen
        if (msg.getImageUrls() != null && !msg.getImageUrls().isEmpty()) {
            android.widget.ImageView iv = v.findViewById(R.id.ivMessageDetailImage);
            iv.setVisibility(View.VISIBLE);
            com.bumptech.glide.Glide.with(context).load(msg.getImageUrls().get(0)).into(iv);
        }

        final AlertDialog d = b.create();
        v.findViewById(R.id.btnMessageDetailClose).setOnClickListener(v1 -> d.dismiss());
        d.show();
    }

    public void showEditAlbumDialog(Message m) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_edit_message, null);
        b.setView(v);
        ((TextView) v.findViewById(R.id.tvDialogTitle)).setText("Editar Momento");
        v.findViewById(R.id.formatToolbar).setVisibility(View.GONE);
        EditText et = v.findViewById(R.id.etDialogMessage);
        et.setText(m.getContent().replace("[ALBUM] ", ""));
        final AlertDialog d = b.create();
        v.findViewById(R.id.btnSave).setOnClickListener(v1 -> {
            m.setContent("[ALBUM] " + et.getText().toString());
            db.collection("messages").document(m.getMessageId()).set(m).addOnSuccessListener(aVoid -> d.dismiss());
        });
        v.findViewById(R.id.btnCancel).setOnClickListener(v1 -> d.dismiss());
        d.show();
    }

    public void addImageUrl(String url) {
        currentAlbumImages.add(url);
    }
}

package calendario.kevshupp.diariokevinali;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class MessageEditor {
    private final Context context;
    private final String coupleId;
    private final String userId;
    private final String userName;
    private final String userImageUri;
    private String currentSelectedImageUrl = null;
    
    public interface EditorCallback {
        void onSave(Message message);
        void onPickImage(int code);
    }

    public MessageEditor(Context context, String coupleId, String userId, String userName, String userImageUri) {
        this.context = context;
        this.coupleId = coupleId;
        this.userId = userId;
        this.userName = userName;
        this.userImageUri = userImageUri;
    }

    public void showEditDialog(@Nullable Message edit, EditorCallback callback) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_edit_message, null);
        b.setView(v);
        
        EditText et = v.findViewById(R.id.etDialogMessage);
        ImageView ivSelectedImage = v.findViewById(R.id.ivSelectedImage);
        LinearLayout imageContainer = v.findViewById(R.id.imageContainer);

        if (edit != null) {
            et.setText(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? 
                Html.fromHtml(edit.getContent(), Html.FROM_HTML_MODE_COMPACT) : 
                Html.fromHtml(edit.getContent()));
            currentSelectedImageUrl = edit.getImageUrl();
            if (currentSelectedImageUrl != null) {
                imageContainer.setVisibility(View.VISIBLE);
                Glide.with(context).load(currentSelectedImageUrl).into(ivSelectedImage);
            }
        }

        v.findViewById(R.id.btnBold).setOnClickListener(v1 -> applySpan(et, StyleSpan.class, new StyleSpan(Typeface.BOLD)));
        v.findViewById(R.id.btnItalic).setOnClickListener(v1 -> applySpan(et, StyleSpan.class, new StyleSpan(Typeface.ITALIC)));
        v.findViewById(R.id.btnColor).setOnClickListener(v1 -> {
            int[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.BLACK, Color.GRAY};
            String[] names = {"Rojo", "Azul", "Verde", "Rosa", "Negro", "Gris"};
            new AlertDialog.Builder(context).setItems(names, (d, w) -> applySpan(et, ForegroundColorSpan.class, new ForegroundColorSpan(colors[w]))).show();
        });
        
        v.findViewById(R.id.btnAddImage).setOnClickListener(v1 -> callback.onPickImage(3)); // Code for Carta
        v.findViewById(R.id.btnRemoveImage).setOnClickListener(v1 -> {
            currentSelectedImageUrl = null;
            imageContainer.setVisibility(View.GONE);
        });

        AlertDialog dialog = b.create();
        v.findViewById(R.id.btnCancel).setOnClickListener(v1 -> dialog.dismiss());
        v.findViewById(R.id.btnSave).setOnClickListener(v1 -> {
            String html = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? 
                Html.toHtml(et.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE) : 
                Html.toHtml(et.getText());
            
            Message m = edit != null ? edit : new Message(UUID.randomUUID().toString(), coupleId, userId, userName, userImageUri, html, new ArrayList<>(), System.currentTimeMillis(), false);
            if (edit != null) m.setContent(html);
            m.setImageUrl(currentSelectedImageUrl);
            callback.onSave(m);
            dialog.dismiss();
        });
        dialog.show();
    }

    public void showMessageDetail(Message msg) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_message_detail, null);
        b.setView(v);
        
        TextView tv = v.findViewById(R.id.tvMessageDetailContent);
        tv.setText(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? 
            Html.fromHtml(msg.getContent(), Html.FROM_HTML_MODE_COMPACT) : 
            Html.fromHtml(msg.getContent()));
            
        ImageView iv = v.findViewById(R.id.ivMessageDetailImage);
        if (msg.getImageUrl() != null) {
            iv.setVisibility(View.VISIBLE);
            Glide.with(context).load(msg.getImageUrl()).into(iv);
        }
        
        final AlertDialog d = b.create();
        v.findViewById(R.id.btnMessageDetailClose).setOnClickListener(v1 -> d.dismiss());
        d.show();
    }

    private void applySpan(EditText et, Class<?> spanClass, Object newSpan) {
        int start = et.getSelectionStart();
        int end = et.getSelectionEnd();
        if (start == -1 || end == -1 || start == end) return;
        android.text.Editable editable = et.getText();
        Object[] existing = editable.getSpans(start, end, spanClass);
        for (Object s : existing) editable.removeSpan(s);
        editable.setSpan(newSpan, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public void setImageUrl(String url) {
        this.currentSelectedImageUrl = url;
    }

    public void uploadImage(Uri uri, UploadCallback callback) {
        MediaManager.get().upload(uri)
            .callback(callback)
            .dispatch();
    }
}

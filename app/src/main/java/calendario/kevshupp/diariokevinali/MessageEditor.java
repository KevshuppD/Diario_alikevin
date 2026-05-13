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
import android.widget.Button;
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
    private String currentTheme = "Pixel Claro";
    
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
        currentDialogView = v;
        b.setView(v);

        if ("Pixel Oscuro".equals(currentTheme)) {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            ((TextView) v.findViewById(R.id.tvDialogTitle)).setTextColor(Color.WHITE);
            EditText et1 = v.findViewById(R.id.etDialogTitle); et1.setTextColor(Color.WHITE); et1.setHintTextColor(Color.LTGRAY);
            EditText et2 = v.findViewById(R.id.etDialogMessage); et2.setTextColor(Color.WHITE); et2.setHintTextColor(Color.LTGRAY);
            et2.setBackgroundResource(R.drawable.bg_message_pixel_dark);
            ((Button) v.findViewById(R.id.btnSave)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnCancel)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnAddImage)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnBold)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnItalic)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnColor)).setTextColor(Color.WHITE);
        }
        
        EditText et = v.findViewById(R.id.etDialogMessage);
        EditText etTitle = v.findViewById(R.id.etDialogTitle);
        ImageView ivSelectedImage = v.findViewById(R.id.ivSelectedImage);
        View imageContainer = v.findViewById(R.id.imageContainer);

        if (edit != null) {
            if (edit.getTitle() != null) etTitle.setText(edit.getTitle());
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
        dialog.setOnDismissListener(d -> currentDialogView = null);
        v.findViewById(R.id.btnCancel).setOnClickListener(v1 -> dialog.dismiss());
        v.findViewById(R.id.btnSave).setOnClickListener(v1 -> {
            String html = "";
            if (et.getText() != null) {
                html = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? 
                    Html.toHtml(et.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE) : 
                    Html.toHtml(et.getText());
            }
            
            Message m = edit != null ? edit : new Message(UUID.randomUUID().toString(), coupleId, userId, userName, userImageUri, html, new ArrayList<>(), System.currentTimeMillis(), false);
            if (etTitle.getText() != null) m.setTitle(etTitle.getText().toString());
            if (edit != null) m.setContent(html);
            m.setImageUrl(currentSelectedImageUrl);
            callback.onSave(m);
            dialog.dismiss();
        });
        dialog.show();
    }

    public void showMessageDetail(Message msg) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_view_message, null);
        dialog.setContentView(v);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);
            
            // Aplicar flags inmersivos
            int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            dialog.getWindow().getDecorView().setSystemUiVisibility(flags);
        }

        Button btnClose = v.findViewById(R.id.btnClose);
        
        if ("Pixel Oscuro".equals(currentTheme)) {
            v.setBackgroundColor(Color.parseColor("#0D0D2B"));
            ((TextView) v.findViewById(R.id.tvViewAuthor)).setTextColor(Color.WHITE);
            ((TextView) v.findViewById(R.id.tvViewTimestamp)).setTextColor(Color.WHITE);
            ((TextView) v.findViewById(R.id.tvViewContent)).setTextColor(Color.WHITE);
            btnClose.setTextColor(Color.WHITE);
            btnClose.setBackgroundColor(Color.parseColor("#1A1A2E"));
        } else {
            v.setBackgroundColor(Color.parseColor("#F5F5F5"));
            btnClose.setTextColor(Color.WHITE);
            btnClose.setBackgroundColor(Color.parseColor("#5D2E7A"));
        }
        
        TextView tvAuthor = v.findViewById(R.id.tvViewAuthor);
        TextView tvTimestamp = v.findViewById(R.id.tvViewTimestamp);
        tvAuthor.setText("De: " + msg.getAuthorName());
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
        tvTimestamp.setText(sdf.format(new java.util.Date(msg.getTimestamp())));
        
        TextView tv = v.findViewById(R.id.tvViewContent);
        tv.setText(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? 
            Html.fromHtml(msg.getContent(), Html.FROM_HTML_MODE_COMPACT) : 
            Html.fromHtml(msg.getContent()));
            
        ImageView iv = v.findViewById(R.id.ivViewImage);
        if (msg.getImageUrl() != null) {
            iv.setVisibility(View.VISIBLE);
            Glide.with(context).load(msg.getImageUrl()).centerCrop().into(iv);
        }
        
        btnClose.setOnClickListener(v1 -> dialog.dismiss());
        dialog.show();
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
        // Si el diálogo está abierto, queremos que la imagen se vea inmediatamente
        if (currentDialogView != null) {
            ImageView ivSelectedImage = currentDialogView.findViewById(R.id.ivSelectedImage);
            View imageContainer = currentDialogView.findViewById(R.id.imageContainer);
            if (ivSelectedImage != null && imageContainer != null) {
                imageContainer.setVisibility(View.VISIBLE);
                Glide.with(context).load(url).into(ivSelectedImage);
            }
        }
    }

    private View currentDialogView = null;

    public void setTheme(String theme) {
        this.currentTheme = theme;
    }

    public void uploadImage(Uri uri, UploadCallback callback) {
        MediaManager.get().upload(uri)
            .callback(callback)
            .dispatch();
    }
}

package calendario.kevshupp.diariokevinali;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private String currentUserId, currentUserName, currentUserImageUri, theme;
    private FirebaseFirestore db;

    public static ProfileFragment newInstance(String userId, String userName, String userImageUri, String theme) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putString("userName", userName);
        args.putString("userImageUri", userImageUri);
        args.putString("theme", theme);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentUserId = getArguments().getString("userId");
            currentUserName = getArguments().getString("userName");
            currentUserImageUri = getArguments().getString("userImageUri");
            theme = getArguments().getString("theme");
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_profile, container, false);

        if ("Pixel Oscuro".equals(theme)) {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            ((TextView) v.findViewById(R.id.tvProfileTitle)).setTextColor(Color.WHITE);
            ((TextView) v.findViewById(R.id.tvCurrentUserName)).setTextColor(Color.WHITE);
            TextView tvTogether = v.findViewById(R.id.tvTogetherTime);
            tvTogether.setTextColor(Color.parseColor("#FF80AB")); // Rosa más claro para modo oscuro
            tvTogether.setBackgroundResource(R.drawable.bg_message_pixel_dark);
            ((TextView) v.findViewById(R.id.tvProfileInfo)).setTextColor(Color.LTGRAY);
            
            v.findViewById(R.id.ivProfileImage).setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            
            Button btnSave = v.findViewById(R.id.btnSaveProfile);
            btnSave.setTextColor(Color.WHITE);
            btnSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1A1A2E")));
            
            Button btnLogout = v.findViewById(R.id.btnLogoutProfile);
            btnLogout.setTextColor(Color.WHITE);
            btnLogout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D81B60")));
        } else {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel);
        }

        ImageView ivProfile = v.findViewById(R.id.ivProfileImage);
        TextView tvName = v.findViewById(R.id.tvCurrentUserName), tvTime = v.findViewById(R.id.tvTogetherTime);
        tvName.setText("Usuario: " + currentUserName); 
        tvTime.setText(calcRelationshipTime(2022, 1, 19));
        
        if (currentUserImageUri != null) Glide.with(this).load(currentUserImageUri).circleCrop().into(ivProfile);
        
        ivProfile.setOnClickListener(v1 -> {
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).pickImage(1); // PICK_IMAGE_PROFILE
        });

        v.findViewById(R.id.btnSaveProfile).setOnClickListener(v1 -> {
            SharedPreferences.Editor e = requireActivity().getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE).edit();
            e.putString("userImage", currentUserImageUri).apply();
            db.collection("users").document(currentUserId).update("profileImageUrl", currentUserImageUri)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show());
        });

        v.findViewById(R.id.btnLogoutProfile).setOnClickListener(v1 -> {
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).logout();
        });

        // Añadir botón de volver si no existe en el layout de diálogo original
        // o simplemente usar el botón atrás del dispositivo.
        
        return v;
    }

    private String calcRelationshipTime(int y, int m, int d) {
        Calendar start = Calendar.getInstance(); start.set(y, m - 1, d); Calendar now = Calendar.getInstance();
        int years = now.get(Calendar.YEAR) - start.get(Calendar.YEAR), months = now.get(Calendar.MONTH) - start.get(Calendar.MONTH), days = now.get(Calendar.DAY_OF_MONTH) - start.get(Calendar.DAY_OF_MONTH);
        if (days < 0) { months--; days += now.getActualMaximum(Calendar.DAY_OF_MONTH); }
        if (months < 0) { years--; months += 12; }
        return String.format(Locale.getDefault(), "Juntos: %d año(s), %d mes(es), %d día(s)", years, months, days);
    }
    
    public void setProfileImage(String url) {
        this.currentUserImageUri = url;
        if (getView() != null) {
            ImageView ivProfile = getView().findViewById(R.id.ivProfileImage);
            Glide.with(this).load(url).circleCrop().into(ivProfile);
        }
    }
}

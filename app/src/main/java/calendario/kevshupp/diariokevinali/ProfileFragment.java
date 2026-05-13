package calendario.kevshupp.diariokevinali;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private String currentUserId, currentUserName, currentUserImageUri, theme, coupleId;
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

    public static ProfileFragment newInstance(String userId, String userName, String userImageUri, String theme, String coupleId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putString("userName", userName);
        args.putString("userImageUri", userImageUri);
        args.putString("theme", theme);
        args.putString("coupleId", coupleId);
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
            coupleId = getArguments().getString("coupleId");
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
            tvTogether.setTextColor(Color.parseColor("#FF80AB"));
            tvTogether.setBackgroundResource(R.drawable.bg_message_pixel_dark);
            ((TextView) v.findViewById(R.id.tvProfileInfo)).setTextColor(Color.LTGRAY);
            
            v.findViewById(R.id.ivProfileImage).setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            v.findViewById(R.id.ivPartnerImage).setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            ((TextView) v.findViewById(R.id.tvPartnerName)).setTextColor(Color.parseColor("#FF80AB"));
            v.findViewById(R.id.dividerPartner).setBackgroundColor(Color.parseColor("#444466"));
            
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
        ImageView ivPartner = v.findViewById(R.id.ivPartnerImage);
        TextView tvName = v.findViewById(R.id.tvCurrentUserName), tvTime = v.findViewById(R.id.tvTogetherTime);
        TextView tvPartnerName = v.findViewById(R.id.tvPartnerName);
        
        tvName.setText(currentUserName);
        tvTime.setText(calcRelationshipTime(2022, 1, 19));
        
        if (currentUserImageUri != null) {
            Glide.with(this).load(currentUserImageUri).circleCrop().into(ivProfile);
        }

        // Cargar datos de la pareja
        db.collection("users").whereEqualTo("coupleId", coupleId).addSnapshotListener((snapshot, error) -> {
            if (snapshot != null) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                    String userId = doc.getId();
                    // Buscamos al usuario que NO sea el actual para mostrarlo como pareja
                    if (!userId.equals(currentUserId)) {
                        String pName = doc.getString("userName");
                        String pUrl = doc.getString("profileImageUrl");
                        
                        // Si es Ali, nos aseguramos que se vea bien el nombre
                        if (pName != null) {
                            tvPartnerName.setText(pName);
                        } else if (userId.equals("user_ali_02")) {
                            tvPartnerName.setText("Ali");
                        }
                        
                        if (pUrl != null && isAdded()) {
                            Glide.with(this).load(pUrl).circleCrop().into(ivPartner);
                        }
                        break;
                    }
                }
            }
        });

        ivProfile.setOnClickListener(v1 -> {
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).pickImage(1); // PICK_IMAGE_PROFILE
        });

        v.findViewById(R.id.btnSaveProfile).setOnClickListener(v1 -> {
            db.collection("users").document(currentUserId).update("profileImageUrl", currentUserImageUri)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        requireActivity().getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE).edit().putString("userImage", currentUserImageUri).apply();
                        android.widget.Toast.makeText(getContext(), "Perfil actualizado", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
        });

        v.findViewById(R.id.btnLogoutProfile).setOnClickListener(v1 -> {
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).logout();
        });

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

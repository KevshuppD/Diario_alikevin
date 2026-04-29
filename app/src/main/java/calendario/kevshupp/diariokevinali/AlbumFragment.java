package calendario.kevshupp.diariokevinali;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AlbumFragment extends Fragment {

    private String coupleId, userId, userName, userImageUri, theme;
    private FirebaseFirestore db;
    private AlbumGalleryAdapter adapter;
    private List<Message> moments = new ArrayList<>();
    private AlbumManager albumManager;

    public static AlbumFragment newInstance(String coupleId, String userId, String userName, String userImageUri, String theme) {
        AlbumFragment fragment = new AlbumFragment();
        Bundle args = new Bundle();
        args.putString("coupleId", coupleId);
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
            coupleId = getArguments().getString("coupleId");
            userId = getArguments().getString("userId");
            userName = getArguments().getString("userName");
            userImageUri = getArguments().getString("userImageUri");
            theme = getArguments().getString("theme");
        }
        db = FirebaseFirestore.getInstance();
        albumManager = new AlbumManager(requireContext(), coupleId, userId, userName, userImageUri);
        albumManager.setTheme(theme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_shared_album, container, false);

        if ("Pixel Oscuro".equals(theme)) {
            view.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            ((TextView) view.findViewById(R.id.tvAlbumTitle)).setTextColor(Color.WHITE);
            ((TextView) view.findViewById(R.id.tvAlbumSubtitle)).setTextColor(Color.LTGRAY);
            view.findViewById(R.id.llAlbumHeader).setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            Button btnAddMoment = view.findViewById(R.id.btnAddMoment);
            btnAddMoment.setTextColor(Color.WHITE);
            btnAddMoment.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1A1A2E")));
        } else {
            view.setBackgroundResource(R.drawable.bg_parchment_pixel);
        }

        Button btnAdd = view.findViewById(R.id.btnAddMoment);
        btnAdd.setOnClickListener(v -> albumManager.showAddMomentDialog(new AlbumManager.AlbumCallback() {
            @Override public void onPickImage() { 
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).pickImage(4); 
            }
            @Override public void onMomentSaved() {
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).sendNotificationV1("Nuevo momento en el álbum 📸");
            }
        }));

        RecyclerView rv = view.findViewById(R.id.rvAlbumPhotos);
        adapter = new AlbumGalleryAdapter(moments, new AlbumGalleryAdapter.OnMomentClickListener() {
            @Override public void onMomentClick(Message m) {
                albumManager.showAlbumDetail(m);
            }

            @Override public void onMomentLongClick(View v, Message m) {
                if (m.getAuthorId().equals(userId)) {
                    PopupMenu p = new PopupMenu(getContext(), v);
                    p.getMenu().add("Editar Momento");
                    p.getMenu().add("Eliminar momento completo");
                    p.setOnMenuItemClickListener(menuItem -> {
                        String title = menuItem.getTitle().toString();
                        if (title.equals("Editar Momento")) {
                            albumManager.showEditAlbumDialog(m);
                        } else if (title.equals("Eliminar momento completo")) {
                            db.collection("messages").document(m.getMessageId()).delete();
                        }
                        return true;
                    });
                    p.show();
                }
            }
        });
        adapter.setTheme(theme);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 4));
        rv.setAdapter(adapter);

        listenAlbum();

        return view;
    }

    private void listenAlbum() {
        db.collection("messages").whereEqualTo("partnerId", coupleId)
            .addSnapshotListener((shots, error) -> {
                if (shots != null) {
                    moments.clear();
                    for (QueryDocumentSnapshot doc : shots) {
                        Message m = doc.toObject(Message.class);
                        if (m.getContent() != null && m.getContent().startsWith("[ALBUM]")) moments.add(m);
                    }
                    moments.sort((m1, m2) -> Long.compare(m2.getTimestamp(), m1.getTimestamp()));
                    adapter.notifyDataSetChanged();
                }
            });
    }

    public void addImageUrl(String url) {
        if (albumManager != null) albumManager.addImageUrl(url);
    }
}

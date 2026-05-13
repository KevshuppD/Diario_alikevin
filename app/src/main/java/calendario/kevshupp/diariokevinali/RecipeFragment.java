package calendario.kevshupp.diariokevinali;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RecipeFragment extends Fragment {

    private String coupleId, userId, userName, theme;
    private FirebaseFirestore db;
    private RecipeAdapter adapter;
    private List<Recipe> recipes = new ArrayList<>();
    private RecipeManager recipeManager; // Reutilizamos para los diálogos de añadir/editar

    public static RecipeFragment newInstance(String coupleId, String userId, String userName, String theme) {
        RecipeFragment fragment = new RecipeFragment();
        Bundle args = new Bundle();
        args.putString("coupleId", coupleId);
        args.putString("userId", userId);
        args.putString("userName", userName);
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
            theme = getArguments().getString("theme");
        }
        db = FirebaseFirestore.getInstance();
        recipeManager = new RecipeManager(requireContext(), coupleId, userId, userName, () -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).pickImage(5); // PICK_IMAGE_RECIPE
            }
        });
        recipeManager.setTheme(theme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_recipe_list, container, false);

        TextView tvTitle = view.findViewById(R.id.tvRecipeListTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvRecipeListSubtitle);
        Button btnAdd = view.findViewById(R.id.btnAddRecipe);

        RecyclerView rvRecipes = view.findViewById(R.id.rvRecipes);
        adapter = new RecipeAdapter(recipes, userId, new RecipeAdapter.RecipeActionListener() {
            @Override public void onRecipeClick(Recipe recipe) { recipeManager.showRecipeDetail(recipe); }
            @Override public void onEditClick(Recipe recipe) { 
                recipeManager.showAddOrEditRecipeDialog(recipe);
            }
            @Override public void onDeleteClick(Recipe recipe) {
                db.collection("recipes").document(recipe.getRecipeId()).delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Receta eliminada", Toast.LENGTH_SHORT).show());
            }
        });
        adapter.setTheme(theme);
        rvRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecipes.setAdapter(adapter);

        if ("Pixel Oscuro".equals(theme)) {
            view.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            tvTitle.setTextColor(Color.WHITE);
            tvSubtitle.setTextColor(Color.LTGRAY);
            btnAdd.setTextColor(Color.WHITE);
            btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1A1A2E")));
        } else {
            view.setBackgroundResource(R.drawable.bg_parchment_pixel);
        }

        listenRecipes();

        btnAdd.setOnClickListener(v -> recipeManager.showAddOrEditRecipeDialog(null));

        return view;
    }

    private void listenRecipes() {
        db.collection("recipes")
                .whereEqualTo("coupleId", coupleId)
                .addSnapshotListener((shots, error) -> {
                    if (shots == null) return;
                    recipes.clear();
                    for (QueryDocumentSnapshot doc : shots) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe.getRecipeId() == null) recipe.setRecipeId(doc.getId());
                        recipes.add(recipe);
                    }
                    recipes.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                    adapter.notifyDataSetChanged();
                });
    }
    
    public void setImageUrl(String url) {
        if (recipeManager != null) recipeManager.setImageUrl(url);
    }
}

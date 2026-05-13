package calendario.kevshupp.diariokevinali;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecipeManager {

    public interface RecipeCallback {
        void onPickImage();
    }

    private final Context context;
    private final FirebaseFirestore db;
    private final String coupleId;
    private final String userId;
    private final String userName;
    private final RecipeCallback pickImageCallback;

    private String currentTheme = "Pixel Claro";
    private String currentSelectedImageUrl;
    private View currentEditorView;

    public RecipeManager(Context context, String coupleId, String userId, String userName, RecipeCallback callback) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.coupleId = coupleId;
        this.userId = userId;
        this.userName = userName;
        this.pickImageCallback = callback;
    }

    public void setTheme(String theme) {
        this.currentTheme = theme;
    }

    public void showRecipeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_recipe_list, null);
        builder.setView(view);

        TextView tvTitle = view.findViewById(R.id.tvRecipeListTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvRecipeListSubtitle);
        Button btnAdd = view.findViewById(R.id.btnAddRecipe);

        RecyclerView rvRecipes = view.findViewById(R.id.rvRecipes);
        List<Recipe> recipes = new ArrayList<>();
        RecipeAdapter adapter = new RecipeAdapter(recipes, userId, new RecipeAdapter.RecipeActionListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                showRecipeDetail(recipe);
            }

            @Override
            public void onEditClick(Recipe recipe) {
                showAddOrEditRecipeDialog(recipe);
            }

            @Override
            public void onDeleteClick(Recipe recipe) {
                String recipeId = recipe.getRecipeId();
                if (recipeId == null || recipeId.trim().isEmpty()) {
                    Toast.makeText(context, "No se pudo borrar: receta sin ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                db.collection("recipes").document(recipeId).delete()
                        .addOnSuccessListener(aVoid -> Toast.makeText(context, "Receta eliminada", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(context, "Error al borrar receta", Toast.LENGTH_SHORT).show());
            }
        });
        adapter.setTheme(currentTheme);

        rvRecipes.setLayoutManager(new LinearLayoutManager(context));
        rvRecipes.setAdapter(adapter);

        if ("Pixel Oscuro".equals(currentTheme)) {
            view.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            tvTitle.setTextColor(Color.WHITE);
            tvSubtitle.setTextColor(Color.LTGRAY);
            btnAdd.setTextColor(Color.WHITE);
        }

        db.collection("recipes")
                .whereEqualTo("coupleId", coupleId)
                .addSnapshotListener((shots, error) -> {
                    if (error != null) {
                        android.util.Log.e("RecipeManager", "Error en listener de recetas", error);
                        return;
                    }
                    if (shots == null) return;
                    recipes.clear();
                    for (QueryDocumentSnapshot doc : shots) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe.getRecipeId() == null || recipe.getRecipeId().trim().isEmpty()) {
                            recipe.setRecipeId(doc.getId());
                        }
                        recipes.add(recipe);
                    }
                    // Ordenar por timestamp descendente manualmente para evitar falta de índice en Firestore
                    recipes.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                    adapter.notifyDataSetChanged();
                });

        AlertDialog dialog = builder.create();
        btnAdd.setOnClickListener(v -> showAddOrEditRecipeDialog(null));
        dialog.show();
    }

    public void showAddOrEditRecipeDialog(@Nullable Recipe edit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_recipe, null);
        builder.setView(view);
        currentEditorView = view;

        TextView tvTitle = view.findViewById(R.id.tvRecipeEditorTitle);
        EditText etTitle = view.findViewById(R.id.etRecipeTitle);
        EditText etIngredients = view.findViewById(R.id.etRecipeIngredients);
        EditText etSteps = view.findViewById(R.id.etRecipeSteps);
        ImageView ivPreview = view.findViewById(R.id.ivRecipePreview);
        View previewContainer = view.findViewById(R.id.recipePreviewContainer);
        Button btnAddImage = view.findViewById(R.id.btnRecipeAddImage);
        Button btnSave = view.findViewById(R.id.btnSaveRecipe);
        Button btnCancel = view.findViewById(R.id.btnCancelRecipe);
        android.widget.ImageButton btnRemoveImage = view.findViewById(R.id.btnRemoveRecipeImage);

        if ("Pixel Oscuro".equals(currentTheme)) {
            view.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            tvTitle.setTextColor(Color.WHITE);
            etTitle.setTextColor(Color.WHITE);
            etTitle.setHintTextColor(Color.LTGRAY);
            etIngredients.setTextColor(Color.WHITE);
            etIngredients.setHintTextColor(Color.LTGRAY);
            etSteps.setTextColor(Color.WHITE);
            etSteps.setHintTextColor(Color.LTGRAY);
            etTitle.setBackgroundResource(R.drawable.bg_message_pixel_dark);
            etIngredients.setBackgroundResource(R.drawable.bg_message_pixel_dark);
            etSteps.setBackgroundResource(R.drawable.bg_message_pixel_dark);
            btnAddImage.setTextColor(Color.WHITE);
            btnSave.setTextColor(Color.WHITE);
            btnCancel.setTextColor(Color.WHITE);
        }

        if (edit != null) {
            tvTitle.setText("Editar receta");
            etTitle.setText(edit.getTitle());
            etIngredients.setText(edit.getIngredients());
            etSteps.setText(edit.getSteps());
            currentSelectedImageUrl = edit.getImageUrl();
            if (currentSelectedImageUrl != null && !currentSelectedImageUrl.trim().isEmpty()) {
                previewContainer.setVisibility(View.VISIBLE);
                Glide.with(context).load(currentSelectedImageUrl).into(ivPreview);
            }
        } else {
            currentSelectedImageUrl = null;
            previewContainer.setVisibility(View.GONE);
        }

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> currentEditorView = null);

        btnAddImage.setOnClickListener(v -> {
            if (pickImageCallback != null) pickImageCallback.onPickImage();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRemoveImage.setOnClickListener(v -> {
            currentSelectedImageUrl = null;
            previewContainer.setVisibility(View.GONE);
        });

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String ingredients = etIngredients.getText().toString().trim();
            String steps = etSteps.getText().toString().trim();

            if (title.isEmpty() || ingredients.isEmpty() || steps.isEmpty()) {
                Toast.makeText(context, "Completa título, ingredientes y pasos", Toast.LENGTH_SHORT).show();
                return;
            }

            String recipeId = edit != null ? edit.getRecipeId() : UUID.randomUUID().toString();
            Recipe recipe = new Recipe(
                    recipeId,
                    coupleId,
                    title,
                    ingredients,
                    steps,
                    currentSelectedImageUrl,
                    userId,
                    userName,
                    System.currentTimeMillis()
            );

            db.collection("recipes").document(recipeId).set(recipe).addOnSuccessListener(aVoid -> {
                if (context instanceof MainActivity) {
                    ((MainActivity) context).sendNotificationV1("Nueva receta: " + title, currentSelectedImageUrl);
                }
                Toast.makeText(context, "Receta guardada", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    public void setImageUrl(String imageUrl) {
        currentSelectedImageUrl = imageUrl;
        if (currentEditorView == null) return;

        ImageView ivPreview = currentEditorView.findViewById(R.id.ivRecipePreview);
        View previewContainer = currentEditorView.findViewById(R.id.recipePreviewContainer);
        previewContainer.setVisibility(View.VISIBLE);
        Glide.with(context).load(imageUrl).into(ivPreview);
    }

    public void showRecipeDetail(Recipe recipe) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_recipe_detail, null);
        builder.setView(view);

        TextView tvTitle = view.findViewById(R.id.tvRecipeDetailTitle);
        TextView tvAuthor = view.findViewById(R.id.tvRecipeDetailAuthor);
        TextView tvIngredients = view.findViewById(R.id.tvRecipeDetailIngredients);
        TextView tvSteps = view.findViewById(R.id.tvRecipeDetailSteps);
        ImageView ivRecipe = view.findViewById(R.id.ivRecipeDetailImage);
        Button btnClose = view.findViewById(R.id.btnCloseRecipeDetail);

        ivRecipe.setScaleType(ImageView.ScaleType.FIT_CENTER); // Asegurar que se vea completa

        if ("Pixel Oscuro".equals(currentTheme)) {
            view.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            tvTitle.setTextColor(Color.WHITE);
            tvAuthor.setTextColor(Color.LTGRAY);
            tvIngredients.setTextColor(Color.WHITE);
            tvSteps.setTextColor(Color.WHITE);
            tvIngredients.setBackgroundResource(R.drawable.bg_message_pixel_dark);
            tvSteps.setBackgroundResource(R.drawable.bg_message_pixel_dark);
            btnClose.setTextColor(Color.WHITE);
        }

        tvTitle.setText(recipe.getTitle());
        tvAuthor.setText("Por: " + (recipe.getAuthorName() == null ? "Anónimo" : recipe.getAuthorName()));
        
        // Formatear ingredientes con viñetas
        String ingredients = recipe.getIngredients();
        if (ingredients != null) {
            StringBuilder sb = new StringBuilder();
            String[] lines = ingredients.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    sb.append("• ").append(line.trim()).append("\n");
                }
            }
            tvIngredients.setText(sb.toString().trim());
        } else {
            tvIngredients.setText("");
        }

        tvSteps.setText(recipe.getSteps());

        if (recipe.getImageUrl() != null && !recipe.getImageUrl().trim().isEmpty()) {
            view.findViewById(R.id.recipeImageContainer).setVisibility(View.VISIBLE);
            Glide.with(context).load(recipe.getImageUrl()).into(ivRecipe);
        } else {
            view.findViewById(R.id.recipeImageContainer).setVisibility(View.GONE);
        }

        AlertDialog dialog = builder.create();
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}

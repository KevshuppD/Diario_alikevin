package calendario.kevshupp.diariokevinali;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface RecipeActionListener {
        void onRecipeClick(Recipe recipe);
        void onEditClick(Recipe recipe);
        void onDeleteClick(Recipe recipe);
    }

    private final List<Recipe> recipes;
    private final String currentUserId;
    private final RecipeActionListener listener;
    private String theme = "Pixel Claro";

    public RecipeAdapter(List<Recipe> recipes, String currentUserId, RecipeActionListener listener) {
        this.recipes = recipes;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setTheme(String theme) {
        this.theme = theme;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.tvRecipeTitle.setText(recipe.getTitle());
        holder.tvRecipeAuthor.setText("Por: " + (recipe.getAuthorName() == null ? "Anónimo" : recipe.getAuthorName()));
        holder.tvRecipeIngredients.setText(recipe.getIngredients());

        if (recipe.getImageUrl() != null && !recipe.getImageUrl().trim().isEmpty()) {
            holder.ivRecipeImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext()).load(recipe.getImageUrl()).centerCrop().into(holder.ivRecipeImage);
        } else {
            holder.ivRecipeImage.setVisibility(View.GONE);
        }

        boolean isOwner = recipe.getAuthorId() != null && recipe.getAuthorId().equals(currentUserId);
        holder.btnEditRecipe.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        holder.btnDeleteRecipe.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        if ("Pixel Oscuro".equals(theme)) {
            holder.itemView.setBackgroundResource(R.drawable.bg_message_pixel_dark);
            holder.tvRecipeTitle.setTextColor(0xFFFFFFFF);
            holder.tvRecipeAuthor.setTextColor(0xFFCCCCCC);
            holder.tvRecipeIngredients.setTextColor(0xFFFFFFFF);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_message_pixel);
            holder.tvRecipeTitle.setTextColor(0xFF4A2511);
            holder.tvRecipeAuthor.setTextColor(0xFF8B4513);
            holder.tvRecipeIngredients.setTextColor(0xFF4A2511);
        }

        holder.itemView.setOnClickListener(v -> listener.onRecipeClick(recipe));
        holder.btnEditRecipe.setOnClickListener(v -> listener.onEditClick(recipe));
        holder.btnDeleteRecipe.setOnClickListener(v -> listener.onDeleteClick(recipe));
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView tvRecipeTitle;
        TextView tvRecipeAuthor;
        TextView tvRecipeIngredients;
        ImageView ivRecipeImage;
        ImageButton btnEditRecipe;
        ImageButton btnDeleteRecipe;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecipeTitle = itemView.findViewById(R.id.tvRecipeTitle);
            tvRecipeAuthor = itemView.findViewById(R.id.tvRecipeAuthor);
            tvRecipeIngredients = itemView.findViewById(R.id.tvRecipeIngredients);
            ivRecipeImage = itemView.findViewById(R.id.ivRecipeImage);
            btnEditRecipe = itemView.findViewById(R.id.btnEditRecipe);
            btnDeleteRecipe = itemView.findViewById(R.id.btnDeleteRecipe);
        }
    }
}


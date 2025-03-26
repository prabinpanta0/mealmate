package edu.ismt.prabin.mealmate.ui.recipe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.model.Recipe

/**
 * Adapter for displaying recipes in a RecyclerView grid.
 */
class RecipeAdapter(
    private var recipes: List<Recipe>,
    private val onRecipeClicked: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.bind(recipe)
    }

    override fun getItemCount(): Int = recipes.size

    /**
     * Update the adapter with a new list of recipes
     */
    fun updateRecipes(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recipeImage: ImageView = itemView.findViewById(R.id.recipe_image)
        private val recipeTitle: TextView = itemView.findViewById(R.id.recipe_name)
        private val recipeDescription: TextView = itemView.findViewById(R.id.recipe_description)
        private val recipePrepTime: Chip = itemView.findViewById(R.id.recipe_cooking_time)
        private val recipeFoodType: Chip = itemView.findViewById(R.id.recipe_type)

        fun bind(recipe: Recipe) {
            recipeTitle.text = recipe.title
            recipeDescription.text = recipe.description
            recipePrepTime.text = "${recipe.prepTime} min"
            recipeFoodType.text = recipe.foodType

            // Load image with Glide if URL is not empty
            if (recipe.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(recipe.imageUrl)
                    .placeholder(R.drawable.placeholder_recipe)
                    .error(R.drawable.placeholder_recipe)
                    .centerCrop()
                    .into(recipeImage)
            } else {
                // Set placeholder image
                recipeImage.setImageResource(R.drawable.placeholder_recipe)
            }

            // Set click listener
            itemView.setOnClickListener {
                onRecipeClicked(recipe)
            }
        }
    }
}
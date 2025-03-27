package edu.ismt.prabin.mealmate.ui.recipe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.chip.Chip
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.model.Recipe

/**
 * Adapter for displaying recipes in a RecyclerView list.
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
        
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRecipeClicked(recipes[position])
                }
            }
        }
        
        fun bind(recipe: Recipe) {
            // Set recipe title
            recipeTitle.text = recipe.title
            
            // Set recipe description (truncate if needed)
            recipeDescription.text = recipe.description?.let {
                if (it.length > 100) "${it.take(100)}..." else it
            } ?: ""
            
            // Set prep time chip
            recipePrepTime.text = itemView.context.getString(R.string.prep_time, recipe.prepTime)
            
            // Set food type chip
            recipeFoodType.text = recipe.foodType
            
            // Load image with Glide
            if (!recipe.imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(recipe.imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_recipe)
                    .error(R.drawable.placeholder_recipe)
                    .into(recipeImage)
            } else {
                recipeImage.setImageResource(R.drawable.placeholder_recipe)
            }
        }
    }
}
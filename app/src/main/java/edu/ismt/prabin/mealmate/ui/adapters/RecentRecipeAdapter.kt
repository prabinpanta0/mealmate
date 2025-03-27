package edu.ismt.prabin.mealmate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.model.Recipe

/**
 * Adapter for displaying recent recipes in a grid layout.
 */
class RecentRecipeAdapter(
    private var recipes: List<Recipe>,
    private val onRecipeClicked: (Recipe) -> Unit
) : RecyclerView.Adapter<RecentRecipeAdapter.RecentRecipeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentRecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_recipe, parent, false)
        return RecentRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentRecipeViewHolder, position: Int) {
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

    inner class RecentRecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recipeImage: ImageView = itemView.findViewById(R.id.recipe_image)
        private val recipeName: TextView = itemView.findViewById(R.id.recipe_name)

        fun bind(recipe: Recipe) {
            recipeName.text = recipe.title

            // Load image with Glide if URL is not empty
            if (recipe.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(recipe.imageUrl)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
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
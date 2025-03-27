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
 * Adapter for displaying recipes in a ViewPager2.
 */
class RecipePagerAdapter(
    private var recipes: List<Recipe>,
    private val onRecipeClicked: (Recipe) -> Unit = {}
) : RecyclerView.Adapter<RecipePagerAdapter.RecipePagerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipePagerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_pager, parent, false)
        return RecipePagerViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipePagerViewHolder, position: Int) {
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

    inner class RecipePagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recipeImage: ImageView = itemView.findViewById(R.id.recipe_image)
        private val recipeTitle: TextView = itemView.findViewById(R.id.recipe_title)
        private val recipePrepTime: TextView = itemView.findViewById(R.id.recipe_prep_time)
        private val recipeFoodType: TextView = itemView.findViewById(R.id.recipe_food_type)

        fun bind(recipe: Recipe) {
            recipeTitle.text = recipe.title
            recipePrepTime.text = "${recipe.prepTime} min"
            recipeFoodType.text = recipe.foodType

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
package com.example.capstone_recipe.recipe_locker.locker_adpater

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.capstone_recipe.post_viewer.PostViewer
import com.example.capstone_recipe.data_class.RecipeBasicInfo
import com.example.capstone_recipe.databinding.ItemLockerRecipeViewerBinding

class LockerRecipeViewerAdapter: RecyclerView.Adapter<LockerRecipeViewerAdapter.Holder>() {
    private lateinit var binding: ItemLockerRecipeViewerBinding
    private lateinit var context: Context
    var creatorIdNameList = listOf<String>()
    var recipeList = listOf<RecipeBasicInfo>()
    var imageList = listOf<Uri>()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        binding = ItemLockerRecipeViewerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = binding.root.context
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        if(recipeList.isNotEmpty()) { holder.bind(creatorIdNameList[position], recipeList[position], imageList[position]) }
    }

    override fun getItemCount() = recipeList.size

    inner class Holder(val binding: ItemLockerRecipeViewerBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(creator: String, recipe:RecipeBasicInfo, image: Uri){
            binding.layerViewer.run {
                tvRecipeTitle.text = recipe.title
                tvRecipeCreator.text = creator
                tvRecipeIntro.text = recipe.intro
                recipeTime.text = recipe.time + "분"
                recipeLevel.text = recipe.level.toKor
                recipeLike.text = recipe.score.toString()
                Glide.with(binding.root.context)
                    .load(image)
                    .into(ivRecipeMainImage)
                itemView.setOnClickListener {
                    val intent = Intent(context, PostViewer::class.java)
                    intent.putExtra("recipeId", recipe.id)
                    context.startActivity(intent)
                }
            }
        }
    }
}
package com.example.capstone_recipe.search.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.capstone_recipe.data_class.UserInfo
import com.example.capstone_recipe.databinding.ItemUserSimpleViewerBinding
import com.example.capstone_recipe.recipe_locker.RecipeLocker

class SearchUserAdapter:RecyclerView.Adapter<SearchUserAdapter.Holder>() {
    private lateinit var binding: ItemUserSimpleViewerBinding
    private lateinit var context: Context
    var userList = listOf<UserInfo>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        binding = ItemUserSimpleViewerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = binding.root.context
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        if (userList.isNotEmpty()) { holder.bind(userList[position]) }
    }

    override fun getItemCount() = userList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateUserList(newList: List<UserInfo>){
        userList = newList
        notifyDataSetChanged()
    }

    inner class Holder(val binding: ItemUserSimpleViewerBinding):RecyclerView.ViewHolder(binding.root){
        @SuppressLint("SetTextI18n")
        fun bind(userInfo: UserInfo){
            binding.tvFriendNameWithId.text = "${userInfo.name} @${userInfo.id}"
            Glide.with(context)
                .load(userInfo.profileImageUri)
                .circleCrop()
                .into(binding.ivFriendProfileImage)

            binding.root.setOnClickListener {
                val intent = Intent(context, RecipeLocker::class.java)
                intent.putExtra("lockerOwnerId", userInfo.id)
                context.startActivity(intent)
            }
        }
    }
}
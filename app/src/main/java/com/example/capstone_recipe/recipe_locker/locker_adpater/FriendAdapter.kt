package com.example.capstone_recipe.recipe_locker.locker_adpater

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.capstone_recipe.data_class.UserInfo
import com.example.capstone_recipe.databinding.ItemUserSimpleViewerBinding
import com.example.capstone_recipe.recipe_locker.RecipeLocker

class FriendAdapter:RecyclerView.Adapter<FriendAdapter.Holder>() {
    private lateinit var binding: ItemUserSimpleViewerBinding
    private lateinit var context: Context
    var friendList = listOf<UserInfo>()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        binding = ItemUserSimpleViewerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = binding.root.context
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        if(friendList.isNotEmpty()){ holder.bind(friendList[position]) }
    }

    override fun getItemCount() = friendList.size

    inner class Holder(val binding:ItemUserSimpleViewerBinding): RecyclerView.ViewHolder(binding.root){
        @SuppressLint("SetTextI18n")
        fun bind(friend: UserInfo){
            binding.tvFriendNameWithId.text = "${friend.name} @${friend.id}"

            Glide.with(context)
                .load(friend.profileImageUri)
                .circleCrop()
                .into(binding.ivFriendProfileImage)

            itemView.setOnClickListener {
                val intent = Intent(context, RecipeLocker::class.java)
                intent.putExtra("lockerOwnerId", friend.id)
                context.startActivity(intent)
            }
        }
    }
}
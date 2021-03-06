package com.pandulapeter.campfire.feature.main.shared.recycler.viewHolder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.databinding.ItemCollectionBinding
import com.pandulapeter.campfire.feature.main.shared.recycler.viewModel.CollectionItemViewModel

class CollectionViewHolder(
    private val binding: ItemCollectionBinding,
    itemClickListener: (position: Int, clickedView: View, image: View) -> Unit,
    saveActionClickListener: ((position: Int) -> Unit)?
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            if (adapterPosition != RecyclerView.NO_POSITION) {
                itemClickListener(adapterPosition, binding.root, binding.image)
            }
        }

        binding.bookmark.setOnClickListener {
            if (adapterPosition != RecyclerView.NO_POSITION) {
                saveActionClickListener?.invoke(adapterPosition)
            }
        }
    }

    fun bind(viewModel: CollectionItemViewModel) {
        binding.viewModel = viewModel
        binding.executePendingBindings()
    }

    companion object {

        fun create(
            parent: ViewGroup,
            itemClickListener: (position: Int, clickedView: View, image: View) -> Unit,
            bookmarkClickListener: ((position: Int) -> Unit)?
        ) = CollectionViewHolder(
            DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_collection, parent, false),
            itemClickListener,
            bookmarkClickListener
        )
    }
}
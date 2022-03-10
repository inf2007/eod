package com.singaporetech.eod

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.singaporetech.eod.databinding.RecognitionItemBinding

/**
 * Adapter for the results recyclerview.
 * [Code adapted from https://github.com/hoitab/TFLClassify]
 */
class InferenceOutputsAdapter(private val ctx: Context) :
    ListAdapter<InferenceOutput, InferenceOutputsViewHolder>(RecognitionDiffUtil()) {

    /**
     * Inflating the ViewHolder with recognition_item layout and data binding
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InferenceOutputsViewHolder {
        val inflater = LayoutInflater.from(ctx)
        val binding = RecognitionItemBinding.inflate(inflater, parent, false)
        return InferenceOutputsViewHolder(binding)
    }

    // Binding the data fields to the RecognitionViewHolder
    override fun onBindViewHolder(holder: InferenceOutputsViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    private class RecognitionDiffUtil : DiffUtil.ItemCallback<InferenceOutput>() {
        override fun areItemsTheSame(oldItem: InferenceOutput, newItem: InferenceOutput): Boolean {
            return oldItem.label == newItem.label
        }

        override fun areContentsTheSame(oldItem: InferenceOutput, newItem: InferenceOutput): Boolean {
            return oldItem.confidence == newItem.confidence
        }
    }


}

/**
 * ViewHolder for the Inference Outputs.
 * [Code adapted from https://github.com/hoitab/TFLClassify]
 */
class InferenceOutputsViewHolder(private val binding: RecognitionItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    // Binding all the fields to the view - to see which UI element is bind to which field, check
    // out layout/recognition_item.xml
    fun bindTo(inferenceOutput: InferenceOutput) {
        binding.inferenceOutputItem = inferenceOutput
        binding.executePendingBindings()
    }
}
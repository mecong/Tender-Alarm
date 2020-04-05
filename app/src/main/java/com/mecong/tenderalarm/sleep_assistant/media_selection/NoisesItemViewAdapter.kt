package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mecong.tenderalarm.R

class NoisesItemViewAdapter
constructor(context: Context?, data: List<SleepNoise>, private var selectedPosition: Int)
    : RecyclerView.Adapter<NoisesItemViewAdapter.ViewHolder>() {

    private val mData: List<SleepNoise> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mClickListener: NoisesItemClickListener? = null

    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = mInflater.inflate(R.layout.fragment_noises_row, parent, false)
        return ViewHolder(view)
    }

    // binds the data to the TextView in each row
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mData[position]
        holder.headerText.text = item.name
        holder.itemView.isSelected = selectedPosition == position
    }

    // total number of rows
    override fun getItemCount(): Int {
        return mData.size
    }

    // convenience method for getting data at click position
    fun getItem(id: Int): SleepNoise {
        return mData[id]
    }

    // allows clicks events to be caught
    fun setClickListener(noisesItemClickListener: NoisesItemClickListener?) {
        mClickListener = noisesItemClickListener
    }

    /////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
// parent activity will implement this method to respond to click events
    interface NoisesItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }

    // stores and recycles views as they are scrolled off screen
    inner class ViewHolder internal constructor(itemView: View)
        : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        var headerText: TextView = itemView.findViewById(R.id.headerText)

        override fun onClick(view: View) {
            if (adapterPosition == RecyclerView.NO_POSITION) return
            notifyItemChanged(selectedPosition)
            selectedPosition = adapterPosition
            mClickListener?.onItemClick(view, adapterPosition)
            notifyItemChanged(selectedPosition)
        }

        init {
            itemView.setOnClickListener(this)
        }
    }

    // data is passed into the constructor
    init {
        if (selectedPosition > mData.size) selectedPosition = 0
    }
}
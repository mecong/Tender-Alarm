package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.app.Dialog
import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.model.PlaylistEntity
import com.mecong.tenderalarm.model.PlaylistEntity.Companion.fromCursor

// parent activity will implement this method to respond to click events
interface PlaylistItemClickListener {
    fun onPlaylistItemClick(title: String?, id: Long, position: Int)
    fun onPlaylistItemEditClick(newTitle: String, id: Long, position: Int)
    fun onPlaylistDeleteClick(id: Long, position: Int)
}

class PlaylistViewAdapter constructor(
        private val context: Context,
        private var playlists: List<PlaylistEntity>,
        private val mClickListenerPlaylist: PlaylistItemClickListener)
    : RecyclerView.Adapter<PlaylistViewAdapter.PlaylistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_playlist_row, parent, false)
        return PlaylistViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: PlaylistViewHolder, position: Int) {
        val myListItem = playlists[position]

        viewHolder.title.text = "[${myListItem.title}]"
        viewHolder.title.tag = myListItem.title
        viewHolder.id = myListItem.id
    }

    fun updateDataSet(cursor: Cursor) {
        cursor.use {
            playlists = generateSequence { if (cursor.moveToNext()) cursor else null }
                    .map { fromCursor(it) }
                    .toList()
        }

        notifyDataSetChanged()
    }


    // stores and recycles views as they are scrolled off screen
    inner class PlaylistViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var title: TextView = itemView.findViewById(R.id.headerText)
        var id: Long = -1L

        override fun onClick(view: View) {
            if (adapterPosition == RecyclerView.NO_POSITION) return
            notifyItemChanged(adapterPosition)
            mClickListenerPlaylist.onPlaylistItemClick(title.tag.toString(), id, adapterPosition)
            notifyItemChanged(adapterPosition)
        }

        init {
            val btnDeleteItem = itemView.findViewById<ImageButton>(R.id.btnDeleteItem)
            val btnEditItem = itemView.findViewById<ImageButton>(R.id.btnEditItem)

            btnDeleteItem.setOnClickListener(View.OnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) return@OnClickListener
                val popup = PopupMenu(context, btnDeleteItem)
                //Inflating the Popup using xml file
                popup.menuInflater.inflate(R.menu.menu_media_element, popup.menu)
                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener {
                    mClickListenerPlaylist.onPlaylistDeleteClick(id, adapterPosition)
                    true
                }
                popup.show() //showing popup menu
            })

            btnEditItem.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                val dialog = Dialog(context, R.style.UrlDialogCustom)
                dialog.setContentView(R.layout.url_input_dialog)
                val textUrl = dialog.findViewById<EditText>(R.id.textUrl)
                textUrl.setText(title.tag.toString())
                textUrl.hint = "Playlist name"

                val buttonOk = dialog.findViewById<Button>(R.id.buttonOk)
                val buttonOkTop = dialog.findViewById<Button>(R.id.buttonOkTop)
                val buttonCancel = dialog.findViewById<Button>(R.id.buttonCancel)
                val buttonCancelTop = dialog.findViewById<Button>(R.id.buttonCancelTop)

                val okOnclickListener: (v: View) -> Unit = {
                    val title = textUrl.text.toString()
                    if (title.isNotBlank()) {
                        mClickListenerPlaylist.onPlaylistItemEditClick(title, id, adapterPosition)
                        dialog.dismiss()
                    } else {
                        Toast.makeText(context, "Playlist name should not be empty", Toast.LENGTH_SHORT).show()
                    }
                }
                buttonOk.setOnClickListener(okOnclickListener)
                buttonOkTop.setOnClickListener(okOnclickListener)

                buttonCancel.setOnClickListener { dialog.dismiss() }
                buttonCancelTop.setOnClickListener { dialog.dismiss() }

                val lp = WindowManager.LayoutParams()
                lp.copyFrom(dialog.window!!.attributes)
                lp.width = WindowManager.LayoutParams.MATCH_PARENT
                lp.horizontalMargin = 1000.0f
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT
                dialog.show()
                dialog.window!!.attributes = lp
            }

            itemView.setOnClickListener(this)
        }
    }

    override fun getItemCount(): Int = playlists.size
}
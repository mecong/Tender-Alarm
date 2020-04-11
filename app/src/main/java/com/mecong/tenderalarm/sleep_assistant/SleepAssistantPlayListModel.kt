package com.mecong.tenderalarm.sleep_assistant

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepMediaType

class SleepAssistantPlayListModel() : ViewModel() {
    var playlist = MutableLiveData<SleepAssistantPlayList>()

    var playing = MutableLiveData<Boolean>()

    constructor(playlist: MutableLiveData<SleepAssistantPlayList>, playing: MutableLiveData<Boolean>) : this() {
        this.playlist = playlist
        this.playing = playing
    }

    //}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}]
    class SleepAssistantPlayList {
        var index: Int
        var media: List<Media>
        var mediaType: SleepMediaType
        var playListId: Long? = null

        constructor(url: String?, name: String?, mediaType: SleepMediaType) {
            media = listOf(Media(url, name))
            this.mediaType = mediaType
            index = 0
        }

        constructor(index: Int, media: List<Media>, mediaType: SleepMediaType, playListId: Long) {
            this.index = index
            this.media = media
            this.mediaType = mediaType
            this.playListId = playListId
        }
    }
}

data class Media(val url: String?, val title: String?)
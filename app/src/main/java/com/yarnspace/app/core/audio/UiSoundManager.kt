package com.yarnspace.app.core.audio

import android.content.Context
import android.media.SoundPool
import com.yarnspace.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UiSoundManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val soundPool = SoundPool.Builder().setMaxStreams(3).build()

    val soundClick: Int
    val soundFollow: Int
    val soundUnfollow: Int
    val soundReblog: Int
    val soundSave: Int
    val soundUnsave: Int
    val soundCreate: Int

    init {
        soundClick = soundPool.load(context, R.raw.click, 1)
        soundFollow = soundPool.load(context, R.raw.follow, 1)
        soundUnfollow = soundPool.load(context, R.raw.unfollow, 1)
        soundReblog = soundPool.load(context, R.raw.reblog, 1)
        soundSave = soundPool.load(context, R.raw.save, 1)
        soundUnsave = soundPool.load(context, R.raw.unsave, 1)
        soundCreate = soundPool.load(context, R.raw.create, 1)
    }


    fun play(soundId: Int) {
        if (soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }
}
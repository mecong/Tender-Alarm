package com.mecong.tenderalarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.hypertrack.hyperlog.HyperLog
import com.mecong.tenderalarm.BuildConfig
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils.TAG
import com.mecong.tenderalarm.alarm.AlarmUtils.setUpNextAlarm
import com.mecong.tenderalarm.model.AlarmEntity
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import com.mecong.tenderalarm.sleep_assistant.RadioServiceStatus
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantFragment
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*

enum class MainActivityMessages {
    ADD_ALARM,
    SWITCH_PLAYBACK
}

private var currentFragment = MainActivity.ALARM_FRAGMENT
private var isSleepAssistantPlaying = false

class MainActivity : AppCompatActivity() {


    private val sleepAssistantFragmentOpenListener: (v: View) -> Unit = {
        HyperLog.i(TAG, "Open Sleep Assistant button clicked")
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val sleepFragment = supportFragmentManager.findFragmentByTag(SLEEP_FRAGMENT)
        val alarmFragment = supportFragmentManager.findFragmentByTag(ALARM_FRAGMENT)

        fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
        fragmentTransaction.hide(alarmFragment!!)
        fragmentTransaction.show(sleepFragment!!)

        currentFragment = SLEEP_FRAGMENT

        setButtons()

        fragmentTransaction.commit()
    }

    private val alarmFragmentOpenListener: (v: View) -> Unit = {
        HyperLog.i(TAG, "Open Alarm button clicked")
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val sleepFragment = supportFragmentManager.findFragmentByTag(SLEEP_FRAGMENT)
        val alarmFragment = supportFragmentManager.findFragmentByTag(ALARM_FRAGMENT)

        fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
        fragmentTransaction.hide(sleepFragment!!)
        fragmentTransaction.show(alarmFragment!!)

        currentFragment = ALARM_FRAGMENT

        setButtons()

        fragmentTransaction.commit()
    }

    private val addAlarmListener: (v: View) -> Unit = {
        EventBus.getDefault().post(MainActivityMessages.ADD_ALARM)
    }

    private val switchPlayStateListener: (v: View) -> Unit = {
        EventBus.getDefault().post(MainActivityMessages.SWITCH_PLAYBACK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HyperLog.initialize(this)
        HyperLog.setLogLevel(Log.VERBOSE)
        setContentView(R.layout.activity_main)
        createNotificationChannels(this)

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)


        val supportFragmentManager = this@MainActivity.supportFragmentManager
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        //        fragmentTransaction.addToBackStack("Init");

        var sleepFragment: Fragment? = supportFragmentManager.findFragmentByTag(SLEEP_FRAGMENT)
        if (sleepFragment == null) {
            sleepFragment = SleepAssistantFragment()
            fragmentTransaction.add(R.id.container, sleepFragment, SLEEP_FRAGMENT)
        }


        var alarmFragment: Fragment? = supportFragmentManager.findFragmentByTag(ALARM_FRAGMENT)
        if (alarmFragment == null) {
            alarmFragment = MainAlarmFragment()
            fragmentTransaction.add(R.id.container, alarmFragment, ALARM_FRAGMENT)
        }

//        if (savedInstanceState == null) {
//            sleepFragment = SleepAssistantFragment()
//            fragmentTransaction.add(R.id.container, sleepFragment, SLEEP_FRAGMENT)
//
//            alarmFragment = MainAlarmFragment()
//            fragmentTransaction.add(R.id.container, alarmFragment, ALARM_FRAGMENT)
//        } else {
//            sleepFragment = supportFragmentManager.findFragmentByTag(SLEEP_FRAGMENT)
//            alarmFragment = supportFragmentManager.findFragmentByTag(ALARM_FRAGMENT)
//        }

        val desiredFragment = intent.getStringExtra(FRAGMENT_NAME_PARAM)

        currentFragment = if (ASSISTANT_FRAGMENT == desiredFragment) {
            fragmentTransaction.hide(alarmFragment)
            HyperLog.i(TAG, "alarmFragment hide $sleepFragment")
            fragmentTransaction.show(sleepFragment)
            HyperLog.i(TAG, "sleepFragment show $sleepFragment")
            SLEEP_FRAGMENT
        } else {
            fragmentTransaction.hide(sleepFragment)
            HyperLog.i(TAG, "sleepFragment hide $sleepFragment")
            fragmentTransaction.show(alarmFragment)
            HyperLog.i(TAG, "alarmFragment show $sleepFragment")
            ALARM_FRAGMENT
        }

        fragmentTransaction.commit()
        setButtons()
    }


    @Subscribe
    fun onEvent(status: RadioServiceStatus) {
        isSleepAssistantPlaying = when (status) {
            RadioServiceStatus.LOADING -> {
                true
            }
            RadioServiceStatus.ERROR -> {
                false
            }
            RadioServiceStatus.PLAYING -> {
                true
            }
            else -> {
                false
            }
        }

        setButtons()
    }

    private fun setButtons() {
        if (currentFragment == SLEEP_FRAGMENT) {
            if (isSleepAssistantPlaying) {
                ibOpenSleepAssistant.setImageResource(R.drawable.pause_btn)
            } else {
                ibOpenSleepAssistant.setImageResource(R.drawable.play_btn)
            }
            val paddingInDp = 10 // 10 dps
            val scale = resources.displayMetrics.density
            val paddingInPx = (paddingInDp * scale + 0.5f).toInt()
            ibOpenAlarm.setPadding(0, paddingInPx, 0, paddingInPx)


//            ibOpenSleepAssistant.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryDark))
            ibOpenAlarm.setOnClickListener(alarmFragmentOpenListener)
            ibOpenSleepAssistant.setOnClickListener(switchPlayStateListener)

            ibOpenAlarm.setImageResource(R.drawable.alarm_active)
//            ibOpenAlarm.clearColorFilter()
        } else {
            ibOpenAlarm.setImageResource(R.drawable.alarm_add)
            val paddingInDp = 8 // 7 dps
            val scale = resources.displayMetrics.density
            val paddingInPx = (paddingInDp * scale + 0.5f).toInt()
            ibOpenAlarm.setPadding(0, paddingInPx, 0, paddingInPx)
//            ibOpenAlarm.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryDark))
            ibOpenAlarm.setOnClickListener(addAlarmListener)
            ibOpenSleepAssistant.setOnClickListener(sleepAssistantFragmentOpenListener)

            ibOpenSleepAssistant.setImageResource(R.drawable.sleep_active)
//            ibOpenSleepAssistant.clearColorFilter()
        }
    }

    private fun createDebugAlarm() {
        val alarmEntity = AlarmEntity()
                .apply {
                    val calendar = Calendar.getInstance()
                    hour = calendar[Calendar.HOUR_OF_DAY]
                    minute = calendar[Calendar.MINUTE] + 2
                    complexity = 1
                    snoozeMaxTimes = 10
                    ticksTime = 1
                    isHeadsUp = true
                    val instance = sqLiteDBHelper(this@MainActivity)
                    val newId = instance!!.addOrUpdateAlarm(this)
                    id = newId
                }
        setUpNextAlarm(alarmEntity, this, true)
    }

    companion object {
        const val TIME_TO_SLEEP_CHANNEL_ID = "TA_TIME_TO_SLEEP_CHANNEL"
        const val BEFORE_ALARM_CHANNEL_ID = "TA_BEFORE_ALARM_CHANNEL"
        const val ALARM_CHANNEL_ID = "TA_BUZZER_CHANNEL"
        const val SLEEP_ASSISTANT_MEDIA_CHANNEL_ID = "TA_SLEEP_ASSISTANT_CHANNEL"
        const val FRAGMENT_NAME_PARAM = BuildConfig.APPLICATION_ID + ".fragment_name"
        const val ASSISTANT_FRAGMENT = "assistant_fragment"
        const val SLEEP_FRAGMENT = "SLEEP_FRAGMENT"
        const val ALARM_FRAGMENT = "ALARM_FRAGMENT"


        fun createNotificationChannels(context: Context) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timeToSleepChannel = NotificationChannel(
                        TIME_TO_SLEEP_CHANNEL_ID,
                        context.getString(R.string.time_to_sleep_channel_name),
                        NotificationManager.IMPORTANCE_LOW)
                        .apply {
                            description = context.getString(R.string.time_to_sleep_channel_description)
                        }

                val beforeAlarmChannel = NotificationChannel(
                        BEFORE_ALARM_CHANNEL_ID,
                        context.getString(R.string.upcoming_alarm_notification_channel_name),
                        NotificationManager.IMPORTANCE_LOW)
                        .apply {
                            description = context.getString(R.string.upcoming_alarm_channel_description)
                        }

                val sleepAssistantChannel = NotificationChannel(
                        SLEEP_ASSISTANT_MEDIA_CHANNEL_ID,
                        context.getString(R.string.sleep_assistant_media_channel_name),
                        NotificationManager.IMPORTANCE_LOW)
                        .apply {
                            setShowBadge(false)
                            description = context.getString(R.string.sleep_assistant_media_channel_description)
                        }


                val alarmChannel = NotificationChannel(
                        ALARM_CHANNEL_ID,
                        context.getString(R.string.buzzer_channel_description),
                        NotificationManager.IMPORTANCE_HIGH)
                        .apply {
                            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                            setShowBadge(false)
                            setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
                            description = context.getString(R.string.buzzer_channel_name)
                        }

                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager?.apply {
                    createNotificationChannel(timeToSleepChannel)
                    createNotificationChannel(beforeAlarmChannel)
                    createNotificationChannel(alarmChannel)
                    createNotificationChannel(sleepAssistantChannel)
                }

            }
        }
    }
}
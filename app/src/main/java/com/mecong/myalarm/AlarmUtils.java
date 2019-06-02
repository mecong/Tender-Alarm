package com.mecong.myalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.widget.Toast;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.myalarm.activity.AlarmReceiver;
import com.mecong.myalarm.model.AlarmEntity;
import com.mecong.myalarm.model.SQLiteDBHelper;

import java.util.Calendar;

import static android.app.AlarmManager.INTERVAL_FIFTEEN_MINUTES;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.mecong.myalarm.SleepTimeAlarmReceiver.RECOMMENDED_SLEEP_TIME;

public class AlarmUtils {
    public static final String TAG = "A.L.A.R.M.A";
    public static final String ALARM_ID_PARAM = "com.mecong.myalarm.alarm_id";
    public static final int MINUTE = 60 * 1000;
    public static final int HOUR = 60 * MINUTE;
    public static final int DAY = 24 * HOUR;

    public static void setUpNextAlarm(String alarmId, Context context, boolean manually) {
        AlarmEntity entity = new SQLiteDBHelper(context).getAlarmById(alarmId);
        setUpNextAlarm(entity, context, manually);
    }

    public static void setUpNextAlarm(AlarmEntity alarmEntity, Context context, boolean manually) {
        alarmEntity.updateNextAlarmDate(manually);

        Intent intentToFire = new Intent(context, AlarmReceiver.class);
        intentToFire.putExtra(ALARM_ID_PARAM, String.valueOf(alarmEntity.getId()));
        PendingIntent alarmIntent = PendingIntent.getActivity(context,
                alarmEntity.getNextRequestCode(), intentToFire, FLAG_UPDATE_CURRENT);

        HyperLog.v(TAG, "Intent: " + intentToFire);
        HyperLog.v(TAG, "Intent extra: " + intentToFire.getExtras());

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    alarmEntity.getNextTime(), alarmIntent);
        } else {
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, alarmEntity.getNextTime(), alarmIntent);
        }

        new SQLiteDBHelper(context).updateAlarm(alarmEntity);
        HyperLog.i(TAG, "Next alarm with[id=" + alarmEntity.getId() + "] set to: "
                + context.getString(R.string.next_alarm_date, alarmEntity.getNextTime()));

        setUpSleepTimeAlarm(context);

        if (alarmEntity.isBeforeAlarmNotification()) {
            setupUpcomingAlarmNotification(context, alarmEntity);
        }
    }

    private static void setupUpcomingAlarmNotification(Context context, AlarmEntity alarmEntity) {
        Intent intentToFire = new Intent(context, UpcomingAlarmNotificationReceiver.class);
        intentToFire.putExtra(ALARM_ID_PARAM, String.valueOf(alarmEntity.getId()));
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context,
                alarmEntity.getNextRequestCode(), intentToFire, FLAG_UPDATE_CURRENT);

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long at = SystemClock.elapsedRealtime()
                + alarmEntity.getNextTime() - HOUR
                - Calendar.getInstance().getTimeInMillis();

        at = Math.max(0, at);

        HyperLog.i(TAG, "Upcoming alarm notification will start in " + at + " ms");

        alarmMgr.set(AlarmManager.ELAPSED_REALTIME, at, alarmIntent);
    }

    public static void setUpSleepTimeAlarm(Context context) {
        AlarmEntity nextMorningAlarm = new SQLiteDBHelper(context).getNextMorningAlarm();
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, SleepTimeAlarmReceiver.class);
        PendingIntent operation = PendingIntent
                .getBroadcast(context, 1, intent, 0);
        if (nextMorningAlarm != null) {

            long triggerAfter = SystemClock.elapsedRealtime()
                    + nextMorningAlarm.getNextTime() - RECOMMENDED_SLEEP_TIME * HOUR
                    - Calendar.getInstance().getTimeInMillis();

            triggerAfter = Math.max(0, triggerAfter);
            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, triggerAfter,
                    INTERVAL_FIFTEEN_MINUTES, operation);
            HyperLog.i(TAG, "Sleep time will start in " + triggerAfter + " ms");
        } else {
            alarmMgr.cancel(operation);
            HyperLog.i(TAG, "Sleep time alarm removed");
        }
    }

    static void resetupAllAlarms(Context context) {
        // TODO: implement
        Toast.makeText(context, "Alarms Was not reset", Toast.LENGTH_LONG).show();

        setUpSleepTimeAlarm(context);
    }

    public static void cancelNextAlarm(String id, Context context) {
        SQLiteDBHelper sqLiteDBHelper = new SQLiteDBHelper(context);
        AlarmEntity entity = sqLiteDBHelper.getAlarmById(id);

        Intent intentToFire = new Intent(context, AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getActivity(context,
                entity.getNextRequestCode(), intentToFire, FLAG_UPDATE_CURRENT);

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmMgr.cancel(alarmIntent);

        HyperLog.i(TAG, "Next alarm with[id=" + id + "] canceled");

        setUpSleepTimeAlarm(context);
    }


    //TODO: set when there are alarms and turn of if no any
    public static void setBootReceiverActive(Context context) {
        ComponentName receiver = new ComponentName(context, DeviceBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
}

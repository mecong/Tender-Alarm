package com.mecong.tenderalarm.alarm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.logs.LogsActivity;
import com.mecong.tenderalarm.model.AlarmEntity;
import com.mecong.tenderalarm.model.SQLiteDBHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_OK;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;
import static com.mecong.tenderalarm.alarm.AlarmUtils.MINUTE;
import static com.mecong.tenderalarm.alarm.AlarmUtils.setUpNextSleepTimeNotification;

public class MainAlarmFragment extends Fragment {


    private static final int ALARM_ADDING_REQUEST_CODE = 42;
    private AlarmsListCursorAdapter alarmsAdapter;
    private TextView textNextAlarm, textNextAlarmDate;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.content_main, container, false);

        final Context context = this.getActivity();
        HyperLog.initialize(context);
        HyperLog.setLogLevel(Log.VERBOSE);

        textNextAlarm = rootView.findViewById(R.id.textNextAlarm);
        textNextAlarmDate = rootView.findViewById(R.id.textNextAlarmDate);

        textNextAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addAlarmIntent = new Intent(context, LogsActivity.class);
                startActivity(addAlarmIntent);
            }
        });


        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);
        alarmsAdapter =
                new AlarmsListCursorAdapter(this, sqLiteDBHelper.getAllAlarms());

        ListView alarmsList = rootView.findViewById(R.id.alarms_list);
        alarmsList.setAdapter(alarmsAdapter);
        updateNextActiveAlarm(sqLiteDBHelper);

        Button fab = rootView.findViewById(R.id.btnAddAlarm);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addAlarmIntent = new Intent(context, AlarmAddingActivity.class);
                MainAlarmFragment.this.startActivityForResult(addAlarmIntent, ALARM_ADDING_REQUEST_CODE);
            }
        });

        sqLiteDBHelper.close();
        return rootView;
    }


    private void updateNextActiveAlarm(SQLiteDBHelper sqLiteDBHelper) {
        Context context = this.getContext();

        AlarmEntity nextActiveAlarm = sqLiteDBHelper.getNextActiveAlarm();
        if (nextActiveAlarm != null) {
            AlarmUtils.setBootReceiverActive(context);

            Calendar calendar = Calendar.getInstance();
            long nextAlarmTime = nextActiveAlarm.getNextTime()
                    + TimeUnit.MINUTES.toMillis(nextActiveAlarm.getTicksTime());
            long difference = nextAlarmTime - calendar.getTimeInMillis();
            calendar.setTime(new Date(difference));
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

            if (difference < MINUTE) {
                textNextAlarm.setText(context.getString(R.string.next_alarm_soon));
            } else if (difference < AlarmUtils.HOUR) {
                textNextAlarm.setText(context.getString(R.string.next_alarm_within_hour,
                        calendar.get(Calendar.MINUTE)));
            } else if (difference < AlarmUtils.DAY) {
                textNextAlarm.setText(context.getString(R.string.next_alarm_today,
                        calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)));
            } else {
                textNextAlarm.setText(context.getString(R.string.next_alarm,
                        calendar.get(Calendar.DAY_OF_YEAR)));
            }
            textNextAlarmDate.setText(context
                    .getString(R.string.next_alarm_date_time, nextAlarmTime));
        } else {
            textNextAlarm.setText(R.string.all_alarms_are_off);
            textNextAlarmDate.setText("");
        }

        setUpNextSleepTimeNotification(context);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Context context = this.getActivity();

        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);

        // Check which request we're responding to
        // Make sure the request was successful
        if (requestCode == ALARM_ADDING_REQUEST_CODE && resultCode == RESULT_OK) {
            alarmsAdapter.changeCursor(sqLiteDBHelper.getAllAlarms());
            updateNextActiveAlarm(sqLiteDBHelper);
            Snackbar.make(this.textNextAlarm,
                    "New alarm created", LENGTH_SHORT)
                    .setAction("Action", null).show();
        }
        sqLiteDBHelper.close();
    }


    void deleteAlarmWithDialog(final String id) {
        final Context context = this.getActivity();

        new AlertDialog.Builder(new ContextThemeWrapper(context, AlertDialog.THEME_DEVICE_DEFAULT_DARK))
                .setTitle("Delete alarm")
                .setMessage("Do you really want to delete this alarm?")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        deleteAlarm(id);
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    void deleteAlarm(String id) {
        final Context context = this.getActivity();
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);

        AlarmUtils.turnOffAlarm(id, context);
        sqLiteDBHelper.deleteAlarm(id);
        alarmsAdapter.changeCursor(sqLiteDBHelper.getAllAlarms());
        updateNextActiveAlarm(sqLiteDBHelper);
        sqLiteDBHelper.close();
    }

    void setActive(String id, boolean active) {
        Context context = this.getActivity();

        if (active) {
            AlarmUtils.setUpNextAlarm(id, context, true);
        } else {
            AlarmUtils.turnOffAlarm(id, context);
        }

        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);
        sqLiteDBHelper.toggleAlarmActive(id, active);
        alarmsAdapter.changeCursor(sqLiteDBHelper.getAllAlarms());
        updateNextActiveAlarm(sqLiteDBHelper);
        sqLiteDBHelper.close();
    }

    void cancelNextAlarms(String id, int num) {
        Context context = this.getActivity();
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);
        final AlarmEntity alarmById = sqLiteDBHelper.getAlarmById(id);
        alarmById.setCanceledNextAlarms(num);
        sqLiteDBHelper.addOrUpdateAlarm(alarmById);
        alarmsAdapter.changeCursor(sqLiteDBHelper.getAllAlarms());
        updateNextActiveAlarm(sqLiteDBHelper);
        sqLiteDBHelper.close();
    }

    void editAlarm(String id) {
        Intent addAlarmIntent = new Intent(this.getActivity(), AlarmAddingActivity.class);
        addAlarmIntent.putExtra("alarmId", id);
        MainAlarmFragment.this.startActivityForResult(addAlarmIntent, ALARM_ADDING_REQUEST_CODE);
    }
}

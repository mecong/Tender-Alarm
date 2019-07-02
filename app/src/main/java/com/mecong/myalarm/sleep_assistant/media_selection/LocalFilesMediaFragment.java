package com.mecong.myalarm.sleep_assistant.media_selection;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.myalarm.R;
import com.mecong.myalarm.alarm.AlarmUtils;
import com.mecong.myalarm.model.SQLiteDBHelper;
import com.mecong.myalarm.sleep_assistant.SleepAssistantViewModel;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor //required
public class LocalFilesMediaFragment extends Fragment implements MediaItemViewAdapter.ItemClickListener {

    private static final int READ_REQUEST_CODE = 42;
    MediaItemViewAdapter adapter;
    SleepAssistantViewModel model;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NoisesFragment.
     */
    static LocalFilesMediaFragment newInstance() {
        return new LocalFilesMediaFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = ViewModelProviders.of(getActivity()).get(SleepAssistantViewModel.class);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_local_media, container, false);
    }

    private void addOnlineMediaRecord(String url, String title) {
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(this.getContext());
        sqLiteDBHelper.addLocalMediaUrl(url, title);

        adapter.changeCursor(sqLiteDBHelper.getAllLocalMedia());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final RecyclerView recyclerView = view.findViewById(R.id.mediaListView);
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(this.getContext());

        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        adapter = new MediaItemViewAdapter(view.getContext(),
                sqLiteDBHelper.getAllLocalMedia(), this);

        recyclerView.setAdapter(adapter);

        final Button buttonAdd = view.findViewById(R.id.buttonAdd);
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFileSearch();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                HyperLog.i(AlarmUtils.TAG, "Uri: " + uri.toString());
                addOnlineMediaRecord(uri.toString(), dumpFileMetaData(uri));
            }
        }
    }

    private String dumpFileMetaData(Uri uri) {

        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.

        try (Cursor cursor = getActivity().getContentResolver()
                .query(uri, null, null, null, null, null)) {
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (cursor != null && cursor.moveToFirst()) {

                // Note it's called "Display Name".  This is
                // provider-specific, and might not necessarily be the file name.
                String displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                HyperLog.i(AlarmUtils.TAG, "Display Name: " + displayName);

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                // If the size is unknown, the value stored is null.  But since an
                // int can't be null in Java, the behavior is implementation-specific,
                // which is just a fancy term for "unpredictable".  So as
                // a rule, check if it's null before assigning to an int.  This will
                // happen often:  The storage API allows for remote files, whose
                // size might not be locally known.
                String size;
                if (!cursor.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    size = cursor.getString(sizeIndex);
                } else {
                    size = "Unknown";
                }
                HyperLog.i(AlarmUtils.TAG, "Size: " + size);

                return displayName;
            }
        }

        return null;
    }

    private void performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("audio/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }


    @Override
    public void onItemClick(String url, int position) {
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(this.getContext());
        List<String> urls;
        try (Cursor allLocalMedia = sqLiteDBHelper.getAllLocalMedia()) {
            urls = new ArrayList<>(allLocalMedia.getCount());
            while (allLocalMedia.moveToNext()) {
                urls.add(allLocalMedia.getString(allLocalMedia.getColumnIndex("uri")));
            }
        }

        SleepAssistantViewModel.PlayList newPlayList =
                new SleepAssistantViewModel.PlayList(urls, position, SleepMediaType.LOCAL);
        model.setPlaylist(newPlayList);
    }


    @Override
    public void onItemDeleteClick(int position) {
        long itemId = adapter.getItemId(position);
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(this.getContext());
        sqLiteDBHelper.deleteLocalMedia(String.valueOf(itemId));
        adapter.changeCursor(sqLiteDBHelper.getAllLocalMedia());
    }
}

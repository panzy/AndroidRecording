package com.skd.androidrecordingtest;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.skd.androidrecording.ui.AudioRecordingActivity;
import com.skd.androidrecording.ui.VideoRecordingActivity;

public class MainActivity extends Activity {

	private static final int REQ_CAP_VIDEO = 123;
	private static final String TAG = MainActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		ListView list = (ListView) findViewById(android.R.id.list);
		ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(getApplicationContext(),
																	android.R.layout.simple_list_item_1,
																	getResources().getStringArray(R.array.activities));
		list.setAdapter(listAdapter);
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				launchDemo(arg2);
			}
		});
	}

	private void launchDemo(int position) {
		switch (position) {
			case 0: default: {
				Intent i = new Intent(MainActivity.this, AudioRecordingActivity.class);
				startActivity(i);
				break;
			}
			case 1: {
				Intent i = new Intent(MainActivity.this, VideoRecordingActivity.class)
						.putExtra(VideoRecordingActivity.EXTRA_HIDE_VIDEOSIZE_PICKER, false)
						.putExtra(VideoRecordingActivity.EXTRA_FILE_LIMIT, 1024 * 1024 * 2)
						.putExtra(VideoRecordingActivity.EXTRA_DURATION_LIMIT, 10)
						.putExtra(VideoRecordingActivity.EXTRA_PREFERRED_WIDTH, 720)
						.putExtra(VideoRecordingActivity.EXTRA_PREFERRED_HEIGHT, 480);
				startActivityForResult(i, REQ_CAP_VIDEO);
				break;
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQ_CAP_VIDEO) {
			if (resultCode == RESULT_OK && data != null) {
				Uri uri = data.getData();
				if (uri != null) {
					Log.i(TAG, "REQ_CAP_VIDEO result " + uri.toString());
				}
			}
		}
	}
}

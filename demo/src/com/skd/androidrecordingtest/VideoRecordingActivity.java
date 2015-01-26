package com.skd.androidrecordingtest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import com.skd.androidrecording.video.AdaptiveSurfaceView;
import com.skd.androidrecording.video.CameraHelper;
import com.skd.androidrecording.video.VideoRecordingHandler;
import com.skd.androidrecording.video.VideoRecordingManager;
import com.skd.androidrecordingtest.utils.NotificationUtils;
import com.skd.androidrecordingtest.utils.StorageUtils;

import java.util.ArrayList;
import java.util.List;

public class VideoRecordingActivity extends Activity {
	/** 输入参数：优先采用的视频尺寸（像素）。 */
	public static final String EXTRA_PREFERRED_WIDTH = "preferred_width";
	/** 输入参数：优先采用的视频尺寸（像素）。 */
	public static final String EXTRA_PREFERRED_HEIGHT = "preferred_height";

	private static String fileName = null;
    
	private Button recordBtn, playBtn;
	private ImageButton switchBtn;
	private Spinner videoSizeSpinner;

	private Size videoSize = null;
	private int preferredWidth;
	private int preferredHeight;
	private ArrayList<Size> supportedSizes = new ArrayList<Size>();
	private VideoRecordingManager recordingManager;
	
	private VideoRecordingHandler recordingHandler = new VideoRecordingHandler() {
		@Override
		public boolean onPrepareRecording() {
			if (videoSizeSpinner == null) {
	    		initVideoSizeSpinner();
	    		return true;
			}
			return false;
		}
		
		@Override
		public Size getVideoSize() {
			return videoSize;
		}
		
		@Override
		public int getDisplayRotation() {
			return VideoRecordingActivity.this.getWindowManager().getDefaultDisplay().getRotation();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_rec);
		
		if (!StorageUtils.checkExternalStorageAvailable()) {
			NotificationUtils.showInfoDialog(this, getString(R.string.noExtStorageAvailable));
			return;
		}
		fileName = StorageUtils.getFileName(false);

		if (savedInstanceState != null) {
			preferredWidth = savedInstanceState.getInt(EXTRA_PREFERRED_WIDTH);
			preferredHeight = savedInstanceState.getInt(EXTRA_PREFERRED_HEIGHT);
		} else if (getIntent() != null) {
			preferredWidth = getIntent().getIntExtra(EXTRA_PREFERRED_WIDTH, 0);
			preferredHeight = getIntent().getIntExtra(EXTRA_PREFERRED_HEIGHT, 0);
		}
		
		AdaptiveSurfaceView videoView = (AdaptiveSurfaceView) findViewById(R.id.videoView);
		recordingManager = new VideoRecordingManager(videoView, recordingHandler);
		
		recordBtn = (Button) findViewById(R.id.recordBtn);
		recordBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				record();
			}
		});
		
		switchBtn = (ImageButton) findViewById(R.id.switchBtn);
		if (recordingManager.getCameraManager().hasMultipleCameras()) {
			switchBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					switchCamera();
				}
			});
		}
		else {
			switchBtn.setVisibility(View.GONE);
		}
		
		playBtn = (Button) findViewById(R.id.playBtn);
		playBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				play();
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		recordingManager.dispose();
		recordingHandler = null;
		
		super.onDestroy();
	}
	
	@SuppressLint("NewApi")
	private void initVideoSizeSpinner() {
		videoSizeSpinner = (Spinner) findViewById(R.id.videoSizeSpinner);
		if (Build.VERSION.SDK_INT >= 11) {
			List<Size> sizes = CameraHelper.getCameraSupportedVideoSizes(recordingManager.getCameraManager().getCamera());
			supportedSizes.clear();
			supportedSizes.addAll(sizes);
			videoSizeSpinner.setAdapter(new SizeAdapter(sizes));
			videoSizeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					videoSize = (Size) arg0.getItemAtPosition(arg2);
					recordingManager.setPreviewSize(videoSize);
				}
	
				@Override
				public void onNothingSelected(AdapterView<?> arg0) {}
			});
			pickPreferredSize();
		}
		else {
			videoSizeSpinner.setVisibility(View.GONE);
		}
	}
	
	@SuppressLint("NewApi")
	private void updateVideoSizes() {
		if (Build.VERSION.SDK_INT >= 11) {
			List<Size> sizes = CameraHelper.getCameraSupportedVideoSizes(recordingManager.getCameraManager().getCamera());
			supportedSizes.clear();
			supportedSizes.addAll(sizes);
			((SizeAdapter) videoSizeSpinner.getAdapter()).set(sizes);
			pickPreferredSize();
			recordingManager.setPreviewSize(videoSize);
		}
	}

	private void pickPreferredSize() {
		if (supportedSizes != null && supportedSizes.size() > 0) {
			int idx = 0;

			if (preferredWidth > 0 && preferredHeight > 0 && supportedSizes.size() > 1) {
				// return the minimum
				int delta = Math.abs((supportedSizes.get(0).width - preferredWidth)
						+ (supportedSizes.get(0).height - preferredHeight));
				for (int i = 1; i < supportedSizes.size(); ++i) {
					int d = Math.abs((supportedSizes.get(i).width - preferredWidth)
							+ (supportedSizes.get(i).height - preferredHeight));
					if (d < delta) {
						idx = i;
						delta = d;
					}
				}
			}

			videoSize = supportedSizes.get(idx);
			videoSizeSpinner.setSelection(idx);
		}
	}

	private void switchCamera() {
		recordingManager.getCameraManager().switchCamera();
		updateVideoSizes();
	}
	
	private void record() {
		if (recordingManager.stopRecording()) {
			recordBtn.setText(R.string.recordBtn);
			switchBtn.setEnabled(true);
			playBtn.setEnabled(true);
			videoSizeSpinner.setEnabled(true);
		}
		else {
			startRecording();
		}
	}
	
	private void startRecording() {
		if (recordingManager.startRecording(fileName, videoSize)) {
			recordBtn.setText(R.string.stopRecordBtn);
			switchBtn.setEnabled(false);
			playBtn.setEnabled(false);
			videoSizeSpinner.setEnabled(false);
			return;
		}
		Toast.makeText(this, getString(R.string.videoRecordingError), Toast.LENGTH_LONG).show();
	}
	
	private void play() {
		Intent i = new Intent(VideoRecordingActivity.this, VideoPlaybackActivity.class);
		i.putExtra(VideoPlaybackActivity.FileNameArg, fileName);
		startActivityForResult(i, 0);
	}
}

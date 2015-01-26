package com.skd.androidrecording.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import com.skd.androidrecording.R;
import com.skd.androidrecording.video.AdaptiveSurfaceView;
import com.skd.androidrecording.video.CameraHelper;
import com.skd.androidrecording.video.VideoRecordingHandler;
import com.skd.androidrecording.video.VideoRecordingManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoRecordingActivity extends Activity {
	/** 输入参数：优先采用的视频尺寸（像素）。 */
	public static final String EXTRA_PREFERRED_WIDTH = "preferred_width";
	/** 输入参数：优先采用的视频尺寸（像素）。 */
	public static final String EXTRA_PREFERRED_HEIGHT = "preferred_height";
	/** 输入参数：最大允许的文件尺寸（字节, Int） */
	public static final String EXTRA_FILE_LIMIT = "file_limit";
	/** 输入参数：最大允许视频长度（秒） */
	public static final String EXTRA_DURATION_LIMIT = "duration_limit";
	/** 输入参数：是否隐藏视频尺寸选择控件？ */
	public static final String EXTRA_HIDE_VIDEOSIZE_PICKER = "hide_videosize_picker";

	private static String fileName = null;
    
	private Button recordBtn, playBtn;
	private ImageButton switchBtn;
	private Spinner videoSizeSpinner;

	private Size videoSize = null;
	private int preferredWidth;
	private int preferredHeight;
	private int durationLimit = 0;
	private int fileLimit = 0;
	private boolean hideVideoSizePicker;
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

		@Override
		public void onStoppedRecording() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateUiStateForNotRecording();
					setSuccessResult();
					addToMediaStore();
				}
			});
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Remove title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.video_rec);
		
		if (!StorageUtils.checkExternalStorageAvailable()) {
			NotificationUtils.showInfoDialog(this, getString(R.string.noExtStorageAvailable));
			return;
		}
		fileName = StorageUtils.getFileName(false);

		if (savedInstanceState != null) {
			preferredWidth = savedInstanceState.getInt(EXTRA_PREFERRED_WIDTH);
			preferredHeight = savedInstanceState.getInt(EXTRA_PREFERRED_HEIGHT);
			fileLimit = savedInstanceState.getInt(EXTRA_FILE_LIMIT);
			durationLimit = savedInstanceState.getInt(EXTRA_DURATION_LIMIT);
			hideVideoSizePicker = savedInstanceState.getBoolean(EXTRA_HIDE_VIDEOSIZE_PICKER);
		} else if (getIntent() != null && getIntent().getExtras() != null) {
			Bundle bundle = getIntent().getExtras();
			preferredWidth = bundle.getInt(EXTRA_PREFERRED_WIDTH);
			preferredHeight = bundle.getInt(EXTRA_PREFERRED_HEIGHT);
			fileLimit = bundle.getInt(EXTRA_FILE_LIMIT);
			durationLimit = bundle.getInt(EXTRA_DURATION_LIMIT);
			hideVideoSizePicker = bundle.getBoolean(EXTRA_HIDE_VIDEOSIZE_PICKER);
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

		if (hideVideoSizePicker) {
			findViewById(R.id.videoSizeSpinner).setVisibility(View.GONE);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt(EXTRA_PREFERRED_WIDTH, preferredWidth);
		outState.putInt(EXTRA_PREFERRED_HEIGHT, preferredHeight);
		outState.putInt(EXTRA_FILE_LIMIT, fileLimit);
		outState.putInt(EXTRA_DURATION_LIMIT, durationLimit);
		outState.putBoolean(EXTRA_HIDE_VIDEOSIZE_PICKER, hideVideoSizePicker);
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
			recordingManager.setPreviewSize(videoSize);
		}
	}

	private void setSuccessResult() {
		Intent data = new Intent().setData(Uri.fromFile(new File(fileName)));
		setResult(RESULT_OK, data);
	}

	private void addToMediaStore() {
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(fileName))));
	}

	private void switchCamera() {
		recordingManager.getCameraManager().switchCamera();
		updateVideoSizes();
	}
	
	private void record() {
		if (recordingManager.stopRecording(false)) {
			updateUiStateForNotRecording();
			setSuccessResult();
			addToMediaStore();
		}
		else {
			startRecording();
		}
	}

	private void startRecording() {
		if (recordingManager.startRecording(fileName, videoSize, fileLimit, durationLimit)) {
			updateUiStateForRecording();
			return;
		}
		Toast.makeText(this, getString(R.string.videoRecordingError), Toast.LENGTH_LONG).show();
	}

	private void updateUiStateForNotRecording() {
		recordBtn.setText(R.string.recordBtn);
		switchBtn.setEnabled(true);
		playBtn.setEnabled(true);
		videoSizeSpinner.setEnabled(true);
	}

	private void updateUiStateForRecording() {
		recordBtn.setText(R.string.stopRecordBtn);
		switchBtn.setEnabled(false);
		playBtn.setEnabled(false);
		videoSizeSpinner.setEnabled(false);
	}

	private void play() {
		Intent i = new Intent(VideoRecordingActivity.this, VideoPlaybackActivity.class);
		i.putExtra(VideoPlaybackActivity.FileNameArg, fileName);
		startActivityForResult(i, 0);
	}
}

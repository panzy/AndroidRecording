package com.skd.androidrecording.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
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
	/** 输入参数：不允许除这些以外的视频尺寸供用户选择。
	 * 值应该是一个 ArrayList&lt;Integer&gt; = [ size1.w, size1.h, size2.w, size2.h, ... ]*/
	public static final String EXTRA_ALLOWED_VIDEO_SIZES = "allowed_video_sizes ";

	private static final int REQ_PREVIEW = 123;

	private static String fileName = null;

	private AdaptiveSurfaceView videoView;
	private ImageButton recordBtn;
	private ImageButton switchBtn;
	private View settingBtn;
	private TextView durationText;
	private TextView fileSizeText;

	// current video size
	private Size videoSize = null;
	private int preferredWidth;
	private int preferredHeight;
	private int durationLimit = 0;
	private int fileLimit = 0;
	private boolean hideVideoSizePicker;
	private ArrayList<Integer> allowedSizes;
	private ArrayList<Size> supportedSizes = new ArrayList<>();
	private VideoRecordingManager recordingManager;
	private boolean previewStarted;

	private VideoRecordingHandler recordingHandler = new VideoRecordingHandler() {
		@Override
		public boolean onPrepareRecording() {
			if (supportedSizes.isEmpty()) {
	    		loadVideoSizes();
			}

			if (!previewStarted) {
				previewStarted = restartPreview(pickPreferredSize());
			}

			return true;
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
					addToMediaStore();
					preview();
				}
			});
		}

		@Override
		public void onRecordingProgress(final int duration, final int fileSize) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (durationLimit <= 0)
						durationText.setText(formatDuration(duration));
					else
						durationText.setText(formatDuration(duration) + "/" + formatDuration(durationLimit));

					if (fileLimit <= 0)
						fileSizeText.setText(formatFileSize(fileSize));
					else
						fileSizeText.setText(formatFileSize(fileSize) + "/" + formatFileSize(fileLimit));
				}
			});
		}

		/**
		 * @param duration in sec.
		 * @return
		 */
		private String formatDuration(int duration) {
			return String.format("%02d:%02d", duration / 60, (duration % 60));
		}

		/**
		 * @param size in bytes.
		 * @return
		 */
		private String formatFileSize(int size) {
			if (size >= 1024 * 1024) {
				return String.format("%.1fMB", (float) size / 1024 / 1024);
			} else if (size >= 1024) {
				return String.format("%dKB", size / 1024);
			} else {
				return String.format("%dB", size);
			}
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
			allowedSizes = savedInstanceState.getIntegerArrayList(EXTRA_ALLOWED_VIDEO_SIZES);
		} else if (getIntent() != null && getIntent().getExtras() != null) {
			Bundle bundle = getIntent().getExtras();
			preferredWidth = bundle.getInt(EXTRA_PREFERRED_WIDTH);
			preferredHeight = bundle.getInt(EXTRA_PREFERRED_HEIGHT);
			fileLimit = bundle.getInt(EXTRA_FILE_LIMIT);
			durationLimit = bundle.getInt(EXTRA_DURATION_LIMIT);
			hideVideoSizePicker = bundle.getBoolean(EXTRA_HIDE_VIDEOSIZE_PICKER);
			allowedSizes = bundle.getIntegerArrayList(EXTRA_ALLOWED_VIDEO_SIZES);
		}
		if (allowedSizes != null) {
			CameraHelper.setAllowedVideoSizes(allowedSizes);
		}
		
		videoView = (AdaptiveSurfaceView) findViewById(R.id.videoView);
		recordingManager = new VideoRecordingManager(videoView, recordingHandler);

		durationText = (TextView) findViewById(R.id.durationText);
		durationText.setVisibility(View.INVISIBLE);

		fileSizeText = (TextView) findViewById(R.id.fileSizeText);
		fileSizeText.setVisibility(View.INVISIBLE);

		recordBtn = (ImageButton) findViewById(R.id.recordBtn);
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

		settingBtn = (ImageButton) findViewById(R.id.settingBtn);
		settingBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showSettings();
			}
		});
		
		if (hideVideoSizePicker) {
			findViewById(R.id.settingBtn).setVisibility(View.GONE);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		previewStarted = false;
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQ_PREVIEW) {
			if (resultCode == RESULT_OK) {
				setSuccessResult();
				finish();
			} else if (data == null) {
				setResult(RESULT_CANCELED);
				finish();
			}
			// else: retake
		}
	}

	@SuppressLint("NewApi")
	private void loadVideoSizes() {
		List<Size> sizes = CameraHelper.getCameraSupportedVideoSizes(recordingManager.getCameraManager().getCamera());
		supportedSizes.clear();
		supportedSizes.addAll(sizes);
	}

	private Size pickPreferredSize() {
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

			return supportedSizes.get(idx);
		}

		return null;
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
		loadVideoSizes();
		restartPreview(pickPreferredSize());
	}

	private void record() {
		if (recordingManager.stopRecording(false)) {
			updateUiStateForNotRecording();
			addToMediaStore();
			preview();
		} else {
			startRecording();
		}
	}

	private boolean restartPreview(Size videoSize) {
		if (videoSize == null) {
			Toast.makeText(this, "Failed to get camera video size", Toast.LENGTH_LONG).show();
			return false;
		}

		this.videoSize = videoSize;
		recordingManager.setPreviewSize(this.videoSize);
		recordingManager.getCameraManager().setupCameraAndStartPreview(
				videoView.getHolder(),
				recordingHandler.getVideoSize(),
				recordingHandler.getDisplayRotation());
		return recordingManager.getCameraManager().isPreviewStarted();
	}

	private void startRecording() {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected void onPreExecute() {
				// temporarily disable button to avoid MediaRecorder RuntimeException
				recordBtn.setEnabled(false);
			}

			@Override
			protected Boolean doInBackground(Void... voids) {
				return recordingManager.startRecording(fileName, fileLimit, durationLimit);
			}

			@Override
			protected void onPostExecute(Boolean result) {
				recordBtn.setEnabled(true);
				if (result)
					updateUiStateForRecording();
				else
					Toast.makeText(VideoRecordingActivity.this,
							getString(R.string.videoRecordingError), Toast.LENGTH_LONG).show();
			}
		}.execute();
	}

	private void updateUiStateForNotRecording() {
		recordBtn.setImageResource(R.drawable.btn_record_video_start);
		switchBtn.setVisibility(View.VISIBLE);
		if (!hideVideoSizePicker)
			settingBtn.setVisibility(View.VISIBLE);
		durationText.setVisibility(View.INVISIBLE);
		fileSizeText.setVisibility(View.INVISIBLE);
	}

	private void updateUiStateForRecording() {
		recordBtn.setImageResource(R.drawable.btn_record_video_stop);
		switchBtn.setVisibility(View.GONE);
		if (!hideVideoSizePicker)
			settingBtn.setVisibility(View.GONE);
		durationText.setVisibility(View.VISIBLE);
		fileSizeText.setVisibility(View.VISIBLE);
	}

	private void preview() {
		Intent i = new Intent(VideoRecordingActivity.this, VideoPlaybackActivity.class);
		i.putExtra(VideoPlaybackActivity.FileNameArg, fileName);
		startActivityForResult(i, REQ_PREVIEW);
	}

	private void showSettings() {
		String[] items = new String[supportedSizes.size()];
		int i = 0;
		for (Size sz : supportedSizes) {
			items[i] = String.format("%c %d x %d", (sz == videoSize ? '*' : ' '), sz.width, sz.height);
			++i;
		}

		new AlertDialog.Builder(this)
				.setTitle(R.string.select_video_size)
				.setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						restartPreview(supportedSizes.get(which));
					}
				})
				.create()
				.show();
	}
}

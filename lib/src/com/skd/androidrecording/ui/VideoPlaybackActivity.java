package com.skd.androidrecording.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import com.skd.androidrecording.R;
import com.skd.androidrecording.video.AdaptiveSurfaceView;
import com.skd.androidrecording.video.PlaybackHandler;
import com.skd.androidrecording.video.VideoPlaybackManager;

public class VideoPlaybackActivity extends Activity implements View.OnClickListener {
	public static String FileNameArg = "arg_filename";
	
	private static String fileName = null;
	
	private AdaptiveSurfaceView videoView;
	
	private VideoPlaybackManager playbackManager;
	
	private PlaybackHandler playbackHandler = new PlaybackHandler() {
		@Override
		public void onPreparePlayback() {
			runOnUiThread (new Runnable() {
		    	public void run() {
		    		playbackManager.showMediaController();
					playbackManager.start();
		    	}
		    });
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_play);
	
		Intent i = getIntent();
		if ((i != null) && (i.getExtras() != null)) {
			fileName = i.getExtras().getString(FileNameArg);
		}
		
		videoView = (AdaptiveSurfaceView) findViewById(R.id.videoView);
		
		playbackManager = new VideoPlaybackManager(this, videoView, findViewById(R.id.mediaControllerAnchor), playbackHandler);
		playbackManager.setupPlayback(fileName);

		findViewById(R.id.acceptBtn).setOnClickListener(this);
		findViewById(R.id.discardBtn).setOnClickListener(this);
		findViewById(R.id.retakeBtn).setOnClickListener(this);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		playbackManager.showMediaController();
	    return false;
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		playbackManager.pause();
		playbackManager.hideMediaController();
	}

	@Override
	protected void onDestroy() {
		playbackManager.dispose();
		playbackHandler = null;
		
		super.onDestroy();
	}

	@Override
	public void onClick(View view) {
		int i = view.getId();
		if (i == R.id.acceptBtn) {
			setResult(RESULT_OK);
			onBackPressed();
		} else if (i == R.id.discardBtn) {
			setResult(RESULT_CANCELED);
			onBackPressed();
		} else if (i == R.id.retakeBtn) {
			setResult(RESULT_CANCELED, new Intent());
			onBackPressed();
		}
	}
}

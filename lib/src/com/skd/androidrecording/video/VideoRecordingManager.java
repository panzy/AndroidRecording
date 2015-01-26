/*
 * Copyright (C) 2013 Steelkiwi Development, Julia Zudikova, Viacheslav Tiagotenkov
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skd.androidrecording.video;

import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;

/*
 * Controls process of previewing and recording video
 */

public class VideoRecordingManager implements SurfaceHolder.Callback {

	private static final String TAG = VideoRecordingManager.class.getSimpleName();

	private AdaptiveSurfaceView videoView;
	private CameraManager cameraManager;
	private MediaRecorderManager recorderManager;
	private VideoRecordingHandler recordingHandler;
	private Thread monitorThrd;
	
	public VideoRecordingManager(AdaptiveSurfaceView videoView, VideoRecordingHandler recordingHandler) {
		this.videoView = videoView;
		this.videoView.getHolder().addCallback(this);
		this.cameraManager = new CameraManager();
		this.recorderManager = new MediaRecorderManager();
		this.recordingHandler = recordingHandler;
	}

	/**
	 *
	 * @param fileName
	 * @param videoSize
	 * @param fileSizeLimit maximum allowed file size in bytes. 0 means infinite.
	 * @param durationLimit maximum duration in seconds, 0 means infinite.
	 * @return
	 */
	public boolean startRecording(final String fileName, Size videoSize,
								  final long fileSizeLimit, final int durationLimit) {
		int degree = cameraManager.getCameraDisplayOrientation();
		boolean startted = recorderManager.startRecording(cameraManager.getCamera(), fileName, videoSize, degree);
		if (startted && (fileSizeLimit > 0 || durationLimit > 0)) {
			monitorThrd = new Thread(new Runnable() {
				@Override
				public void run() {
					long beginTime = System.currentTimeMillis();
					while (!Thread.currentThread().isInterrupted()) {
						try {
							Thread.sleep(1000);
						}
						catch (InterruptedException e) {
							break;
						}

						long fileSize = new File(fileName).length();
						Log.i(TAG, "file size: " + fileName + " : " + fileSize);
						if (fileSizeLimit > 0 && fileSizeLimit < fileSize) {
							Log.i(TAG, "file size limitation reached, stop recording");
							stopRecording(true);
							break;
						}

						if (durationLimit > 0 && durationLimit * 1000 < System.currentTimeMillis() - beginTime) {
							Log.i(TAG, "duration limitation reached, stop recording");
							stopRecording(true);
						}
					}
					monitorThrd = null;
				}
			});
			monitorThrd.start();
		}
		return startted;
	}
	
	public boolean stopRecording(boolean willNotifyHandler) {
		boolean stopped = recorderManager.stopRecording();
		if (stopped && monitorThrd != null && monitorThrd.isAlive()) {
			monitorThrd.interrupt();
			monitorThrd = null;
		}
		if (stopped && willNotifyHandler && recordingHandler != null) {
			recordingHandler.onStoppedRecording();
		}
		return stopped;
	}

	public void setPreviewSize(Size videoSize) {
		videoView.setPreviewSize(videoSize);
	}
	
	public SurfaceHolder getDisplay() {
		return videoView.getHolder();
	}
	
    public CameraManager getCameraManager() {
		return cameraManager;
	}
	
    public void dispose() {
    	videoView = null;
    	cameraManager.releaseCamera();
    	recorderManager.releaseRecorder();
    	recordingHandler = null;
    }
    
    //surface holder callbacks ******************************************************************
    
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
    	cameraManager.openCamera();
	}
    
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (recordingHandler == null) { return; }
		if (!recordingHandler.onPrepareRecording()) {
			cameraManager.setupCameraAndStartPreview(videoView.getHolder(),
												     recordingHandler.getVideoSize(),
												     recordingHandler.getDisplayRotation());
		}
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		recorderManager.stopRecording();
		cameraManager.releaseCamera();
	}
}

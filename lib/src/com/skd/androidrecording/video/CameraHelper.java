/*
 * Copyright (C) 2013 Steelkiwi Development, Julia Zudikova, Viacheslav Tyagotenkov
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Build;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;

/*
 * Represents camera management helper class.
 * Holds method for setting camera display orientation. 
 */

public class CameraHelper {

	private static int[] allowedSizes = {
			// width, height,
			1920, 1080,
			1280, 720,
			720, 480,
			640, 480,
			320, 240
	};

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static int getAvailableCamerasCount() {
		return Camera.getNumberOfCameras();
	}
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static int getDefaultCameraID() {
		int camerasCnt = getAvailableCamerasCount();
		int defaultCameraID = 0;
		CameraInfo cameraInfo = new CameraInfo();
        for (int i=0; i <camerasCnt; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
            	defaultCameraID = i;
            }
        }
        return defaultCameraID;
	}
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static boolean isCameraFacingBack(int cameraID) {
		CameraInfo cameraInfo = new CameraInfo();
		Camera.getCameraInfo(cameraID, cameraInfo);
		return (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK);
	}
	
	@SuppressLint("NewApi")
	public static List<Size> getCameraSupportedVideoSizes(android.hardware.Camera camera) {
		if (camera != null) {
			List<Size> supportedSizes = null;
			if ((Build.VERSION.SDK_INT >= 11))
				supportedSizes = camera.getParameters().getSupportedVideoSizes();
			if (supportedSizes == null)
				supportedSizes = camera.getParameters().getSupportedPreviewSizes();
			return filterVideoSizes(camera, supportedSizes);
		}
		else {
			return null;
		}
	}

	/**
	 * On some devices, not all sizes it declared to support are REALLY supported,
	 * so filter them, preserve only well-known sizes.
	 * @param camera
	 * @param supportedSizes
	 * @return
	 */
	private static List<Size> filterVideoSizes(Camera camera, List<Size> supportedSizes) {
		// Video sizes may be null, which indicates that all the supported
		// preview sizes are supported for video recording.
		StringBuilder allSizesLiteralSB = new StringBuilder();
		for (Size sz : supportedSizes) {
            allSizesLiteralSB.append(String.format("(%dx%d)", sz.width, sz.height));
        }
		String allSizesLiteral = allSizesLiteralSB.toString();

		List<Size> result = new ArrayList<>(allowedSizes.length);
		for (int i = 0; i < allowedSizes.length; i += 2) {
			int w = allowedSizes[i];
			int h = allowedSizes[i + 1];
            if (allSizesLiteral.contains(String.format("(%dx%d)", w, h)))
                result.add(camera.new Size(w, h));
        }

		return result;
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static int setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera, int displayRotation) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int degrees = 0;
		switch (displayRotation) {
			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
		}

		int camRotationDegree = 0;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			camRotationDegree = (info.orientation + degrees) % 360;
			camRotationDegree = (360 - camRotationDegree) % 360; // compensate the mirror
		} else { 
			camRotationDegree = (info.orientation - degrees + 360) % 360;
		}

		if (camera != null) {
			try {
				camera.setDisplayOrientation(camRotationDegree);
			} catch (RuntimeException e) {
				// java.lang.RuntimeException: set display orientation failed
				e.printStackTrace();
			}
		}
		return camRotationDegree;
	}

	public static void setAllowedVideoSizes(int[] sizes) {
		allowedSizes = sizes;
	}

	public static void setAllowedVideoSizes(ArrayList<Integer> sizes) {
		allowedSizes = new int[sizes.size()];
		int i = 0;
		for (Integer value : sizes) {
			allowedSizes[i] = value;
			++i;
		}
	}
}

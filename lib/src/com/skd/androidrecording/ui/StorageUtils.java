package com.skd.androidrecording.ui;

import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class StorageUtils {
	private static final String AUDIO_EXT = "wav";
	private static final String VIDEO_EXT = "3gp";

	public static boolean checkExternalStorageAvailable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
	    }
		else {
			return false;
		}
	}
	
	public static String getFileName(boolean isAudio) {
		File path;
		if (isAudio) {
			path = Environment.getExternalStorageDirectory();
			path = new File(path.getAbsolutePath() + "/Sound");
		} else {
			path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
			path = new File(path.getAbsolutePath() + "/Camera");
		}
		path.mkdirs();

		return String.format("%s/%s.%s",
				path.getAbsoluteFile(),
				new SimpleDateFormat("yyyyMMdd_kkmmss_S").format(Calendar.getInstance().getTime()),
				(isAudio) ? AUDIO_EXT : VIDEO_EXT);
	}
}

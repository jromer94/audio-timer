package com.thems.timer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.thems.timer.capture.AudioCapture;
import com.thems.timer.capture.AudioLevelListener;

public class VolumeMeter extends android.widget.ProgressBar implements AudioLevelListener {
	static final int MY_MSG = 1;
	AudioCapture mCapture;
	int mMaxValue = 0;
	MaxVolumeListener mMaxVolumeListener = null;
	
	public VolumeMeter(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}
	
	public VolumeMeter(Context context) {
		super(context, null, android.R.attr.progressBarStyleHorizontal);		
	}
	
	public void setupHandler(MaxVolumeListener menu) {
		mMaxVolumeListener = menu;
	}
	
	public void setMaxValue(int max) {
		mMaxValue = max;
	}
	
	public void start() {
Log.d("VolumeMeter", "Start");
		AudioCapture.addListener(this);
	}

	public void stop() {
Log.d("VolumeMeter", "Stop");
		AudioCapture.removeListener(this);
	}

	@Override
	public void notifyAudioLevel(int level) {
		mMaxValue = Math.max(mMaxValue, level);
		mMaxVolumeListener.newMaxValue(mMaxValue);
		if (level > 10) {
			Log.d("Handler", "Got Value: " + level + " Max: " + mMaxValue);
		}
		VolumeMeter.this.setProgress(level);
		
	}

}

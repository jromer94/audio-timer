package com.thems.timer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.thems.timer.capture.AudioCapture;

public class MainMenu extends Activity implements VolumeThresholdDialogListener {
	TextView mTextView;
	TextView mMinBufferTextView;
	Button mVolDialogButton;
	Button mStartTimerActivity;
	int mThreshold;
	
	static final int DIALOG_VOLUME_ID = 2;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mTextView = (TextView)findViewById(R.id.ThresholdText);
		setThreshold(0);
		
		mMinBufferTextView = (TextView)findViewById(R.id.MinBufferText);
		mMinBufferTextView.setText("MinBuffer: " + AudioCapture.getInstance().BUFFERSIZE);
		
		mVolDialogButton = (Button)findViewById(R.id.VolumeDialogButton);
		mVolDialogButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MainMenu.this.showDialog(DIALOG_VOLUME_ID);
			}});
		
		mStartTimerActivity = (Button)findViewById(R.id.StartTimerActivityButton);
		mStartTimerActivity.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(MainMenu.this, TimerActivity.class);
				i.putExtra(TimerActivity.VOLUME_THRESHOLD, MainMenu.this.mThreshold);
				startActivity(i);
			}});
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
	@Override
	public void setThreshold(int threshold) {
		mThreshold = threshold;
		mTextView.setText("Threshold: " + Integer.toString(threshold));
	}
	
	public Dialog onCreateDialog(int dialogID) {
		Dialog dialog = null;
		if (dialogID == DIALOG_VOLUME_ID) {
			VolumeThresholdDialog vDialog = new VolumeThresholdDialog(this, this);
			
			dialog = vDialog;
		}
		return dialog;
	}
	
	public void onPrepareDialog(int dialogID, Dialog dialog) {
		if (dialogID == DIALOG_VOLUME_ID && dialog instanceof VolumeThresholdDialog) {
			VolumeThresholdDialog vDialog = (VolumeThresholdDialog)dialog;
			vDialog.start();
		}
	}
}
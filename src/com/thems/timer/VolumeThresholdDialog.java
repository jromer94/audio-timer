package com.thems.timer;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class VolumeThresholdDialog extends Dialog implements MaxVolumeListener {
	SeekBar mSeekBar;
	VolumeMeter volumeMeter;
	TextView mTextView;
	Button mOkButton;
	Button mCancelButton;
	VolumeThresholdDialogListener mReturnListener;

	/*
	 * protected VolumeThresholdDialog(Context context, boolean cancelable,
	 * OnCancelListener cancelListener) { super(context, cancelable,
	 * cancelListener); init(); }
	 */

	public VolumeThresholdDialog(Context context,
			VolumeThresholdDialogListener listener) {
		super(context);
		Log.d("VolumeThresholdDialog", "Constructor");
		mReturnListener = listener;
		init();
	}

	private void init() {
		setContentView(R.layout.volume_threshold_dialog);
		Log.d("VolumeThresholdDialog", "Init");
		LayoutParams params = getWindow().getAttributes();
		params.width = LayoutParams.FILL_PARENT;
		getWindow().setAttributes(
				(android.view.WindowManager.LayoutParams) params);

		mTextView = (TextView) findViewById(R.id.ThresholdText);
		mTextView.setGravity(Gravity.CENTER);

		mOkButton = (Button) findViewById(R.id.OkButton);
		mOkButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				stop();

				VolumeThresholdDialog.this.mReturnListener
						.setThreshold(VolumeThresholdDialog.this.mSeekBar
								.getProgress());
				VolumeThresholdDialog.this.dismiss();
			}

		});
		this.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface arg0) {
				((VolumeThresholdDialog) arg0).stop();

			}
		});
		mCancelButton = (Button) findViewById(R.id.CancelButton);

		mSeekBar = (SeekBar) findViewById(R.id.SeekBar);
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					volumeMeter.setMaxValue(progress);
				}
				mTextView.setText(Integer.toString(mSeekBar.getProgress()));

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// don't care
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// don't care
			}

		});
		// volumeMeter.start();
	}

	public void start() {
		volumeMeter = (VolumeMeter) findViewById(R.id.VolumeMeter);
		volumeMeter.setupHandler(this);
		volumeMeter.setKeepScreenOn(true);
		volumeMeter.start();
	}

	public void stop() {
		if (volumeMeter != null) {
			volumeMeter.stop();
			volumeMeter = null;
		}
	}

	@Override
	public void newMaxValue(int value) {
		int temp = mSeekBar.getProgress();
		if (value > temp) {
			mSeekBar.setProgress(value);
		}
	}
}

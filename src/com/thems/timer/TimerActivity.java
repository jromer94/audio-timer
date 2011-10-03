package com.thems.timer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class TimerActivity extends Activity {
	public static final String VOLUME_THRESHOLD = "VOLUME_THRESHOLD";
	ListView mListView;
	SimpleAdapter mListAdapter;
	List<HashMap<String, String>> mListAdapterData;
	Chronometer mChronoDisplay;
	Button mStartStopResetButton;
	int mThreshold;
	static final int mDefaultThreshold = 50;
	final String laplabel = "Lap";
	final String laptimelabel = "LapTime";
	final String timerlabel = "Timer";
	int lapCounter = 1;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.timer);
		mListView = (ListView)findViewById(R.id.TimerListView);
		mListAdapterData = new ArrayList<HashMap<String, String>>();
		mListAdapter = new SimpleAdapter(this, mListAdapterData, R.layout.laptime, new String[] {laplabel, laptimelabel, timerlabel}, new int[] {R.id.lapno, R.id.laptime, R.id.timertime});
		mListView.setAdapter(mListAdapter);
		mStartStopResetButton = (Button)findViewById(R.id.StartStopTimerButton);
		mStartStopResetButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				HashMap<String, String> newRow = new HashMap<String, String>();
				newRow.put(laplabel, Integer.toString(lapCounter++));
				newRow.put(laptimelabel, "laptime");
				newRow.put(timerlabel, "total");
				mListAdapterData.add(newRow);
				mListAdapter.notifyDataSetChanged();
			}});
		mChronoDisplay = (Chronometer)findViewById(R.id.ChronoDisplay);
		mThreshold = this.getIntent().getIntExtra(VOLUME_THRESHOLD, mDefaultThreshold);
		//TODO: this
		//mListView.getAdapter().
	}

}

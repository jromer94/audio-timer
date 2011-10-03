package com.thems.timer.capture;

import java.util.Collections;
import java.util.Vector;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioCapture extends Thread {
	public int FREQUENCY = 0;
	public int CHANNEL = 0;
	public int ENCODING = 0;
	public int BUFFERSIZE = 0;
	public double MAXAUDIOVALUE = 0;

	private static AudioCapture mInstance = null;

	private volatile boolean mIsRunning = false;
	private static final Vector<AudioLevelListener> mListeners = new Vector<AudioLevelListener>();
	private AudioRecord mRecordInstance;

	private AudioCapture() {
		mIsRunning = false;

		int[] sampleRates = new int[] { 8000, 11025, 22050, 44100 };
		for (int rate : sampleRates) {
			for (short audioFormat : new short[] {
					AudioFormat.ENCODING_PCM_8BIT,
					AudioFormat.ENCODING_PCM_16BIT }) {
				for (short channelConfig : new short[] {
						AudioFormat.CHANNEL_CONFIGURATION_MONO,
						AudioFormat.CHANNEL_CONFIGURATION_STEREO }) {
					try {
						Log.d("Attempting rate", +rate + "Hz, bits: "
								+ audioFormat + ", channel: " + channelConfig);
						int bufferSize = AudioRecord.getMinBufferSize(rate,
								channelConfig, audioFormat);

						if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
							// check if we can instantiate and have a success
							AudioRecord recorder = new AudioRecord(
									MediaRecorder.AudioSource.MIC, rate,
									channelConfig, audioFormat, bufferSize);

							if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
								recorder.stop();
								FREQUENCY = rate;
								CHANNEL = channelConfig;
								ENCODING = audioFormat;
								BUFFERSIZE = bufferSize;
								if (ENCODING == AudioFormat.ENCODING_PCM_8BIT) {
									MAXAUDIOVALUE = (Math.pow(2, 8) - 1) / 2d;
								} else if (ENCODING == AudioFormat.ENCODING_PCM_16BIT) {
									MAXAUDIOVALUE = (Math.pow(2, 16) - 1) / 2d;
								}
							}
						}
					} catch (Exception e) {
						Log.e(rate + "Exception, keep trying.", e.getMessage());
					}
				}
			}
		}
	}

	public static synchronized AudioCapture getInstance() {
		if (mInstance == null) {
			mInstance = new AudioCapture();
		}
		return mInstance;
	}

	protected static void startCapture() {
		AudioCapture capture = getInstance();
		if (!capture.isAlive()) {
			capture.start();
		}
	}

	protected static void stopCapture() {
		AudioCapture capture = getInstance();
		capture.mIsRunning = false;
	}

	public static void addListener(AudioLevelListener listener) {
		synchronized (mListeners) {
			if (mListeners.contains(listener)) {
				Log.i("AudioCapture::addListener", "Listener already in list");
			} else {
				if (!mListeners.add(listener)) {
					Log.e("AudioCapture::addListener", "Add operation failed");
				} else if (mListeners.size() == 1) {
					// if add was successful and this is the first listener,
					// start
					startCapture();
				}
			}
		}
	}

	public static void removeListener(AudioLevelListener listener) {
		synchronized (mListeners) {
			if (!mListeners.contains(listener)) {
				Log.i("AudioCapture::removeListener",
						"Listener not in list to remove");
			} else {
				if (!mListeners.remove(listener)) {
					Log.e("AudioCapture::removeListener",
							"Remove operation failed");
				}
			}
		}
	}

	public void run() {
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		mRecordInstance = new AudioRecord(MediaRecorder.AudioSource.MIC,
				FREQUENCY, CHANNEL, ENCODING, BUFFERSIZE);

		mRecordInstance.startRecording();
		short[] tempBuffer = new short[BUFFERSIZE];
		double localMax;
		int retVal;
		mIsRunning = true;

		while (mIsRunning) {
			localMax = 0.0;

			for (int i = 0; i < BUFFERSIZE - 1; i++) {
				tempBuffer[i] = 0;
			}

			mRecordInstance.read(tempBuffer, 0, BUFFERSIZE);

			for (int i = 0; i < BUFFERSIZE - 1; ++i) {
				localMax = Math.max(Math.abs(tempBuffer[i]), localMax);
			}

			// make it out of 100
			retVal = (int) ((localMax / MAXAUDIOVALUE) * 100);

			// publish value
			synchronized (mListeners) {
				// hopefully remove garbage-collected listeners that have gone
				// out of scope
				// Dialogs don't get garbage collected though =(
				mListeners.removeAll(Collections.singletonList(null));
				if (mListeners.size() > 0) {
					for (AudioLevelListener listener : mListeners) {
						listener.notifyAudioLevel(retVal);
					}
				} else {
					mIsRunning = false;
				}
			}

		}
		mRecordInstance.stop();
		mRecordInstance.release();
		mRecordInstance = null;
		System.gc();
		Log.i("AudioCapture", "Thread stopping");
		mInstance = null;
	}
}

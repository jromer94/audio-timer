package com.thems.timer.capture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
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
	
	private final static Set<Integer> mSampleRates;
	private final static Set<Integer> mAudioFormats;
	private final static Set<Integer> mChannelConfigs;
	
	static {
		mSampleRates = new HashSet<Integer>();
		mSampleRates.add(8000);
		mSampleRates.add(11025);
		mSampleRates.add(22050);
		mSampleRates.add(44100);
		mSampleRates.add(AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM));
		
		mAudioFormats = new HashSet<Integer>();
		mAudioFormats.add(AudioFormat.ENCODING_PCM_8BIT);
		mAudioFormats.add(AudioFormat.ENCODING_PCM_16BIT);
		
		mChannelConfigs = new HashSet<Integer>();
		mChannelConfigs.add(AudioFormat.CHANNEL_CONFIGURATION_MONO);
		mChannelConfigs.add(AudioFormat.CHANNEL_CONFIGURATION_STEREO);
	}
	
	private class AudioConfig implements Comparable<AudioConfig>{
		public int samplerate = 0;
		public int audioformat = 0;
		public int channelconfig = 0;
		public int buffersize = 0;
		
		public double buffersPerSecond = 0;
		public double maxvolume = 0;
		
		@Override
		public int compareTo(AudioConfig arg0) {
			//value minimal delay over all else
			if (this.buffersPerSecond < arg0.buffersPerSecond) {
				return 1;
			} else if (this.buffersPerSecond > arg0.buffersPerSecond) {
				return -1;
			} else {
				return 0;
			}
		}
		
		protected void calculateBuffersPerSecond() {
			double bytesPerSecond = (double)samplerate;
			if (audioformat == AudioFormat.ENCODING_PCM_16BIT) {
				bytesPerSecond *= 2d;
			}
			
			buffersPerSecond = bytesPerSecond / (double)buffersize;			
		}
		
		public void calculateMaxVolume() {
			if (audioformat == AudioFormat.ENCODING_PCM_8BIT) {
				maxvolume = (Math.pow(2, 8) - 1) / 2d;
			} else if (audioformat == AudioFormat.ENCODING_PCM_16BIT) {
				maxvolume = (Math.pow(2, 16) - 1) / 2d;
			}
		}
	}

	private AudioCapture() {
		mIsRunning = false;
		ArrayList<AudioConfig> configurations = new ArrayList<AudioConfig>();
		for (int rate : mSampleRates) {
			for (int audioFormat : mAudioFormats) {
				for (int channelConfig : mChannelConfigs) {
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
								AudioConfig config = new AudioConfig();
								config.audioformat = audioFormat;
								config.channelconfig = channelConfig;
								config.samplerate = rate;
								config.buffersize = bufferSize;
								config.calculateMaxVolume();
								config.calculateBuffersPerSecond();
								configurations.add(config);
							}
						}
					} catch (Exception e) {
						Log.e(rate + "Exception, keep trying.", e.getMessage());
					}
				}
			}
		}
		Collections.sort(configurations);
		Log.d("AudioConfigurations", "Number of valid configs: " + configurations.size());
		if (configurations.size() > 0) {
			AudioConfig theBest = configurations.get(0);
			FREQUENCY = theBest.samplerate;
			CHANNEL = theBest.channelconfig;
			ENCODING = theBest.audioformat;
			BUFFERSIZE = theBest.buffersize;
			MAXAUDIOVALUE = theBest.maxvolume;
			Log.d("AudioConfigurations", "Frequency     : " + FREQUENCY);
			Log.d("AudioConfigurations", "Channel       : " + CHANNEL);
			Log.d("AudioConfigurations", "Encoding      : " + ENCODING);
			Log.d("AudioConfigurations", "BufferSize    : " + BUFFERSIZE);
			Log.d("AudioConfigurations", "MaxAudioValue : " + MAXAUDIOVALUE);
			Log.d("AudioConfigurations", "buffers per s : " + theBest.buffersPerSecond);
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

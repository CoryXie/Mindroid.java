/*
 * Copyright (C) 2012 Daniel Himmelein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mindroid.util.logging;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.Environment;
import mindroid.os.IBinder;
import mindroid.util.Log;
import mindroid.util.logging.LogBuffer.LogRecord;

public class Logger extends Service {
	private static final String LOG_TAG = "Logger";
	private LoggerThread mMainLoggerThread = null;
	private LoggerThread mPerformanceMonitoringLoggerThread = null;

	class LoggerThread extends Thread {
		private LogBuffer mLogBuffer;
		private boolean mConsoleLogging;
		private boolean mFileLogging;
		private boolean mTimestamps;
		private int mPriority;
		private String mLogDirectory;
		private String mLogFileName;
		private int mLogFileSizeLimit;
		private int mLogFileCount;
		private ConsoleHandler mConsoleHander;
		private FileHandler mFileHander;

		public LoggerThread(String name, int logBufferId, boolean consoleLogging, boolean fileLogging, boolean timestamps, int priority, String logDirectory,
				String logFileName, int logFileSizeLimit, int logFileCount) {
			super(name);
			mLogBuffer = Log.getLogBuffer(logBufferId);
			mConsoleLogging = consoleLogging;
			mFileLogging = fileLogging;
			mTimestamps = timestamps;
			mPriority = priority;
			mLogDirectory = logDirectory;
			mLogFileName = logFileName;
			mLogFileSizeLimit = logFileSizeLimit;
			mLogFileCount = logFileCount;
		}

		public void run() {
			open();

			while (!isInterrupted()) {
				LogRecord logMessage;
				try {
					logMessage = mLogBuffer.take(mPriority);
				} catch (InterruptedException e) {
					break;
				}
				if (logMessage != null) {
					if (mConsoleHander != null) {
						mConsoleHander.publish(logMessage);
					}
					if (mFileHander != null) {
						mFileHander.publish(logMessage);
					}
				}
			}

			close();
		}

		private void open() {
			if (mConsoleLogging) {
				mConsoleHander = new ConsoleHandler();
				if (mTimestamps) {
					mConsoleHander.setFlag(ConsoleHandler.FLAG_TIMESTAMP);
				}
			}

			if (mFileLogging) {
				try {
					File logDirectory = new File(mLogDirectory).getAbsoluteFile();
					if (!logDirectory.exists()) {
						logDirectory.mkdir();
					}
					mFileHander = new FileHandler(mLogDirectory + File.separator + mLogFileName, mLogFileSizeLimit, mLogFileCount, true);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		private void close() {
			if (mConsoleHander != null) {
				mConsoleHander.close();
			}

			if (mFileHander != null) {
				mFileHander.close();
			}
		}
	}

	public void onCreate() {
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		int threadPriority = intent.getIntExtra("threadPriority", Thread.MIN_PRIORITY);
		ArrayList logBuffers = intent.getStringArrayListExtra("logBuffers");
		if (logBuffers != null) {
			if (logBuffers.contains("main")) {
				boolean timestamps = intent.getBooleanExtra("timestamps", false);
				int priority = intent.getIntExtra("priority", Log.ERROR);
				String logDirectory = Environment.getLogDirectory().getAbsolutePath();
				String logFileName = intent.getStringExtra("logFileName");
				int logFileSizeLimit = intent.getIntExtra("logFileSizeLimit", 0);
				int logFileCount = intent.getIntExtra("logFileCount", 1);
				boolean fileLogging = (logDirectory != null && logDirectory.length() > 0 && logFileName != null && logFileName.length() > 0);

				mMainLoggerThread = new LoggerThread(LOG_TAG + " {main}", Log.LOG_ID_MAIN, true, fileLogging, timestamps, priority, logDirectory, logFileName,
						logFileSizeLimit, logFileCount);
				mMainLoggerThread.setPriority(threadPriority);
				mMainLoggerThread.start();
			}

			if (logBuffers.contains("perfmon")) {
				if (mPerformanceMonitoringLoggerThread == null) {
					mPerformanceMonitoringLoggerThread = new LoggerThread(LOG_TAG + " {perfmon}", Log.LOG_ID_PERFORMANCE_MONITORING, true, false, true,
							Log.DEBUG, null, null, 0, 0);
					mPerformanceMonitoringLoggerThread.setPriority(threadPriority);
					mPerformanceMonitoringLoggerThread.start();
				}
			}
		}

		return 0;
	}

	public void onDestroy() {
		if (mMainLoggerThread != null) {
			mMainLoggerThread.interrupt();
			try {
				mMainLoggerThread.join();
			} catch (InterruptedException e) {
			}
		}

		if (mPerformanceMonitoringLoggerThread != null) {
			mPerformanceMonitoringLoggerThread.interrupt();
			try {
				mPerformanceMonitoringLoggerThread.join();
			} catch (InterruptedException e) {
			}
		}
	}

	public IBinder onBind(Intent intent) {
		return null;
	}
}

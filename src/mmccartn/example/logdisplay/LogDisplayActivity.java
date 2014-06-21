package mmccartn.example.logdisplay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * This activity demonstrates how to capture and display Android 
 * logcat messages from within an app… for the debugger-on-the-go
 * 
 * Intended for use on Android 4.3+, which means that the read_logs 
 * permission is not required and that the app can only capture its 
 * own logs: http://stackoverflow.com/a/16795874 
 * 
 * Partially inspired by: 
 * http://code.google.com/p/android-random/source/browse/#svn/trunk/Logcat
 * 
 * Logcat clearing, filtering, and other features can be found here: 
 * https://developer.android.com/tools/help/logcat.html
 * 
 * Licensed under GPLv3
 * 
 * @author mmccartn
 */
public class LogDisplayActivity extends Activity {

	public static final int LOG_LIMIT = 4096;
	public static final String TAG = "LogDisplay";
	public static final String LOGCATCMD = "logcat";
	
	private final StringBuilder logText = new StringBuilder("");
	
	private boolean isFocused = true;
	private boolean isStarted = false;
	private boolean isPaused = true;
	private Thread logThread;
	private Process logProcess;
	private ScrollView logScroll;
	private TextView logTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.log_display_activity);

		logTextView = (TextView) findViewById(R.id.log_text);
		
		logScroll = ((ScrollView) findViewById(R.id.log_scroll));
				
		startLogger();
		
		final Button doButton = (Button) findViewById(R.id.do_button);
		doButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d(TAG, "Do button pressed.");
				String d;
				if (isStarted) {
					stopLogger();
					d = "Start";					
				} else {
					startLogger();					
					d = "Stop";
				}
				doButton.setText(d);
			}
		});
		
		final Button focusButton = (Button) findViewById(R.id.focus_button);
		focusButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d(TAG, "Focus button pressed.");
				isFocused = !isFocused;
				String f = isFocused ? "Focused" : "Focus";
				focusButton.setText(f);							
			}
		});
		
	}
	
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    stopLogger();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		isPaused = true;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		isPaused = false;
	}
	
	private void startLogger() {
		if (!isStarted) {
			logThread = new Thread(new Runnable() {
				@Override
				public void run() {
					Log.d(TAG, "Logger begin.");
					try {
						logProcess = Runtime.getRuntime().exec(LOGCATCMD);
						BufferedReader bufferedReader = new BufferedReader(
								new InputStreamReader(logProcess.getInputStream()));
						String line;
						while (isStarted && (line = bufferedReader.readLine()) != null) {
							writeline(line);
						}
						logProcess.destroy();
					} catch (IOException e) {
						Log.e(TAG, "Logger failed: " + e.getMessage());
					}
					Log.d(TAG, "Logger end.");
				}
			}, "Logger_Thread");
			logThread.start();		
			isStarted = true;
		}
	}
	
	private void stopLogger() {
		if (isStarted) {
			isStarted = false;		
			try {
				logThread.join();
			} catch (InterruptedException e) {}			
		}
	}
	
	private synchronized void writeline(String msg) {
		logText.append(msg).append("\n");		
		if (logText.length() > LOG_LIMIT) {
			logText.delete(0, logText.length() - LOG_LIMIT);
		}
		if (!isPaused) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					logTextView.setText(logText.toString());
					logScroll.post(new Runnable() {
						@Override
						public void run() {
							if (isFocused) {
								logScroll.fullScroll(View.FOCUS_DOWN);							
							}
						}
					});
				}
			});
		}
	}
}

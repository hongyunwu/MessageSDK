package com.why.messagesystem;

import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.why.message.PosterThread;
import com.why.message.Message;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "MainActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

	}

	@Override
	protected void onResume() {
		super.onResume();
		PosterThread posterThread = new PosterThread(){
			@Override
			public boolean accept(Message message) {
				Log.i(TAG, "accept: "+message);
				return false;
			}

			@Override
			public void onSendFailed(Message message, int reasonCode) {
				super.onSendFailed(message, reasonCode);
			}
		};
		posterThread.start();
		posterThread.getThreadHandler().postSyncBarrier();
		for (int i = 0; i < 100; i++) {
			Message message = Message.obtain();
			message.what = i%3;
			message.cookie = i;
			if (i%9==0){
				message.setSynchronous(false);
			}
			posterThread.getThreadHandler().sendMessageAtLevel(message,i%5);
		}

		SystemClock.sleep(5000);
		posterThread.getThreadHandler().removeSyncBarrier();

	}
}

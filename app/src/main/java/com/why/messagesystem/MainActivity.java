package com.why.messagesystem;

import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.why.message.Carrier;
import com.why.message.PosterThread;

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
			public boolean accept(Carrier message) {
				Log.i(TAG, "accept: "+message);
				return false;
			}

			@Override
			public void onSendFailed(Carrier message, int reasonCode) {
				super.onSendFailed(message, reasonCode);
			}
		};
		posterThread.start();
		posterThread.getThreadHandler().postSyncBarrier();
		for (int i = 0; i < 100; i++) {
			Carrier carrier = Carrier.obtain();
			carrier.what = i%3;
			carrier.cookie = i;
			if (i%9==0){
				carrier.setSynchronous(false);
			}
			posterThread.getThreadHandler().sendMessageAtLevel(carrier,i%5);
		}

		SystemClock.sleep(5000);
		posterThread.getThreadHandler().removeSyncBarrier();

	}
}

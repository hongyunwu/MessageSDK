package com.why.message;

import android.util.Log;

/**
 * Created by android_wuhongyun@163.com
 * on 2018/11/16.
 * 消息的轮询者
 */
 public class Looper {

	private static final String TAG = "Looper";
 	//当前looper轮询的队列
	final MessageQueue mQueue;

	//当前looper所在线程
	public Thread mThread;

	static final ThreadLocal<Looper> sThreadLocal= new ThreadLocal<>();

	private Looper(){
		mQueue = new MessageQueue();
		mThread = Thread.currentThread();
	}
	public static void loop(){
		Looper looper = Looper.myLooper();
		if (looper==null){
			throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
		}
		MessageQueue mQueue = looper.mQueue;
		for (;;){
			Message message = mQueue.next();
			if (message==null){
				break;
			}
			message.target.dispatchMessage(message);
			message.recycle();
		}
		Log.w(TAG, "loop: 退出------");
	}

	public static Looper myLooper() {
		return sThreadLocal.get();
	}

	static MessageQueue myQueue(){
		return myLooper().mQueue;
	}

	public static void prepare() {
		if (sThreadLocal.get() != null) {
			throw new RuntimeException("Only one Looper may be created per thread");
		}
		sThreadLocal.set(new Looper());
	}

	boolean isCurrentThread(){
		return Thread.currentThread()==mThread;
	}

	public void quit(){
		mQueue.quit(false);
	}

	public void quitSafely(){
		mQueue.quit(true);
	}

}

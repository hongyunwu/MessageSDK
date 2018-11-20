package com.why.message;

/**
 * Created by android_wuhongyun@163.com
 * on 2018/11/20.
 */

public class HandlerThread extends Thread implements Handler.HandlerCallback{

	private Looper mLooper;
	private Handler mHandler;

	@Override
	public void run() {
		super.run();
		Looper.prepare();
		synchronized (this){
			mLooper = Looper.myLooper();
			notifyAll();
		}
		onLooperPrepared();
		Looper.loop();


	}

	public void onLooperPrepared() {
		//
	}

	/**
	 * 阻塞式的调用
	 * @return
	 */
	public Looper getLooper(){
		if (!isAlive()){
			return null;
		}
		synchronized (this) {
			while (isAlive()&&mLooper==null){
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}
		return mLooper;
	}

	/**
	 * 阻塞式调用
	 * @return
	 */
	public Handler getThreadHandler(){
		if (mHandler==null){
			mHandler = new Handler(getLooper(),this);
		}
		return mHandler;
	}

	public boolean quit(boolean safely){
		Looper looper = getLooper();
		if (looper!=null){
			if (safely){
				looper.quitSafely();
			}else {
				looper.quit();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean accept(Message message) {
		return false;
	}

	@Override
	public void onSendFailed(Message message, int reasonCode) {

	}
}

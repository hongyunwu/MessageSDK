package com.why.message;

/**
 * Created by android_wuhongyun@163.com
 * on 2018/11/20.
 */

public class PosterThread extends Thread implements Poster.HandlerCallback{

	private Recycler mRecycler;
	private Poster mPoster;

	@Override
	public void run() {
		super.run();
		Recycler.prepare();
		synchronized (this){
			mRecycler = Recycler.myLooper();
			notifyAll();
		}
		onLooperPrepared();
		Recycler.loop();


	}

	public void onLooperPrepared() {
		//
	}

	/**
	 * 阻塞式的调用
	 * @return
	 */
	public Recycler getLooper(){
		if (!isAlive()){
			return null;
		}
		synchronized (this) {
			while (isAlive()&& mRecycler ==null){
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}
		return mRecycler;
	}

	/**
	 * 阻塞式调用
	 * @return
	 */
	public Poster getThreadHandler(){
		if (mPoster ==null){
			mPoster = new Poster(getLooper(),this);
		}
		return mPoster;
	}

	public boolean quit(boolean safely){
		Recycler recycler = getLooper();
		if (recycler !=null){
			if (safely){
				recycler.quitSafely();
			}else {
				recycler.quit();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean accept(Carrier carrier) {
		return false;
	}

	@Override
	public void onSendFailed(Carrier carrier, int reasonCode) {

	}
}

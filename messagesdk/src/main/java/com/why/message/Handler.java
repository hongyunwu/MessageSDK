package com.why.message;


import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.why.message.contract.IAccept;
import com.why.message.contract.ISend;
import com.why.message.contract.ISyncBarrierCallback;
import com.why.message.contract.IdleHandler;

/**
 * Created by android_wuhongyun@163.com
 * on 2018/11/16.
 */

public class Handler implements IAccept,ISend {

	private static final String TAG = "Handler";
	//消息轮询器
	Looper mLooper;

	//消息队列
	MessageQueue mQueue;

	//回调接口
	HandlerCallback mCallback;

	/***************************构造方法***************************/

	public Handler(){
		this(null,null);
	}

	public Handler( HandlerCallback callback){
		this(null,callback);
	}

	public Handler(Looper looper){
		this(looper,null);
	}

	public Handler(@Nullable Looper looper, @Nullable HandlerCallback callback){

		if (looper==null){
			looper = Looper.myLooper();
		}
		if (looper==null){
			throw new RuntimeException(
					"Can't create handler inside thread that has not called Looper.prepare()");
		}
		this.mLooper = looper;
		this.mQueue = mLooper.mQueue;
		this.mCallback = callback;
	}

	/***************************接收方*****************************/
	@Override
	public void accept(Message message) {
		//可在此接收并处理消息
	}

	/**************************发送方*******************************/
	/**
	 *发送失败回调，应予以重写
	 * @param message 发送失败的消息
	 * @param reasonCode 发送失败的原因 1.消息为空，2.消息非法-无target，3.消息已被使用，4.队列已退出
	 */
	@CallSuper
	public void onSendFailed(Message message, int reasonCode) {
		if (mCallback!=null){
			mCallback.onSendFailed(message,reasonCode);
		}
	}

	@Override
	public final void sendMessage(Message message) {
		sendMessageAtLevel(message,1);
	}

	@Override
	public final void sendMessageAtLevel(Message message, int level) {
		if (level<1){
			level = 1;
		}
		message.level = level;
		enqueueMessage(message);
	}


	@Override
	public final void sendEmptyMessage(int what) {
		Message message = Message.obtain();
		message.what = what;
		sendMessageAtLevel(message,1);
	}

	@Override
	public final void sendEmptyMessageAtLevel(int what, int level) {
		Message message = Message.obtain();
		message.what = what;
		sendMessageAtLevel(message,level);
	}

	@Override
	public final void post(Runnable runnable) {
		Message message = Message.obtain();
		message.callback = runnable;
		sendMessageAtLevel(message,1);
	}

	@Override
	public final void postAtLevel(Runnable runnable, int level) {
		Message message = Message.obtain();
		message.callback = runnable;
		sendMessageAtLevel(message,level);
	}

	public final void removeMessages(int what){
		removeMessages(what,null);
	}

	public final boolean hasMessages(int what){
		return hasMessages(what,null);
	}

	public final void removeMessages(int what,Object cookie){
		mQueue.removeMessages(this,what,cookie);
	}

	public final void removeRunnables(Runnable runnable){
		mQueue.removeRunnables(this,runnable);
	}

	public final boolean hasMessages(int what,Object cookie){

		return mQueue.hasMessages(this,what,cookie);
	}

	public final boolean hasRunnables(Runnable runnable){
		return mQueue.hasMessages(this,runnable);
	}
	/**
	 * 阻塞所有消息
	 */
	public final void postSyncBarrier(){
		mQueue.postSyncBarrier(1,mSyncBarrierCallback);
	}


	/**
	 * 用于阻塞level及以上的消息
	 *
	 * @param level
	 */
	public final void postSyncBarrier(int level){
		if (level<1){
			level = 1;
		}
		mQueue.postSyncBarrier(level,mSyncBarrierCallback);
	}

	public final void removeSyncBarrier(){

		mQueue.removeSyncBarrier(mSyncBarrierCallback);
	}

	public final void addIdleHandler(IdleHandler idleHandler){
		mQueue.addIdleHandler(idleHandler);
	}

	public final void removeIdleHandler(IdleHandler idleHandler){
		mQueue.removeIdleHandler(idleHandler);
	}

	ISyncBarrierCallback mSyncBarrierCallback = new ISyncBarrierCallback() {
		@Override
		public void onSyncBarrier(int level) {
			Log.d(TAG, "onSyncBarrier: "+level);
		}

		@Override
		public void onRemoveSyncBarrier(int level) {
			Log.d(TAG, "onRemoveSyncBarrier: "+level);
		}
	};

	/****************************私有方法***************************/
	private void enqueueMessage(Message message) {
		//所有关于此消息的细节都需在此前
		message.target = this;
		int result = mQueue.enqueueMessage(message);
		if (result!=0){
			Log.e(TAG, "enqueueMessage: 消息投递失败");
			onSendFailed(message,result);
		}
	}



	/**
	 * 分发消息
	 *
	 * @param message 被分发的消息体
	 */
	void dispatchMessage(Message message){
		if (message.callback!=null){
			message.callback.run();
		}else {
			if (mCallback!=null&&mCallback.accept(message)){
				return;
			}
			accept(message);
		}
	}


	/****************************接口******************************/
	public interface HandlerCallback{
		boolean accept(Message message);
		void onSendFailed(Message message,int reasonCode);
	}

}

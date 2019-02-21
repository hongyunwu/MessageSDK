package com.why.message;

import android.os.SystemClock;
import android.util.Log;

import com.why.message.contract.ISyncBarrierCallback;
import com.why.message.contract.IdleHandler;

import java.util.ArrayList;

/**
 * Created by android_wuhongyun@163.com
 * on 2018/11/16.
 * 需要被轮询的消息队列
 */

public class MessageQueue {

	private static final String TAG = "MessageQueue";
	//消息队列--->单链表
	Carrier mMessages;

	//队列操作锁
	Object mWRLock = new Object();

	//队列wait/notify锁
	Object mWNLock = new Object();

	//标识当前队列是否退出
	boolean mQuitting;

	//是否支持安全退出
	private boolean mSafely = true;

	//是否阻塞
	private boolean mBlocked = false;

	ArrayList<IdleHandler> mIdleHandlers = new ArrayList<>();

	MessageQueue() {
	}

	public int enqueueMessage(Carrier carrier) {
		/**
		 * 1.消息是否有target
		 * 2.消息是否已经循环过
		 * 3.检查队列是否还存在
		 * 4.唤醒消息队列notify
		 */
		int result = checkValidity(carrier);
		if (result !=0) return result;
		Log.d(TAG, "enqueueMessage: "+ carrier);
		synchronized (mWRLock) {
			carrier.when = SystemClock.uptimeMillis();
			//根据level来决定当前消息所在位置
			Carrier local = mMessages;
			if (local == null || local.level > carrier.level) {
				carrier.next = local;
				mMessages = carrier;
			} else {
				Carrier prev;
				for (; ; ) {
					prev = local;
					local = local.next;
					if (local == null || local.level > carrier.level) {

						break;
					}
				}
				prev.next = carrier;
				carrier.next = local;
			}
			carrier.makeInUse(true);
		}
		//唤醒队列
		if (mBlocked){
			try {
				synchronized (mWNLock) {
					mWNLock.notify();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	int checkValidity(Carrier carrier) {
		if (carrier == null) {
			Log.w(TAG, "enqueueMessage: 入队消息为空");
			return 1;
		}
		if (carrier.target == null) {
			Log.w(TAG, "enqueueMessage: 非法消息,target=null");
			return 2;
		}
		if (carrier.isInUse()) {
			Log.w(TAG, "enqueueMessage: 当前消息正处于使用中，无法投送");
			return 3;
		}
		if (mQuitting) {
			Log.w(TAG, "enqueueMessage: 当前消息队列已退出");
			return 4;
		}
		return 0;
	}

	public Carrier next() {

		for (; ; ) {
			if (mQuitting){
				if (!mSafely){
					Log.w(TAG, "next: 不安全退出...");
					return null;
				}
			}
			//等待被唤醒
			try {
				if (mBlocked){
					synchronized (mWNLock) {
						Log.d(TAG, "next: wait...");
						mWNLock.wait();
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			synchronized (mWRLock) {
				Carrier carrier = mMessages;
				Carrier prev = null;
				if (carrier != null && carrier.isSyncBarrier()) {
					do {
						prev = carrier;
						carrier = carrier.next;
					} while (carrier != null && !carrier.isAsynchronous());
				}
				//取出当前msg处理
				if (carrier != null) {
					if (prev != null) {
						prev.next = carrier.next;
					} else {
						mMessages = carrier.next;
					}
					carrier.next = null;
					carrier.makeInUse(true);
					mBlocked = false;
					return carrier;
				} else {
					if(size()==0){
						synchronized (mIdleHandlers) {
							for (IdleHandler idleHandler : mIdleHandlers) {
								idleHandler.queueIdle();
							}
						}
					}

					if (mQuitting){
						Log.i(TAG, "next: 安全退出...");
						return null;
					}
					mBlocked = true;
				}

			}
		}
	}

	/**
	 * 消息队列退出
	 *
	 * @param safely true表示消息队列中的消息运行完了之后在退出，false表示强行退出。
	 */
	public void quit(boolean safely) {
		mQuitting = true;
		mSafely = safely;
		//需要注意syncBarrier
		removeSyncBarrier(null);
	}


	/**
	 * 同一队列中只允许有一个SyncBarrier，如post多次则之前的将被remove
	 *
	 * @param level level以上的同步消息都被阻塞掉
	 * @param callback 栅栏加入时的回调
	 */
	void postSyncBarrier(int level, ISyncBarrierCallback callback) {
		/**
		 * 1.先删除之前的栅栏
		 * 2.添加level级的同步栅栏
		 */
		synchronized (mWRLock){
			Carrier local = mMessages;
			//先去掉链头
			while (local!=null&&local.target==null){
				Carrier n = local.next;
				mMessages = n;
				if (callback!=null){
					callback.onRemoveSyncBarrier(local.level);
				}
				local.recycle();
				local = n;
			}
			while (local!=null){
				Carrier n = local.next;
				if (n!=null){
					if (n.target==null){
						Carrier nn = n.next;
						if (callback!=null){
							callback.onRemoveSyncBarrier(n.level);
						}
						n.recycle();
						local.next = nn;
						continue;
					}
				}
				local = n;
			}
			local = mMessages;
			Carrier syncBarrier = Carrier.obtain();
			syncBarrier.level = level;
			syncBarrier.when = SystemClock.uptimeMillis();
			syncBarrier.makeInUse(true);
			if (local == null || local.level >= syncBarrier.level) {
				syncBarrier.next = local;
				mMessages = syncBarrier;
			} else {
				Carrier prev;
				for (; ; ) {
					prev = local;
					local = local.next;
					if (local == null || local.level >= syncBarrier.level) {
						break;
					}
				}
				prev.next = syncBarrier;
				syncBarrier.next = local;
			}
			if (callback!=null){
				callback.onSyncBarrier(level);
			}
		}
	}

	public void removeSyncBarrier(ISyncBarrierCallback callback) {
		synchronized (mWRLock){
			Carrier local = mMessages;
			//先去掉链头
			while (local!=null&&local.target==null){
				Carrier n = local.next;
				mMessages = n;
				if (callback!=null){
					callback.onRemoveSyncBarrier(local.level);
				}
				local.recycle();
				local = n;
			}
			while (local!=null){
				Carrier n = local.next;
				if (n!=null){
					if (n.target==null){
						Carrier nn = n.next;
						if (callback!=null){
							callback.onRemoveSyncBarrier(n.level);
						}
						n.recycle();
						local.next = nn;
						continue;
					}
				}
				local = n;
			}
		}
		if (mBlocked){
			try {
				synchronized (mWNLock) {
					mWNLock.notify();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public boolean hasMessages(Poster poster, int what, Object cookie) {

		if (poster == null) {
			return false;
		}
		synchronized (mWRLock) {
			Carrier local = mMessages;
			while (local != null) {
				if (local.target == poster && local.what == what && local.cookie == cookie) {
					return true;
				}
				local = local.next;
			}
		}
		return false;
	}

	public boolean hasMessages(Poster poster, Runnable runnable) {

		if (poster == null) {
			return false;
		}
		synchronized (mWRLock) {
			Carrier local = mMessages;
			while (local != null) {
				if (local.target == poster && local.callback == runnable) {
					return true;
				}
				local = local.next;
			}
		}
		return false;
	}


	public int size() {
		int size = 0;
		synchronized (mWRLock) {
			Carrier local = mMessages;
			while (local != null) {
				size++;
				local = local.next;
			}
		}
		return size;
	}

	public void removeMessages(Poster poster, int what, Object cookie) {

		if (poster == null) {
			return;
		}
		synchronized (mWRLock) {
			Carrier local = mMessages;
			while (local != null && local.target == poster && local.what == what
					&& (local.cookie == cookie)) {
				Carrier n = local.next;
				mMessages = n;
				local.recycle();
				local = n;
			}
			while (local != null) {
				Carrier n = local.next;
				if (n != null) {
					if (n.target == poster && n.what == what
							&& (n.cookie == cookie)) {
						Carrier nn = n.next;
						n.recycle();
						local.next = nn;
						continue;
					}
				}
				local = n;
			}
		}
	}

	public void removeRunnables(Poster poster, Runnable runnable) {
		if (poster == null) {
			return;
		}
		synchronized (mWRLock) {
			Carrier local = mMessages;
			while (local != null && local.target == poster && local.callback == runnable) {
				Carrier n = local.next;
				mMessages = n;
				local.recycle();
				local = n;
			}
			while (local != null) {
				Carrier n = local.next;
				if (n != null) {
					if (n.target == poster && n.callback == runnable) {
						Carrier nn = n.next;
						n.recycle();
						local.next = nn;
						continue;
					}
				}
				local = n;
			}
		}

	}

	public void addIdleHandler(IdleHandler idleHandler) {
		synchronized (mIdleHandlers) {
			if (!mIdleHandlers.contains(idleHandler)) {
				mIdleHandlers.add(idleHandler);
			}
		}
	}

	public void removeIdleHandler(IdleHandler idleHandler) {
		synchronized (mIdleHandlers) {
			if (mIdleHandlers.contains(idleHandler)) {
				mIdleHandlers.remove(idleHandler);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Carrier carrier = mMessages;
		synchronized (mWRLock) {
			while (carrier !=null){
				sb.append(carrier);
				carrier = carrier.next;
			}
		}
		return sb.toString();
	}
}

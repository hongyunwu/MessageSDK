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
	Message mMessages;

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

	public int enqueueMessage(Message message) {
		/**
		 * 1.消息是否有target
		 * 2.消息是否已经循环过
		 * 3.检查队列是否还存在
		 * 4.唤醒消息队列notify
		 */
		int result = checkValidity(message);
		if (result !=0) return result;
		Log.d(TAG, "enqueueMessage: "+message);
		synchronized (mWRLock) {
			message.when = SystemClock.uptimeMillis();
			//根据level来决定当前消息所在位置
			Message local = mMessages;
			if (local == null || local.level > message.level) {
				message.next = local;
				mMessages = message;
			} else {
				Message prev;
				for (; ; ) {
					prev = local;
					local = local.next;
					if (local == null || local.level > message.level) {

						break;
					}
				}
				prev.next = message;
				message.next = local;
			}
			message.makeInUse(true);
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

	int checkValidity(Message message) {
		if (message == null) {
			Log.w(TAG, "enqueueMessage: 入队消息为空");
			return 1;
		}
		if (message.target == null) {
			Log.w(TAG, "enqueueMessage: 非法消息,target=null");
			return 2;
		}
		if (message.isInUse()) {
			Log.w(TAG, "enqueueMessage: 当前消息正处于使用中，无法投送");
			return 3;
		}
		if (mQuitting) {
			Log.w(TAG, "enqueueMessage: 当前消息队列已退出");
			return 4;
		}
		return 0;
	}

	public Message next() {

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
				Message message = mMessages;
				Message prev = null;
				if (message != null && message.isSyncBarrier()) {
					do {
						prev = message;
						message = message.next;
					} while (message != null && !message.isAsynchronous());
				}
				//取出当前msg处理
				if (message != null) {
					if (prev != null) {
						prev.next = message.next;
					} else {
						mMessages = message.next;
					}
					message.next = null;
					message.makeInUse(true);
					mBlocked = false;
					return message;
				} else {
					synchronized (mIdleHandlers) {
						for (IdleHandler idleHandler : mIdleHandlers) {
							idleHandler.queueIdle();
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
			Message local = mMessages;
			//先去掉链头
			while (local!=null&&local.target==null){
				Message n = local.next;
				mMessages = n;
				if (callback!=null){
					callback.onRemoveSyncBarrier(local.level);
				}
				local.recycle();
				local = n;
			}
			while (local!=null){
				Message n = local.next;
				if (n!=null){
					if (n.target==null){
						Message nn = n.next;
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
			Message syncBarrier = Message.obtain();
			syncBarrier.level = level;
			syncBarrier.when = SystemClock.uptimeMillis();
			syncBarrier.makeInUse(true);
			if (local == null || local.level >= syncBarrier.level) {
				syncBarrier.next = local;
				mMessages = syncBarrier;
			} else {
				Message prev;
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
			Message local = mMessages;
			//先去掉链头
			while (local!=null&&local.target==null){
				Message n = local.next;
				mMessages = n;
				if (callback!=null){
					callback.onRemoveSyncBarrier(local.level);
				}
				local.recycle();
				local = n;
			}
			while (local!=null){
				Message n = local.next;
				if (n!=null){
					if (n.target==null){
						Message nn = n.next;
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

	public boolean hasMessages(Handler handler, int what, Object cookie) {

		if (handler == null) {
			return false;
		}
		synchronized (mWRLock) {
			Message local = mMessages;
			while (local != null) {
				if (local.target == handler && local.what == what && local.cookie == cookie) {
					return true;
				}
				local = local.next;
			}
		}
		return false;
	}

	public boolean hasMessages(Handler handler, Runnable runnable) {

		if (handler == null) {
			return false;
		}
		synchronized (mWRLock) {
			Message local = mMessages;
			while (local != null) {
				if (local.target == handler && local.callback == runnable) {
					return true;
				}
				local = local.next;
			}
		}
		return false;
	}

	public void removeMessages(Handler handler, int what, Object cookie) {

		if (handler == null) {
			return;
		}
		synchronized (mWRLock) {
			Message local = mMessages;
			while (local != null && local.target == handler && local.what == what
					&& (local.cookie == cookie)) {
				Message n = local.next;
				mMessages = n;
				local.recycle();
				local = n;
			}
			while (local != null) {
				Message n = local.next;
				if (n != null) {
					if (n.target == handler && n.what == what
							&& (n.cookie == cookie)) {
						Message nn = n.next;
						n.recycle();
						local.next = nn;
						continue;
					}
				}
				local = n;
			}
		}
	}

	public void removeRunnables(Handler handler, Runnable runnable) {
		if (handler == null) {
			return;
		}
		synchronized (mWRLock) {
			Message local = mMessages;
			while (local != null && local.target == handler && local.callback == runnable) {
				Message n = local.next;
				mMessages = n;
				local.recycle();
				local = n;
			}
			while (local != null) {
				Message n = local.next;
				if (n != null) {
					if (n.target == handler && n.callback == runnable) {
						Message nn = n.next;
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
		Message message = mMessages;
		synchronized (mWRLock) {
			while (message!=null){
				sb.append(message);
				message = message.next;
			}
		}
		return sb.toString();
	}
}

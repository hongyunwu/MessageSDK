package com.why.message;

import android.os.Bundle;
import android.os.SystemClock;

import java.util.ArrayList;

/**
 * Created by android_wuhongyun@163.com
 * on 2018/11/16.
 * 消息
 */

public class Message {

	//链表中下条消息
	Message next;

	//消息携带数据
	public Object cookie;

	//消息的处理者
	Poster target;

	//消息携带数据(K-V)
	Bundle data;

	//标识消息投送时间-辅助信息
	long when;

	//用于标识消息是否已经循环过。
	boolean used = false;

	//消息的优先级
	public long level;

	//消息类型 据FLAG区分为同步消息和异步消息
	int flag;

	//用于标识消息类型,需要大于0
	public int what;

	//消息处理内容
	Runnable callback;

	//标识此消息是一个同步消息
	public static final int FLAG_SYNCHRONOUS = 1 << 0;

	//标识此消息是一个异步消息
	public static final int FLAG_ASYNCHRONOUS = 1 << 1;

	static MessagePool mPool = new MessagePool();

	Message(){
		init();
	}

	private void init() {
		next = null;
		cookie = null;
		target = null;
		data = null;
		when = SystemClock.uptimeMillis();
		used = false;
		level = 0;
		flag = FLAG_SYNCHRONOUS;
		what = 0;
		callback = null;
	}

	public void makeInUse(boolean used){
		this.used = used;
	}

	public void setData(Bundle data){
		this.data = data;
	}

	public Bundle getData(){
		return data;
	}

	/**
	 * 从消息池中取出recycle的消息
	 *
	 * @return
	 */
	public static Message obtain() {
		Message message = mPool.get();
		return message;
	}


	public void setSynchronous(boolean synchronous){
		if (synchronous){
			flag = FLAG_SYNCHRONOUS;
		}else {
			flag = FLAG_ASYNCHRONOUS;
		}
	}

	public void recycle(){
		init();
		mPool.put(this);
	}

	public boolean isInUse() {
		return used;
	}

	public boolean isSyncBarrier() {
		return target==null;
	}

	public boolean isAsynchronous() {

		return flag==FLAG_ASYNCHRONOUS;
	}

	static class MessagePool{
		private ArrayList<Message> messages = new ArrayList<>();
		Object mPoolLock = new Object();
		public Message get() {
			synchronized (mPoolLock){
				if (messages.size()>0){
					return messages.remove(0);
				}
			}
			return new Message();
		}

		public void put(Message message) {
			synchronized (mPoolLock){
				if (message!=null)
					messages.add(message);
			}
		}
	}

	@Override
	public String toString() {
		return "Message{" +
				"cookie=" + cookie +
				", target=" + target +
				", data=" + data +
				", when=" + when +
				", used=" + used +
				", level=" + level +
				", flag=" + flag +
				", what=" + what +
				", callback=" + callback +
				'}';
	}
}

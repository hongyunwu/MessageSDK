package com.why.message.contract;

import com.why.message.Message;

/**
 * Created by android_wuhongyun@163.com
 * on 2018/11/16.
 */

public interface ISend {

	/**
	 * default level 1
	 *
	 * @param message 需要发送的消息
	 */
	void sendMessage(Message message);

	/**
	 *
	 * @param message 需要发送的消息体
	 * @param level 必须大于1，不然以默认1论
	 */
	void sendMessageAtLevel(Message message,int level);

	/**
	 * default level 1;
	 *
	 * @param what 标识消息类型
	 */
	void sendEmptyMessage(int what);

	/**
	 * @param what 空消息类型
	 * @param level 空消息级别
	 */
	void sendEmptyMessageAtLevel(int what,int level);

	void post(Runnable runnable);

	void postAtLevel(Runnable runnable,int level);


}

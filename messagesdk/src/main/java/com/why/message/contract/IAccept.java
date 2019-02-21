package com.why.message.contract;

import com.why.message.Carrier;

/**
 * Created by android_wuhongyun@163.com
 * on 2018/11/16.
 */

public interface IAccept {

	/**
	 * 收到消息，需在此方法中对此消息进行具体处理
	 *
	 * @param carrier
	 */
	void accept(Carrier carrier);
}

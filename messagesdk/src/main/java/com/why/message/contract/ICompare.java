package com.why.message.contract;

import com.why.message.Carrier;

/**
 * Created by android_wuhongyun@163.com
 * on 2018/11/16.
 */

public interface ICompare {

	/**
	 *
	 * @param msg1
	 * @param msg2
	 * @return default int>0则，msg1>msg2; int=0,msg1=msg2; int<0,msg<msg2。
	 */
	int compare(Carrier msg1, Carrier msg2);

}

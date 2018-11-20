package com.why.message.contract;

/**
	 * 消息队列空闲时接口
	 */
	public interface IdleHandler{
		void queueIdle();
	}
package com.why.message.contract;

public interface ISyncBarrierCallback {

		void onSyncBarrier(int level);

		void onRemoveSyncBarrier(int level);
	}
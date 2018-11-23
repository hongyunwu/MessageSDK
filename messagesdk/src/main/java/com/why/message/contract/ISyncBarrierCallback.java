package com.why.message.contract;

public interface ISyncBarrierCallback {

		void onSyncBarrier(long level);

		void onRemoveSyncBarrier(long level);
	}
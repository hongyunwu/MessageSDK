# MessageSDK
消息执行机制，模仿的Handler+Looper+MessageQueue方式，把消息执行顺序由when转换为level优先级来可阻塞式执行。

```
PosterThread posterThread = new PosterThread(){
			@Override
			public boolean accept(Carrier message) {
				Log.i(TAG, "accept: "+message);
				return false;
			}

			@Override
			public void onSendFailed(Carrier message, int reasonCode) {
				super.onSendFailed(message, reasonCode);
			}
		};
		posterThread.start();
		posterThread.getThreadHandler().postSyncBarrier();
		for (int i = 0; i < 100; i++) {
			Carrier carrier = Carrier.obtain();
			carrier.what = i%3;
			carrier.cookie = i;
			if (i%9==0){
				carrier.setSynchronous(false);
			}
			posterThread.getThreadHandler().sendMessageAtLevel(carrier,i%5);
		}

		SystemClock.sleep(5000);
		posterThread.getThreadHandler().removeSyncBarrier();
```

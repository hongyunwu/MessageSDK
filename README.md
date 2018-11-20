# MessageSDK
消息执行机制，模仿的Handler+Looper+MessageQueue方式，把消息执行顺序由when转换为level优先级来可阻塞式执行。

```
HandlerThread handlerThread = new HandlerThread(){
			@Override
			public boolean accept(Message message) {
				Log.i(TAG, "accept: "+message);
				return false;
			}

			@Override
			public void onSendFailed(Message message, int reasonCode) {
				super.onSendFailed(message, reasonCode);
			}
		};
		handlerThread.start();
		handlerThread.getThreadHandler().postSyncBarrier();
		for (int i = 0; i < 100; i++) {
			Message message = Message.obtain();
			message.what = i%3;
			message.cookie = i;
			if (i%9==0){
				message.setSynchronous(false);
			}
			handlerThread.getThreadHandler().sendMessageAtLevel(message,i%5);
		}

		SystemClock.sleep(5000);
		handlerThread.getThreadHandler().removeSyncBarrier();
```

package com.android.facecard.app;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ThreadPool {
	ThreadPoolExecutor mExecutor;
	private static final int CORE_POOL_SIZE = 4;
	private static final int MAX_POOL_SIZE = 8;
	private static final int KEEP_ALIVE_TIME = 10; // 10 seconds

	public ThreadPool() {
		mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE,
				KEEP_ALIVE_TIME, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory(
						"thread-pool",
						android.os.Process.THREAD_PRIORITY_BACKGROUND));
	}

	public static interface Job<T> {
		public T run();
	}

	public static class Worker<T> implements Runnable, Future<T> {
		private T mResult;
		Job<T> mJob;
		FutureListener<T> mListener;

		public Worker(Job<T> job, FutureListener<T> listener) {
			mJob = job;
			mListener = listener;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {

			return false;
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			while (!mDone) {
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			return mResult;
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException,
				ExecutionException, TimeoutException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isCancelled() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isDone() {
			// TODO Auto-generated method stub
			return false;
		}

		boolean mDone = false;

		@Override
		public void run() {
			T result = mJob.run();
			synchronized (this) {
				mResult = result;
				mDone = true;
				notifyAll();
			}
			mListener.onExecuteFinish(result);
		}
	}

	public <T> Future<T> submit(Job<T> job, FutureListener<T> listener) {
		Worker<T> w = new Worker<T>(job, listener);
		mExecutor.execute(w);
		return w;
	}

	public static interface FutureListener<T> {
		public void onExecuteFinish(T result);
	}
}

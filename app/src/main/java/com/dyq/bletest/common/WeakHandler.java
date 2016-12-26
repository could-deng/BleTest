package com.dyq.bletest.common;

import android.os.Handler;

import java.lang.ref.WeakReference;

public class WeakHandler extends Handler {
	@SuppressWarnings("rawtypes")
	private WeakReference mInstance;
	private boolean bDiscardMsg = false;

	public void setDiscardMsgFlag(boolean bDiscard) {
		bDiscardMsg = true;
		if(mInstance != null)mInstance.clear();
		mInstance = null;
	}

	public <T> WeakHandler(T instance) {
		mInstance = new WeakReference<>(instance);
		bDiscardMsg = false;
	}

	protected Object getRefercence() {
		if(bDiscardMsg || (mInstance == null))return null;
		return  mInstance.get();
	}

}

/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.dyq.bletest.common.heartRate;


import android.bluetooth.BluetoothDevice;

public interface HRSManagerCallbacks extends BleManagerCallbacks {

	/**
	 * Called when the sensor position information has been obtained from the sensor
	 * 
	 * @param position
	 *            the sensor position
	 */
	void onHRSensorPositionFound(String position);

	/**
	 * Called when new Heart Rate value has been obtained from the sensor
	 * 
	 * @param value
	 *            the new value
	 */
	void onHRValueReceived(BluetoothDevice device,int value);

	/** 耳机是否脱落及信号强度
	 * @param ifDrop 是否脱落
	 * @param signalValue	信号强度
	 */
	void onSignalValueReceived(BluetoothDevice device,boolean ifDrop, int signalValue);

	/**
	 * @param btnType	按键类型
	 * @param msgType	消息类型
	 */
	void onPushBtnReceived(BluetoothDevice device,int btnType, int msgType);

//	/**	当发送心率耳机消息指令成功后执行的方法
//	 */
//	void onCmdSendSuccess();
	void onLAVAHRReceive(BluetoothDevice device,int current_hr,int avg_hr, int min_hr, int max_hr,int light_intensity);

	void onSportDataReceive(BluetoothDevice device,int sportMode, int stepBPM, int distance, int totalStep, int speed, int vo2, int calBurnRate, int totalCal, int maxVo2);
}

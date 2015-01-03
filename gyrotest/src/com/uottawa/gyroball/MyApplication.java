package com.uottawa.gyroball;

import android.app.Application;

public class MyApplication extends Application {

    private BluetoothGameService bluetoothService;

	public BluetoothGameService getBluetoothService() {
		return bluetoothService;
	}

	public void setBluetoothService(BluetoothGameService bluetoothService) {
		this.bluetoothService = bluetoothService;
	}
    
}
package com.uottawa.gyroball;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

public class MainActivity extends Activity {

    private GyroVisualizer mGyroView;
    private SensorManager mSensorManager;
    private Sensor mAccSensor;
    private BluetoothGameService bluetoothService;
    private boolean singleMode;
	private WakeLock wl;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // get
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
	            PowerManager.ACQUIRE_CAUSES_WAKEUP, "MainActivity"); 
		wl.acquire();
       Bundle extras = getIntent().getExtras();
       
       String gameMode = extras.getString("gameMode");
       if ( gameMode != null && gameMode.equals("single")) {
    	   singleMode = true;
       }
       
       if (!singleMode) {
    	   bluetoothService = ((MyApplication) this.getApplication()).getBluetoothService();
           bluetoothService.setmHandler(mHandler);
            
            String value = "";
            String message = "";
            if (extras != null) {
            	value = extras.getString("isLeader");
            	//message = extras.getString("message");
            }
            if (value.equals("false")) {
            	mGyroView = new GyroVisualizer(this, false, false, bluetoothService);
            }
            else {
            	mGyroView = new GyroVisualizer(this, true, false, bluetoothService);
            }
       }
       else {
    	   mGyroView = new GyroVisualizer(this, true, true, bluetoothService);
       }
      

        
        setContentView(mGyroView);
        
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private SensorEventListener mAccListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            float x = values[0];
            float y = values[1];

            mGyroView.setAcceleration(-x, y);
        }
    };
    
    @Override
    protected void onResume() {
    	 bluetoothService = ((MyApplication) this.getApplication()).getBluetoothService();
         if(bluetoothService != null) {
        	 bluetoothService.setmHandler(mHandler);
         }
        super.onResume();
        mSensorManager.registerListener(mAccListener, mAccSensor, SensorManager.SENSOR_DELAY_UI);
    }
    
	@Override
	public void onPause()
	{
		if ( wl.isHeld() )
		{
			wl.release();
		}
		//mGyroView.setActive(false);
		//Toast.makeText(getApplicationContext(), "inside on Pause", Toast.LENGTH_SHORT).show();
		//if (bluetoothService != null) bluetoothService.stop();
		super.onPause();
	}
    
    // The Handler that gets information back from the BluetoothGameService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	//Toast.makeText(getApplicationContext(), "state change = " + msg.what , Toast.LENGTH_SHORT).show();
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                
                case BluetoothGameService.STATE_CONNECTED:
                	//Toast.makeText(this, " You are not connected", Toast.LENGTH_SHORT).show();
                	//Toast.makeText(getApplicationContext(), "Status=STATE_CONNECTED", Toast.LENGTH_SHORT).show();
                	
                    break;
                case BluetoothGameService.STATE_CONNECTING:
                    //setStatus(R.string.title_connecting);
                	//Toast.makeText(getApplicationContext(), "Status=STATE_CONNECTING", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothGameService.STATE_LISTEN:
                	//Toast.makeText(getApplicationContext(), "Status=STATE_LISTEN", Toast.LENGTH_SHORT).show();
                case BluetoothGameService.STATE_NONE:
                	//Toast.makeText(getApplicationContext(), "Status=STATE_NONE", Toast.LENGTH_SHORT).show();
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                ((MyApplication) getApplication()).setBluetoothService(bluetoothService);
              //  Toast.makeText(getApplicationContext(), "msg sent", Toast.LENGTH_SHORT).show();

                String writeMessage = new String(writeBuf);
               // myIntent.putExtra("isLeader", "true");
            	//startActivity(myIntent);
                //mGyroView.setActive(false);
                break;
            case MESSAGE_READ:
            	mGyroView.setActive(true);
            //    Toast.makeText(getApplicationContext(), "msg received", Toast.LENGTH_SHORT).show();

                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String message = new String(readBuf);
                String[] array = message.split(",");
            	float x = Float.parseFloat(array[0]);
            	float y = Float.parseFloat(array[1]);
            	
            	float previousWidth = Float.parseFloat(array[4]);
            	float previousHeight = Float.parseFloat(array[5]);
                x = mGyroView.getWidth() * x;
                y = mGyroView.getHeight() * y;

            	mGyroView.setPreviousX(x);
            	mGyroView.setPreviousY(y);
            	mGyroView.setVelocityX(Float.parseFloat(array[2]) / 2);
            	mGyroView.setVelocityY(Float.parseFloat(array[3]) / 2);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                break;
            case MESSAGE_TOAST:
                
                break;
            }
        }
    };
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
		//Toast.makeText(getApplicationContext(), "inside Destroy", Toast.LENGTH_SHORT).show();
		
        if (bluetoothService != null) bluetoothService.stop();
    }
    
    // Message types sent from the BluetoothGameService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
}
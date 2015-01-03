package com.uottawa.gyroball;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothGame extends Activity {
    /** Called when the activity is first created. */
    //ListView listViewPaired;
    //ArrayList<String> arrayListpaired;
    //ArrayList<BluetoothDevice> arrayListPairedBluetoothDevices;
    private boolean isLeader;
    ListView listViewAvailable;
    ArrayList<String> arrayListAvailable;
    ArrayList<BluetoothDevice> arrayListAvailableBluetoothDevices;
    
    ArrayAdapter<String> pairedListAdapter;
    ArrayAdapter<String> availableListAdapter;
    BluetoothDevice bdDevice;
    BluetoothClass bdClass;
    ListItemClickedonPaired listItemClickedonPaired;

    BluetoothAdapter bluetoothAdapter = null;
    BluetoothGameService bluetoothService;
    
    // Message types sent from the BluetoothGameService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    private static final int REQUEST_ENABLE_BT = 3;
    
    // Key names received from the BluetoothGameService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    @Override 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        listViewPaired = (ListView) findViewById(R.id.listViewPaired);
//        arrayListpaired = new ArrayList<String>();
//        arrayListPairedBluetoothDevices = new ArrayList<BluetoothDevice>();
        
        listViewAvailable = (ListView) findViewById(R.id.listViewAvailable);
        arrayListAvailable = new ArrayList<String>();
        arrayListAvailableBluetoothDevices = new ArrayList<BluetoothDevice>();
        
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);

        this.registerReceiver(mReceiver, filter1);
        this.registerReceiver(mReceiver, filter2);
        
        listItemClickedonPaired = new ListItemClickedonPaired();

        availableListAdapter = new ArrayAdapter<String>(BluetoothGame.this, android.R.layout.simple_list_item_1, arrayListAvailable);
        getPairedDevices();
        getAvailableDevices();
        listViewAvailable.setAdapter(availableListAdapter);
        listViewAvailable.setOnItemClickListener(listItemClickedonPaired);
        
//        pairedListAdapter = new ArrayAdapter<String>(BluetoothGame.this, android.R.layout.simple_list_item_1, arrayListpaired);
        
//        listViewPaired.setAdapter(pairedListAdapter);
//        listViewPaired.setOnItemClickListener(listItemClickedonPaired);

        // set

       
    }
    
    @Override
    public void onStart() {
       // isLeader = false;
       // set dialog message
		AlertDialog.Builder alert = new AlertDialog.Builder(BluetoothGame.this);

		alert.setTitle("Game Mode");
		alert.setMessage("Please select your Game Mode:");
		
//		// Set an EditText view to get user input 
//		final EditText input = new EditText(BluetoothGame.this);
//		alert.setView(input);

		alert.setPositiveButton("Single", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  //String value = input.getText().toString();
			Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
			myIntent.putExtra("gameMode", "single");
         	startActivity(myIntent);	
         	dialog.cancel();
		  	try {
		  		
		  	} catch ( Exception ex) {
		  		Toast.makeText(getApplicationContext(), " exception in sendMessage is " + ex.getMessage(), Toast.LENGTH_SHORT).show();
		  	}
		  }
		});

		alert.setNegativeButton("Multi", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {

		        if (!bluetoothAdapter.isEnabled()) {
		            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		        // Otherwise, setup the chat session
		        } else {
		        	 // Initialize the BluetoothChatService to perform bluetooth connections
		        	bluetoothService = new BluetoothGameService(BluetoothGame.this, mHandler);
		        	try {
		        		bluetoothService.start();
		        	} catch ( Exception ex) {
		        		Toast.makeText(getApplicationContext(), "Exception in start " + ex.getMessage() ,Toast.LENGTH_SHORT).show();
		        	}
		        }
			  dialog.cancel();
		  }
		});

		alert.show();
		
        super.onStart();

       
    }
    
    @Override
    public synchronized void onResume() {
        super.onResume();
        isLeader = false;
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (bluetoothService != null) {
        	bluetoothService.setmHandler(mHandler);
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (bluetoothService.getState() == BluetoothGameService.STATE_NONE) {
              // Start the Bluetooth chat services
            	bluetoothService.start();
            }
        }
    }

    
    private void getPairedDevices() {
        Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();            
        if(pairedDevice.size()>0)
        {
        	//arrayListPairedBluetoothDevices.clear();
            for(BluetoothDevice device : pairedDevice)
            {
                arrayListAvailable.add(device.getName());
                arrayListAvailableBluetoothDevices.add(device);
            }
        }
        availableListAdapter.notifyDataSetChanged();
    }
   
    private void getAvailableDevices() {
    	// If we're already discovering, stop it
        if (bluetoothAdapter.isDiscovering()) {
        	bluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        bluetoothAdapter.startDiscovery();
    }
    class ListItemClickedonPaired implements OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
        	String deviceName = ((TextView) view).getText().toString();
        	int index = arrayListAvailable.indexOf(deviceName);
        	
        	bdDevice = arrayListAvailableBluetoothDevices.get(index);
//        	if(arrayListAvailable.contains(deviceName)) {
//        		bdDevice = arrayListAvailableBluetoothDevices.get(position);
//        	}
//        	else {
//        		bdDevice = arrayListPairedBluetoothDevices.get(position);
//        	}
            isLeader = true;

            // Attempt to connect to the device
            bluetoothService.connect(bdDevice);
           
        }
    }
    

    // The Handler that gets information back from the BluetoothGameService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
        	
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                
                case BluetoothGameService.STATE_CONNECTED:
                	//Toast.makeText(this, " You are not connected", Toast.LENGTH_SHORT).show();
                	//Toast.makeText(getApplicationContext(), "Status=STATE_CONNECTED", Toast.LENGTH_SHORT).show();
                	((MyApplication) getApplication()).setBluetoothService(bluetoothService);

                    myIntent.putExtra("isLeader", "" + isLeader);
                	startActivity(myIntent);
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
                
            	
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                //((MyApplication) getApplication()).setBluetoothService(bluetoothService);

       
                //myIntent.putExtra("isLeader", "false");
           
            	//startActivity(myIntent);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
               // Toast.makeText(getApplicationContext(), "You are Connected to " +  msg.getData().getString(DEVICE_NAME) + "! Initiating the game....", Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (bluetoothService != null) bluetoothService.stop();
    }
  //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
             // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
               // Toast.makeText(getApplicationContext(), "Found Device " + device.getName() , Toast.LENGTH_LONG).show();
                
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    arrayListAvailableBluetoothDevices.add(device);
                    arrayListAvailable.add(device.getName());
                    availableListAdapter.notifyDataSetChanged();
                }
            // When discovery is finished, change the Activity title
            } 
            else if ((BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))) {
            	BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
               // Toast.makeText(getApplicationContext(), "connected to " + device.getName(), Toast.LENGTH_LONG).show();  
            }
            
               
        }
    };
}
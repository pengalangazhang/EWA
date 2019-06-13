package com.example.usbconnectiontest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Iterator;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {
    static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private UsbManager manager;
    private PendingIntent permissionIntent;
    private UsbDevice device;

    private TextView text;
    private LinearLayout lView;


    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ( ACTION_USB_PERMISSION.equals(action) ) {
                synchronized (this) {
                    device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if ( intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) ) {
                        if ( device != null ) {
                            //text.setText("permission granted for device");
                            Intent intent1 = new Intent(getApplicationContext(), Connect.class);
                            startActivity(intent1);
                        }
                    }
                    else {
                        text.setText("permission denied for device");
                    }
                }
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lView = new LinearLayout(this);
        text = new TextView(this);
        text.setText("Connect a device");
        lView.addView(text);
        setContentView(lView);
        Log.d(TAG, "Start");
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION),0);
        IntentFilter filter = new IntentFilter(MainActivity.ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while ( deviceIterator.hasNext() ) {
            device = deviceIterator.next();
            manager.requestPermission(device, permissionIntent);
            text.setText("requesting permission");
        }
    }
}

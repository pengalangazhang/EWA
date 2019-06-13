package com.example.accessorymode;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private UsbManager manager;
    private PendingIntent permissionIntent;
    private UsbAccessory accessory;

    private TextView text;
    private LinearLayout lView;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ( ACTION_USB_PERMISSION.equals(action) ) {
                synchronized (this) {
                    if ( intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false ) ) {
                        if (accessory != null) {

                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(receiver, filter);
        UsbAccessory[] accessoryList = manager.getAccessoryList();
        for(int i = 0; i < accessoryList.length; i++) {
            accessory = accessoryList[i];
            manager.requestPermission(accessory, permissionIntent);
        }

    }
}

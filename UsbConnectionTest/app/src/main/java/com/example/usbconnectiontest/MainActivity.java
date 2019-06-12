package com.example.usbconnectiontest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private boolean received;
    PendingIntent permissionIntent = null;
    UsbManager manager = null;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if ( ACTION_USB_PERMISSION.equals(action) ) {
                synchronized(this) {
                    if ( intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) ) {
                        received = true;
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        received = false;
        if (received) {
            Intent intent = new Intent(this, DisplayActivity.class);
            startActivity(intent);
        }
    }
}

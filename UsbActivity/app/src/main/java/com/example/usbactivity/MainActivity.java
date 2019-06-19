package com.example.usbactivity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity{

    private AccessoryCommunicator communicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        communicator = new AccessoryCommunicator(this) {
            @Override
            public void onReceive(byte[] payload, int length) {
                Log.d("HOST", new String(payload, 0, length));
            }

            @Override
            public void onConnected() {
                Log.d("ACCESSORY", "connected");
            }

            @Override
            public void onDisconnected() {
                Log.d("ACCESSORY", "disconnected");
            }
        };
    }

    protected void sendString(String string) {
        communicator.send(string.getBytes());
    }
}

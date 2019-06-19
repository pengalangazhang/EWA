package com.example.usbactivity;

import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public abstract class AccessoryCommunicator {

    private UsbManager manager;
    private Context context;
    private Handler handler;
    private ParcelFileDescriptor fileDescriptor;
    private FileInputStream inputStream;
    private FileOutputStream outputStream;
    private boolean running;

    public AccessoryCommunicator(Context context) {
        this.context = context;
        manager = (UsbManager)(this.context.getSystemService(Context.USB_SERVICE));
        UsbAccessory[] accessories = manager.getAccessoryList();
        if (manager.getDeviceList().size() > 0) {
            Log.d("DEVICES","FOUND");
        }
        else {
            Log.d("DEVICES", "none");
        }

        if (accessories == null || accessories.length == 0) {
            Log.d("ACCESSORIES", "no accessory found");
        }
        else {
            openAccessory(accessories[0]);
        }
    }

    public void send(byte[] payload) {
        if (handler != null) {
            Message message = handler.obtainMessage();
            message.obj = payload;
            handler.sendMessage(message);
        }
    }

    private void receive(byte[] payload, int length) {
        onReceive(payload, length);
    }

    public abstract void onReceive(byte[] payload, int length);
    public abstract void onConnected();
    public abstract void onDisconnected();

    private class CommunicationThread extends Thread {
        public void run() {
            running = true;

            while (running) {
                byte[] message = new byte[256];
                try {
                    int length = inputStream.read(message);
                    while (inputStream != null && length > 0 && running) {
                        receive(message, length);
                        Thread.sleep(10);
                        length = inputStream.read(message);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();

                }
            }
        }
    }

    private void openAccessory(UsbAccessory accessory) {
        fileDescriptor = manager.openAccessory(accessory);
        if ( fileDescriptor != null ) {
            FileDescriptor fd = fileDescriptor.getFileDescriptor();
            inputStream = new FileInputStream(fd);
            outputStream = new FileOutputStream(fd);

            new CommunicationThread().start();

            handler = new Handler() {
                public void handleMessage(Message msg) {
                    try {
                        outputStream.write((byte[])msg.obj);
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            onConnected();
        }
        else {
            Log.d("ACCESSORIES", "Could not connect");
        }
    }

    public void closeAccessory() {
        running = false;

        try {
            if (fileDescriptor != null) {
                fileDescriptor.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            fileDescriptor = null;
        }
        onDisconnected();
    }
}

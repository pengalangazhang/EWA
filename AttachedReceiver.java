package com.example.aoaconnect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;

public class AttachedReceiver extends BroadcastReceiver implements Runnable{
    private UsbManager manager;
    private UsbAccessory accessory;

    private UsbInterface inface;
    private UsbInterface controlInface;
    private UsbEndpoint controlEp;
    private UsbEndpoint outEp;
    private UsbEndpoint inEp;

    private UsbDeviceConnection connection;
    private byte[] buffer;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("USB: ", "detected");
        Intent i = new Intent(context, MainActivity.class);
        context.startActivity(i);
        openAccessory(context, intent);
    }

    public void openAccessory(Context context, Intent intent){
        manager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
        accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        HashMap<String, UsbDevice> devices = manager.getDeviceList();
        UsbDevice deviceAccessory = devices.get(UsbManager.EXTRA_ACCESSORY);
        controlInface = deviceAccessory.getInterface(0);
        controlEp = controlInface.getEndpoint(0);
        //find appropriate interface and endpoints; Main Assumption = deviceAccessory and accessory are essentially same just different data types
        boolean found = false;
        for(int i = 0; i <  deviceAccessory.getInterfaceCount(); i++){
            if(found) break;
            UsbInterface infaceTemp = deviceAccessory.getInterface(i);
            if(infaceTemp.getEndpointCount() > 0) {
                for (int j = 0; j < infaceTemp.getEndpointCount(); j++){
                    UsbEndpoint epTemp = infaceTemp.getEndpoint(i);
                    if (epTemp.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK){
                        if(epTemp.getDirection() == UsbConstants.USB_DIR_OUT)
                            outEp = epTemp;
                        else
                            inEp = epTemp;
                    }
                }
                if(outEp != null && inEp != null) {
                    inface = infaceTemp;
                    found = true;
                    break;
                }
            }

        }
        connection = manager.openDevice(deviceAccessory);

        Thread thread = new Thread(null, this, "AccessoryThread");
        thread.start();
        }

    @Override
    public void run(){
       //control data transfer to determine desirability of communication
        connection.claimInterface(controlInface,true);
        buffer = new byte[1];
        buffer[0] = (byte)accessory.hashCode();
        int status = connection.bulkTransfer(controlEp, buffer, buffer.length, 5000);
        if(status < 0)
            Log.d("TAG", "Error happened!");
        else if(status == 0)
            Log.d("TAG", "No data transferred!");
        else
            Log.d("TAG", "Hashcode transferred from accessory to dev board!");
        Log.d("TAG", accessory.hashCode()+"");

        //terminate communication
        connection.releaseInterface(controlInface);
        connection.close();
    }


}

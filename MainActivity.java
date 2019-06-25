package com.example.aoaconnect;

import androidx.appcompat.app.AppCompatActivity;

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
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    /*
    TODO: Test connection and control/bulk transmission w/ HiKey development board
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Thread worker = new Thread(new USBConnection(this, this.getIntent());
        worker.start();
    }


    class USBConnection implements Runnable {
        private Context context;
        private UsbManager manager;
        private UsbAccessory accessory;
        private HashMap<String, UsbDevice> devices;

        private UsbInterface inface;
        private UsbEndpoint outEp;
        private UsbEndpoint inEp;

        private UsbDeviceConnection connection;
        private byte[] controlBuffer;
        private byte[] outBuffer;
        private byte[] inBuffer;

        private USBConnection(Context context, Intent intent) {
            this.context = context;
            manager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
            accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
            devices = manager.getDeviceList();
            inface = null;
            outEp = null;
            inEp = null;
            connection = null;
            controlBuffer = new byte[64];
            outBuffer = new byte[64];
            inBuffer = new byte[64];
        }

        public void run() {
            UsbDevice deviceAccessory = devices.get(UsbManager.EXTRA_ACCESSORY);
            connection = manager.openDevice(deviceAccessory);
            //check accessory mode support
            boolean accessoryMode = false;
            int counter = 0;
            while (!accessoryMode && counter < 2) {
                counter++;
                if (((deviceAccessory.getVendorId() == 0x18D1) && (deviceAccessory.getProductId() == 0x2D00 || deviceAccessory.getProductId() == 0x2D01)))
                    accessoryMode = true;
                else {
                    //attempt to start in accessory mode
                    if (deviceAccessory.getDeviceProtocol() > 0)
                        connection.controlTransfer(64, 52, 0, Integer.parseInt(accessory.getSerial()), controlBuffer, controlBuffer.length, 5000);
                        connection.controlTransfer(64, 53, 0, 0, null, 0, 5000);
                }
            }


            //set up connection

            //bulk interface and endpoints
            boolean found = false;
            for (int i = 0; i < deviceAccessory.getInterfaceCount(); i++) {
                UsbInterface infaceTemp = deviceAccessory.getInterface(i);
                if (infaceTemp.getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA && infaceTemp.getEndpointCount() > 0) {
                    for (int j = 0; j < infaceTemp.getEndpointCount(); j++) {
                        UsbEndpoint epTemp = infaceTemp.getEndpoint(i);
                        if (epTemp.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (epTemp.getDirection() == UsbConstants.USB_DIR_OUT)
                                outEp = epTemp;
                            else
                                inEp = epTemp;
                        }
                        if (outEp != null && inEp != null) {
                            inface = infaceTemp;
                            found = true;
                            break;
                        }
                    }//inner
                    if (found) break;

                }//condition
            }//outer


            boolean infaceClaimed = connection.claimInterface(inface, true);
            if (infaceClaimed)
                Log.d("Inface Claim: ", "Success");
            else
                Log.d("Inface Claim: ", "Error");

            //bulk transfer 1: android device -> host
            int bulkStatusOut = connection.bulkTransfer(outEp, outBuffer, outBuffer.length, 5000);
            if (bulkStatusOut > 0)
                Log.d("Bulk Transfer Out: ", "successful");
            //bulk transfer 2: host -> android device
            int bulkStatusIn = connection.bulkTransfer(inEp, inBuffer, inBuffer.length, 5000);
            if (bulkStatusIn > 0)
                Log.d("Bulk Transfer In: ", "successful");


        //terminate connection
        BroadcastReceiver usbReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (accessory != null && action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
                    connection.releaseInterface(inface);
                    connection.close();
                }
            }
        };
    }//end run()


    }//end USBConnection


}//end MainActivity


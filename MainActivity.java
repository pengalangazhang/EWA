package com.example.aoaconnect;

import androidx.appcompat.app.AppCompatActivity;
import org.usb4java.*;
import android.os.Bundle;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("onCreate","Began");
        Thread worker = new Thread(new UsbConnection());
        worker.start();
    }

/*
TODO: Tweak control transfers sending hardware metadata and request 53
      Perform bulk transfer
 */
    class UsbConnection implements Runnable {
        private Device device; //HiKey board
        private UsbConnection()throws LibUsbException {
            device = null;
            int result = LibUsb.init(null);
            if (result != LibUsb.SUCCESS)
                throw new LibUsbException("Unable to initialize libusb(create new session)", result);
        }

        public void run() throws LibUsbException{
            //find HiKey board
            DeviceList list = new DeviceList();
            int result = LibUsb.getDeviceList(null, list);
            if (result < 0)
                throw new LibUsbException("Unable to get device list", result);
            try {
                for (Device d : list) {
                    DeviceDescriptor descriptor = new DeviceDescriptor();
                    result = LibUsb.getDeviceDescriptor(d, descriptor);
                    if (result != LibUsb.SUCCESS)
                        throw new LibUsbException("Unable to read device descriptor", result);
                    //google board
                    if (descriptor.idVendor() == 0x18D1)
                        device = d;
                }
                if(device == null)
                    throw new LibUsbException("Unable to find device",-1);
            }
            catch(Exception e){
                e.printStackTrace();
            }
            int [] testData = {21,22,23,24};
            ByteBuffer b = ByteBuffer.wrap(new byte[64]);
            DeviceDescriptor descriptor = new DeviceDescriptor();
            LibUsb.getDeviceDescriptor(device,descriptor);
            //get device handle
            DeviceHandle handle = new DeviceHandle();
            result = LibUsb.open(device,handle);
            if(result != LibUsb.SUCCESS)
                throw new LibUsbException("Unable to open USB device", result);
            //check accessory mode support
            boolean accessoryMode = false;
            int counter = 0;
            while (!accessoryMode && counter < 2) {
                counter++;
                if (((descriptor.idVendor() == 0x18D1) && (descriptor.idProduct() == 0x2D00 || descriptor.idProduct() == 0x2D01)))
                    accessoryMode = true;
                else {
                    //attempt to start in usb mode (if supports,force reintroduction in accessory mode)
                    //first send manufacturer,model,descriptipn,version,URI,serial number
                    if (descriptor.bDeviceProtocol() > 0)
                        //host metadata
                        LibUsb.controlTransfer(handle,(byte)0b00000010, (byte)52, (short)testData[0], (short)0,b,5000);
                        //add more metadata here
                        LibUsb.controlTransfer(handle, (byte)0b00000010, (byte)53, (short)0,(short)0,null,5000);
                }
            }
            //get device bulk interface and endpoints via configuration descriptor
            ConfigDescriptor configDescriptor = new ConfigDescriptor();
           LibUsb.getActiveConfigDescriptor(device,configDescriptor);
           Interface [] infaces = configDescriptor.iface();
           Interface inface = null;
           InterfaceDescriptor altSetting= null;
           EndpointDescriptor epOutBulk= null;
           EndpointDescriptor epInBulk = null;
           boolean found = false;
           for(int i = 0; i < infaces.length; i++) {
               Interface infaceTemp = infaces[i];
               InterfaceDescriptor[] altSettings = infaceTemp.altsetting();
              for(int j = 0; j < altSettings.length; j++){
                  InterfaceDescriptor altSettingTemp = altSettings[j];
                  EndpointDescriptor[] endpoints = altSettingTemp.endpoint();
                  for(int k = 0; k < endpoints.length; k++){
                      EndpointDescriptor epTemp = endpoints[i];
                      if(epTemp.bDescriptorType() == LibUsb.TRANSFER_TYPE_BULK) {
                          if (epTemp.bEndpointAddress() == LibUsb.ENDPOINT_OUT)
                              epOutBulk = epTemp;
                          else
                              epInBulk = epTemp;
                      }
                  }
                  if(epOutBulk != null && epInBulk != null) {
                      inface = infaceTemp;
                      altSetting = altSettingTemp;
                      found = true;
                      Log.d("Endpoint status: ", "All found");
                  }
                 if(found)
                     break;
              }
           }

         //bulk transfer (send data and read from HiKey)
           LibUsb.claimInterface(handle,inface.hashCode());
           ByteBuffer buffer = ByteBuffer.allocateDirect(8);
            buffer.put(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
            IntBuffer transfered = IntBuffer.allocate(1);
           result = LibUsb.bulkTransfer(handle,(byte)0x03,buffer,transfered, 5000);
            if (result != LibUsb.SUCCESS)
                throw new LibUsbException("Control transfer failed: ", -1);
            System.out.println(transfered.get() + " bytes sent");
            //clean up
        LibUsb.releaseInterface(handle,inface.hashCode());
        LibUsb.freeDeviceList(list,true);
        LibUsb.close(handle);
        LibUsb.exit(null);
                }//end run()


    }//end USBConnection


}//end MainActivity


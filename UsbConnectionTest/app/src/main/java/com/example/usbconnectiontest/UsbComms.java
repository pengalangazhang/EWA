package com.example.usbconnectiontest;

import android.hardware.usb.UsbDevice;

public class UsbComms {
    private UsbDevice device;

    public UsbComms(UsbDevice device) {
        this.device = device;
    }
}

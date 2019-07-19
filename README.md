# Android Open Accessory Protocol
## LibUsb / Usb4Java
Testing with a Hikey960, the board needed compatible drivers to be installed both before and after starting accessory mode. This means that **both interfaces** in accessory mode need the **same driver** to be installed. Libusbk or libusb-win32 work.
## Accessory Mode
To start the android device in accessory mode, the host sends 3 control transfers with different request types:
* 51 - Asks which version the device supports
* 52 - Identifying information
* 53 - Starts accessory mode

Afterwards, the device vendor id and product id should be Google's.

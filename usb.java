import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.usb4java.BufferUtils;
import org.usb4java.ConfigDescriptor;
import org.usb4java.Context;
import org.usb4java.DescriptorUtils;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.EndpointDescriptor;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

public class usb {
	
	private final static byte USB_DIR_IN = (byte) 0x80;
	private final static byte USB_DIR_OUT = (byte) 0x00;
	
	private final static short HIKEY_VENDORID = (short) 0x18D1;
	private final static short ACCESSORY_PRODUCTID = (short) 0x2D01;
	
	private final static int STANDARD_INTERFACE = 0;
	private final static int ADB_INTERFACE = 1;
	
	private static final long TIMEOUT = 5000;
	
	private static Context context;
	private static Device device;
	private static DeviceHandle handle;
	private static byte endpointIn;
	private static byte endpointOut;
	
	/*
	 * To start accessory mode, HiKey must have libusbk/libusb-win32 driver installed; winusb - error 3
	 */
	
	public static void main(String[] args) {
		try {
			init();
			DeviceDescriptor desc = new DeviceDescriptor();
			LibUsb.getDeviceDescriptor(device, desc);		
			if ( Short.compare(desc.idProduct(), ACCESSORY_PRODUCTID) == 0 && Short.compare(desc.idVendor(), HIKEY_VENDORID) == 0 ) {
				System.out.println("Hikey is already in accessory mode");
			}
			else {
				int result = setupAccessory("Google","Hikey960", "Description", "1.0", "http://www.mycompany.com", "SerialNumber");
				if ( result != LibUsb.SUCCESS ) {
					throw new LibUsbException("Unable to setup Accessory", result);
				}
				//wait for accessory mode
				try {
					Thread.sleep(TIMEOUT);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				init();
			}
			
			getEndPointAddress();
			LibUsb.releaseInterface(handle, 1);
			LibUsb.close(handle);
			LibUsb.exit(context);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void write(DeviceHandle handle, byte[] data) {
		ByteBuffer buffer = BufferUtils.allocateByteBuffer(data.length);
		buffer.rewind();
		buffer.put(data);
		IntBuffer transferred = BufferUtils.allocateIntBuffer();
		int result = LibUsb.bulkTransfer(handle, endpointOut, buffer, transferred, TIMEOUT);
		if ( result != LibUsb.SUCCESS ) {
			throw new LibUsbException("Bulk Transfer failure", result);
		}
		System.out.println(transferred.get() + " bytes sent to device");
	}
	
	public static ByteBuffer read(DeviceHandle handle, int size) {
		ByteBuffer buffer = BufferUtils.allocateByteBuffer(size).order(ByteOrder.LITTLE_ENDIAN);
		IntBuffer transferred = BufferUtils.allocateIntBuffer();
		int result = LibUsb.bulkTransfer(handle, endpointIn, buffer, transferred, TIMEOUT);
		if ( result  != LibUsb.SUCCESS ) {
			throw new LibUsbException("Unable to read data", result);
		}
		System.out.println(transferred.get() + " bytes read from device");
		return buffer;
	}
	
	private static void init() {
		context = new Context();
		int result = LibUsb.init(context);
		if ( result != LibUsb.SUCCESS ) {
			throw new LibUsbException("Unable to initialize libusb.", result);
		}
		
		device = findDevice(HIKEY_VENDORID);
		handle = new DeviceHandle();
		
		DeviceDescriptor desc = new DeviceDescriptor();
		LibUsb.getDeviceDescriptor(device, desc);		
		
		result = LibUsb.open(device, handle);
		if ( result < 0 ) {		
			throw new LibUsbException("Unable to open USB device", result);
		}
		System.out.println("USB successfully opened");
		
		
		int iface = Short.compare(ACCESSORY_PRODUCTID, desc.idProduct()) == 0 ? ADB_INTERFACE : STANDARD_INTERFACE;
		result = LibUsb.claimInterface(handle, iface);
		if ( result != LibUsb.SUCCESS ) {
			throw new LibUsbException("Unable to claim interface", result);
		}
		System.out.println("Interface successfully claimed");
	}
	
	private static Device findDevice(short vendorId) {
		DeviceList list = new DeviceList();
		int result = LibUsb.getDeviceList(context, list);
		if ( result < 0 ) {
			throw new LibUsbException("Unable to get device list", result);
		}
		try {
			for (Device device : list) {
				DeviceDescriptor descriptor = new DeviceDescriptor();
				result = LibUsb.getDeviceDescriptor(device, descriptor);
				if ( result != LibUsb.SUCCESS ) {
					throw new LibUsbException("Unable to read device descriptor", result);
				}
				if ( descriptor.idVendor() == vendorId ) {
					LibUsb.refDevice(device);
					return device;
				}
			}
		}
		finally {
			LibUsb.freeDeviceList(list, false);
			list = null;
		}
		return null;
	}
	
	public static void getEndPointAddress() {
		ConfigDescriptor configDescriptor = new ConfigDescriptor();
		LibUsb.getActiveConfigDescriptor(device, configDescriptor);
		EndpointDescriptor[] endPoints = (configDescriptor.iface()[0]).altsetting()[0].endpoint();
		for(EndpointDescriptor endPoint : endPoints) {
			if ( DescriptorUtils.getDirectionName(endPoint.bEndpointAddress()) == "IN" ) {
				endpointIn = (byte)(endPoint.bEndpointAddress() % 0xff);
			}
			if ( DescriptorUtils.getDirectionName(endPoint.bEndpointAddress()) == "OUT" ) {
				endpointOut = (byte)(endPoint.bEndpointAddress() % 0xff);
			}
		}
	}
	
	private static int setupAccessory(String vendor, String model, String description, String version, String uri, String serial) throws LibUsbException, InterruptedException {	
		int response = 0;
		//setup setup packet
		response = transferSetupPacket(LibUsb.cpuToLe16((short) 2), (byte) (USB_DIR_IN | LibUsb.REQUEST_TYPE_VENDOR), (byte) 51);
		System.out.println("Hikey supports accessory protocol version: " + response);
		//setup data packet
		response = transferAccessoryDataPacket(vendor, (short) 0);
		response = transferAccessoryDataPacket(model, (short) 1);
		response = transferAccessoryDataPacket(description, (short) 2);
		response = transferAccessoryDataPacket(version, (short) 3);
		response = transferAccessoryDataPacket(uri, (short) 4);
		response = transferAccessoryDataPacket(serial, (short) 5);
		//setup handshake packet
		response = transferSetupPacket((short) 0, (byte)(USB_DIR_OUT | LibUsb.REQUEST_TYPE_VENDOR), (byte) 53);
		System.out.println("Successfully put device in accessory mode");
		
		LibUsb.releaseInterface(handle, 0);
		LibUsb.close(handle);
		LibUsb.exit(context);
		
		handle = null;
		context = null;
		
		return response;
	}
	
	private static int transferAccessoryDataPacket(String param, short index) {
		int response;
		byte[] fill = null;
		try {
			fill = param.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		byte[] byteArray = new byte[256];
		for (int i = 0; i < byteArray.length; i++) {
			if ( i < fill.length ) {
				byteArray[i] = fill[i];
			}
			else {
				byteArray[i] = 0;
			}
		}
		byteArray[fill.length] = 0;
		ByteBuffer data = BufferUtils.allocateByteBuffer(byteArray.length);
		data.put(byteArray);
		
		final byte bRequest = (byte) 52;
		final short wValue = 0;
		final long timeout = 0;
		
		data.rewind();
		response = LibUsb.controlTransfer(handle, (byte) (USB_DIR_OUT | LibUsb.REQUEST_TYPE_VENDOR), bRequest, wValue, index, data, timeout);
		if ( response < 0 ) {
			throw new LibUsbException("Unable to control transfer.", response);
		}
		return response;
	}
	
	private static int transferSetupPacket(short bufferLength, byte requestType, byte request) throws LibUsbException {
		int response = 0;
		byte[] byteBuffer = new byte[bufferLength];
		ByteBuffer data = BufferUtils.allocateByteBuffer(bufferLength);
		data.put(byteBuffer);
		
		final short wValue = 0;
		final short wIndex = 0;
		final long timeout = 0;
		
		data.rewind();
		response = LibUsb.controlTransfer(handle, requestType, request, wValue, wIndex, data, timeout);
		if ( response < 0 ) {
			throw new LibUsbException("Unable to transfer setup packet.", response);
		}
		return response;
	}

}

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
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

	private static byte END_POINT_IN = (byte) 0x81;
	private static byte END_POINT_OUT = (byte) 0x01;
	
	private final static byte USB_DIR_IN = (byte) 0x80;
	private final static byte USB_DIR_OUT = (byte) 0x00;
	
	private final static short HIKEY_VENDORID = (short) 0x18D1;
	private final static short HIKEY_PRODUCTID = (short) 0x4EE7;
	
	private final static short ACC_PRODUCTID = (short) 0x2D01;
	
	private static Context context;
	private static Device device;
	private static DeviceHandle handle;
	private static boolean isShutdown;
	
	public static void main(String[] args) {
		isShutdown = false;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				isShutdown = true;
			}
		});
		try {
			init();
			int result = setupAccessory("Google","Hikey960", "Description", "1.0", "http://www.mycompany.com", "SerialNumber");
			if ( result != LibUsb.SUCCESS ) {
				throw new LibUsbException("Unable to setup Accessory", result);
			}
			device = findDevice(HIKEY_VENDORID);
			LibUsb.claimInterface(handle, 0);
			LibUsb.setConfiguration(handle, 1);
			try {
				Thread.sleep(20000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			getEndPointAddress(); //TODO Check if it works
			write(handle); //TODO Rewrite method
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			LibUsb.releaseInterface(handle, 0);
			LibUsb.resetDevice(handle);
			LibUsb.close(handle);
			LibUsb.exit(context);
		}
	}
	
	public static void write(DeviceHandle handle) {
		int data = 0;
		ByteBuffer buffer = BufferUtils.allocateByteBuffer(1);
		while (!isShutdown) {
			buffer.rewind();
			buffer.put((byte) data);
			IntBuffer transferred = BufferUtils.allocateIntBuffer();
			int result = LibUsb.bulkTransfer(handle, END_POINT_OUT, buffer, transferred, 0);
			if ( result != LibUsb.SUCCESS ) {
				throw new LibUsbException("Bulk Transfer failure", result);
			}
			System.out.println(transferred.get() + " bytes sent to device");
			if ( data < 255 ) {
				data = data + 1;
			}
			else {
				data = 0;
			}
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void init() {
		context = new Context();
		int result = LibUsb.init(context);
		if ( result != LibUsb.SUCCESS ) {
			throw new LibUsbException("Unable to initialize libusb.", result);
		}
		
		device = findDevice(HIKEY_VENDORID);
		DeviceDescriptor desc = new DeviceDescriptor();
		LibUsb.getDeviceDescriptor(device, desc);
		System.out.println(desc.dump());
		handle = new DeviceHandle();
		
		result = LibUsb.open(device, handle);
		if ( result < 0 ) {		
			throw new LibUsbException("Unable to open USB device", result);
		}
		System.out.println("Successfully opened");
		
		result = LibUsb.claimInterface(handle, 0);
		if ( result != LibUsb.SUCCESS ) {
			throw new LibUsbException("Unable to claim interface", result);
		}
		System.out.println("Successfully claimed");
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
		}
		
		return null;
	}
	
	public static void getEndPointAddress() {
		ConfigDescriptor configDescriptor = new ConfigDescriptor();
		LibUsb.getActiveConfigDescriptor(device, configDescriptor);
		EndpointDescriptor[] endPoints = (configDescriptor.iface()[0]).altsetting()[0].endpoint();
		for(EndpointDescriptor endPoint : endPoints) {
			if ( DescriptorUtils.getDirectionName(endPoint.bEndpointAddress()) == "IN" ) {
				END_POINT_IN = (byte)(endPoint.bEndpointAddress() % 0xff);
			}
			if ( DescriptorUtils.getDirectionName(endPoint.bEndpointAddress()) == "OUT" ) {
				END_POINT_OUT = (byte)(endPoint.bEndpointAddress() % 0xff);
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
		System.out.println("Successfully put device in accessory mode, bytes: " + response);
		
		LibUsb.releaseInterface(handle, 0);
		
		return response;
	}
	
	private static int transferAccessoryDataPacket(String param, short index) {
		int response;
		byte[] fill = null;
		try {
			fill = param.getBytes("UTF-8");
			System.out.println(new String(fill, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		byte[] byteArray = new byte[256];
		for (int i = 0; i < byteArray.length; i++) {
			if ( i < fill.length ) {
				byteArray[i] = fill[i];
				System.out.print(fill[i]);
			}
			else {
				byteArray[i] = 0;
				System.out.print('0');
			}
		}
		System.out.println();
		byteArray[fill.length] = 0;
		ByteBuffer data = BufferUtils.allocateByteBuffer(byteArray.length);
		data.put(byteArray);
		final byte bRequest = (byte) 52;
		final short wValue = 0;
		final long timeout = 0;
		data.rewind();
		response = LibUsb.controlTransfer(handle, (byte) (USB_DIR_OUT | LibUsb.REQUEST_TYPE_VENDOR), bRequest, wValue, index, data, timeout);
		if ( response < 0 ) {
			System.out.println(response);
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
		System.out.println(LibUsb.strError(response));
		if ( response < 0 ) {
			throw new LibUsbException("Unable to transfer setup packet.", response);
		}
		return response;
	}

}

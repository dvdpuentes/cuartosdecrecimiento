package com.example.arduinoconnector;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

public class UsbConnectionHandler {

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;

    public UsbConnectionHandler(UsbManager usbManager) {
        this.usbManager = usbManager;
    }

    public void setDevice(UsbDevice device) {
        this.device = device;
    }

    public void openConnection() {
        connection = usbManager.openDevice(device);
        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialPort != null) {
            if (serialPort.open()) {
                serialPort.setBaudRate(115200);
                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
            }
        }
    }

    public void closeConnection() {
        if (serialPort != null) {
            serialPort.close();
        }
    }

    public void writeData(byte[] data) {
        if (serialPort != null) {
            serialPort.write(data);
        }
    }

    public void setReadCallback(UsbSerialInterface.UsbReadCallback callback) {
        if (serialPort != null) {
            serialPort.read(callback);
        }
    }
}

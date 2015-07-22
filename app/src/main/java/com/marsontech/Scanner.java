package com.marsontech;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.marsontech.scsi.ScsiDeviceDriver;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by pinglamb on 7/20/15.
 */
public class Scanner implements View.OnKeyListener {
    private static final String TAG = Scanner.class.getSimpleName();

    private static final int INTERFACE_SUBCLASS_SCSI = 6;
    private static final int INTERFACE_PROTOCOL_BULK_TRANSFER = 80;

    private Context context;
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbInterface usbInterface;
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;
    private UsbDeviceConnection deviceConnection;

    private ScsiDeviceDriver driver;

    private String scanBuffer;
    private OnScanSuccessListener listener = null;
    private boolean scanning = false;

    public Scanner(Context context, UsbDevice device) {
        this.context = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.usbDevice = device;

        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface usbInterface = device.getInterface(i);
            Log.i(TAG, "USB interface detected: " + usbInterface);
            if (usbInterface.getInterfaceClass() != UsbConstants.USB_CLASS_MASS_STORAGE
                    || usbInterface.getInterfaceSubclass() != INTERFACE_SUBCLASS_SCSI
                    || usbInterface.getInterfaceProtocol() != INTERFACE_PROTOCOL_BULK_TRANSFER) {
                Log.i(TAG, "USB interface not suitable");
                continue;
            }

            if (usbInterface.getEndpointCount() == 2 && usbInterface.getEndpoint(0).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                this.usbInterface = usbInterface;
                if (usbInterface.getEndpoint(0).getDirection() == UsbConstants.USB_DIR_OUT) {
                    this.outEndpoint = usbInterface.getEndpoint(0);
                    this.inEndpoint = usbInterface.getEndpoint(1);
                } else {
                    this.outEndpoint = usbInterface.getEndpoint(1);
                    this.inEndpoint = usbInterface.getEndpoint(0);
                }
            } else {
                Log.i(TAG, "USB interface not of 2 endpoints of XFER BULK");
            }
        }
    }

    public boolean scan() {
        if (!scanning) {
            scanning = true;
            try {
                sendResetCommand();
                sendScanCommand();
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Failed to Scan", e);
                return false;
            }
        } else {
            return false;
        }
    }

    public void setOnScanSuccessListener(OnScanSuccessListener listener) {
        this.listener = listener;
    }

    public String getLastScanResult() {
        return scanBuffer.toString();
    }

    public boolean open() {
        deviceConnection = usbManager.openDevice(usbDevice);
        if (deviceConnection == null) {
            Log.i(TAG, "Cannot open device connection");
            return false;
        }

        if(!deviceConnection.claimInterface(usbInterface, true)) {
            Log.i(TAG, "Cannot claim USB interface");
            return false;
        }

        driver = new ScsiDeviceDriver(deviceConnection, inEndpoint, outEndpoint, 512);
        return true;
    }

    public boolean isOpen() {
        return deviceConnection != null;
    }

    public void close() {
        if (isOpen()) {
            deviceConnection.close();
        }
    }

    private void sendResetCommand() throws IOException {
        if (isOpen()) {
            byte[] data = new byte[512];
            data[0]  = (byte) 0xe5;
            data[1]  = (byte) 0x54;
            data[2]  = (byte) 0x41;
            data[3]  = (byte) 0x30;
            data[4]  = (byte) 0x32;
            data[5]  = (byte) 0x31;
            data[6]  = (byte) 0x30;
            data[7]  = (byte) 0x30;
            data[8]  = (byte) 0x30;
            data[9]  = (byte) 0x30;
            data[10] = (byte) 0x30;
            data[11] = (byte) 0x10;
            driver.write(1024, ByteBuffer.wrap(data));
        }
    }

    private void sendScanCommand() throws IOException {
        byte[] data = new byte[512];
        data[0]  = (byte) 0x48;
        data[1]  = (byte) 0x54;
        data[2]  = (byte) 0x41;
        data[3]  = (byte) 0x30;
        data[4]  = (byte) 0x32;
        data[5]  = (byte) 0x31;
        data[6]  = (byte) 0x30;
        data[7]  = (byte) 0x30;
        data[8]  = (byte) 0x30;
        data[9]  = (byte) 0x30;
        data[10] = (byte) 0x30;
        data[11] = (byte) 0x10;
        driver.write(1024, ByteBuffer.wrap(data));
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        Log.d(TAG, "KeyCode: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                if (scanning) {
                    scanBuffer = ((EditText)v).getText().toString();
                    if (this.listener != null) {
                        this.listener.onScanSuccess(scanBuffer);
                    }
                    ((EditText) v).setText(null);
                    scanning = false;
                    return true;
                }
        }
        return false;
    }

    public interface OnScanSuccessListener {
        public abstract void onScanSuccess(String result);
    }
}

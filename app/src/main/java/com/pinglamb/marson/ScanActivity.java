package com.pinglamb.marson;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.marsontech.Scanner;

import java.util.HashMap;
import java.util.Iterator;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ScanActivity extends AppCompatActivity {
    private static final String ACTION_USB_PERMISSION = "com.pinglamb.marson.USB_PERMISSION";

    @Bind(R.id.result)
    TextView resultView;
    @Bind(R.id.scan)
    Button scanButton;

    UsbManager usbManager;
    UsbDevice usbDevice = null;
    Scanner scanner = null;
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                deviceAttached();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                deviceDetached();
            } else if (ACTION_USB_PERMISSION.equals(action)) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (usbDevice != null) {
                        deviceApproved();
                    } else {
                        deviceDenied();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        ButterKnife.bind(this);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        deviceAttached();

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);
    }

    @OnClick(R.id.scan)
    public void scan(View view) {
        scanner.scan();
    }

    void deviceAttached() {
        scanButton.setEnabled(false);

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> iterator = deviceList.values().iterator();
        while (iterator.hasNext()) {
            UsbDevice device = iterator.next();
            if (device.getVendorId() == 273 && device.getProductId() == 273) {
                usbDevice = device;
                resultView.setText("Device Attached");
            }
        }

        if (usbDevice != null) {
            if (usbManager.hasPermission(usbDevice)) {
                deviceApproved();
            } else {
                PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(usbDevice, permissionIntent);
            }
        } else {
            resultView.setText("Unknown device");
        }
    }

    void deviceDetached() {
        resultView.setText("Device Detached");
        scanButton.setEnabled(false);
    }

    void deviceApproved() {
        resultView.setText("Device approved");

        scanner = new Scanner(this, usbDevice);
        if (scanner.open()) {
            resultView.setText("Ready to Scan");
            scanButton.setEnabled(true);
        } else {
            resultView.setText("Please replug the device");
        }
    }

    void deviceDenied() {
        resultView.setText("Please replug the device");
    }
}

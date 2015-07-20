package com.pinglamb.marson;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ScanActivity extends AppCompatActivity {
    @Bind(R.id.result)
    TextView resultView;
    @Bind(R.id.scan)
    Button scanButton;

    UsbManager mUsbManager;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                resultView.setText("Device Attached");
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                resultView.setText("Device Detached");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        ButterKnife.bind(this);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(mUsbReceiver);
    }

    @OnClick(R.id.scan)
    public void scan(View view) {
        resultView.setText("123123123");
    }
}

package com.hausa.transferbatterycapacity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private int intLevel;
    private int intScale;
    private String batteryCapacity;
    private String[] currentTime;
    private Button mButton;
    private TextView batteryTextView;
    private TextView timeTextView;

    private LinearLayout linearLayout;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    private static final String TAG = "MainActivity";
    private static final int MESSAGE_READ = 1;

    private Handler mHandler = new Handler();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        mButton = (Button) findViewById(R.id.getBleButton);

        // 检查手机是否支持蓝牙
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "本机不支持低功耗蓝牙", Toast.LENGTH_LONG).show();
            finish();
            return;
        } else {
            Toast.makeText(this, "正在获取电量和时间", Toast.LENGTH_SHORT).show();
        }

        // 获取电量和时间并实时更新UI数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    batteryCapacity = getBatteryCapacity();
                    currentTime = new SimpleDateFormat("hh:mm:ss").format(new Date(System.currentTimeMillis())).split(":");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            timeTextView.setText("当前时间：" + currentTime[0] + ":" + currentTime[1] + ":" + currentTime[2]);
                            batteryTextView.setText("当前电量：" + batteryCapacity + "%");
                        }
                    });
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "该设备缺少蓝牙适配器！", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "001");
        // 如果蓝牙未开启，请求用户开启蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "002");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            }
            Log.d(TAG, "004");
            startActivityForResult(enableBtIntent, 1);
        }


        // 获取已配对的设备
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBondedDevices();
            }
        });


    }

    private void getBondedDevices() {
        linearLayout.removeAllViews();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d(TAG, "Paired device found: " + device.getName() + " - " + device.getAddress());
                TextView newTextView = new TextView(this);
                newTextView.setText(device.getName() + "  -  " + device.getAddress());
                newTextView.setTextSize(18);
                newTextView.setPadding(10, 10, 10, 10);
                newTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        }
                        Toast.makeText(MainActivity.this, "正在向 " + device.getName() + " 发送信息", Toast.LENGTH_SHORT).show();
                        connectDevice(device);
                        sendText("battery:" + batteryCapacity + ";h:" + currentTime[0] + ";m:" + currentTime[1] + ";s:" + currentTime[2]);
                    }
                });
                linearLayout.addView(newTextView);
            }
        }
    }


    private void sendText(String text) {
        try {
            mOutputStream.write(text.getBytes());
            Toast.makeText(MainActivity.this, "发送成功", Toast.LENGTH_SHORT);
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "发送失败", Toast.LENGTH_SHORT);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothSocket != null) {
            try {
                mBluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 连接设备并打开输入输出流
    private void connectDevice(BluetoothDevice device) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {}
            Toast.makeText(MainActivity.this, "正在连接设备：" + device.getName(), Toast.LENGTH_SHORT);
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {}
            mBluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            mBluetoothSocket.connect();
            mInputStream = mBluetoothSocket.getInputStream();
            mOutputStream = mBluetoothSocket.getOutputStream();
            startReadThread();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "连接设备失败！", Toast.LENGTH_SHORT);
        }
    }



    private void startReadThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;
                while (true) {
                    try {
                        bytes = mInputStream.read(buffer);
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                        Toast.makeText(MainActivity.this, "收到消息：" + String.valueOf(bytes), Toast.LENGTH_SHORT);
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, "处理收到的消息时出现错误", Toast.LENGTH_SHORT);
                        break;
                    }
                }
            }
        }).start();
    }






    // 获取当前电量和时间并显示到屏幕上
    private String getBatteryCapacity(){
        batteryTextView = (TextView) findViewById(R.id.batteryTextView);
        timeTextView = (TextView) findViewById(R.id.timeTextView);
        if (Build.VERSION.SDK_INT >= 21) {
            BatteryManager bm = (BatteryManager) MainActivity.this.getSystemService(BATTERY_SERVICE);
            batteryCapacity = String.valueOf((int) bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
        } else {
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = MainActivity.this.registerReceiver(null, iFilter);
            int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
            double batteryPct = level / (double) scale;
            batteryCapacity = String.valueOf((int) (batteryPct * 100));
        }
        return batteryCapacity;
    }
}
package com.example.carremotecontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.nio.ByteBuffer;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // Local request code, equal to or greater than 0
    final int REQUEST_ENABLE_BT = 0;
    BLEManager bleManager;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button forwardBtn = (Button) findViewById(R.id.ForwardsBtn);
        Button backwardBtn = (Button) findViewById(R.id.BackwardsBtn);
        Button stopBtn = (Button) findViewById(R.id.StopBtn);
        Button leftBtn = (Button) findViewById(R.id.LeftBtn);
        Button rightBtn = (Button) findViewById(R.id.RightBtn);
        ConstraintLayout controlsLayout = (ConstraintLayout) findViewById(R.id.Controlls);
        Button connectBtn = (Button) findViewById(R.id.Connectbtn);
        TextView connectInformationTV = findViewById(R.id.ConnectInformationTV);

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
                BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

                if (bluetoothAdapter == null) {
                    connectInformationTV.setText("Device doesn't support Bluetooth");
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                        return;
                    }
                }

                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    onActivityResult(REQUEST_ENABLE_BT, 200, enableBtIntent);
                }

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    //return;
                }


                // Check if HC-05 is already paired with phone
                Set<BluetoothDevice> bluetoothDevices = bluetoothAdapter.getBondedDevices();
                BluetoothDevice carBluetoothModule = null;
                for (BluetoothDevice d: bluetoothDevices) {
                    if (d.getName().equals("HC-05")) {
                        carBluetoothModule = d;
                        break;
                    }
                }

                bleManager = new BLEManager(MainActivity.this);
                bleManager.setListener(new BLEManager.BLEListener() {
                    @Override
                    public void onConnecting() {
                        connectInformationTV.setText("Connecting...");
                    }

                    @Override
                    public void onConnected() {
                        connectInformationTV.setText("Device connected");
                        connectBtn.setVisibility(View.INVISIBLE);
                        controlsLayout.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onServicesDiscovered() {
                        connectInformationTV.setText("Found services");
                    }

                    @Override
                    public void onWritableCharacteristicFound(BluetoothGattCharacteristic characteristic) {
                        connectInformationTV.setText("Ready");
                    }

                    @Override
                    public void onDataWritten(byte[] data) {
                        connectInformationTV.setText("Sent: " + ByteBuffer.wrap(data).getInt());
                    }

                    @Override
                    public void onNotification(byte[] data) {
                        connectInformationTV.setText("Recieved: " + ByteBuffer.wrap(data).getInt());
                    }

                    @Override
                    public void onError(String reason) {
                        connectInformationTV.setText("Error: " + reason);
                    }

                    @Override
                    public void onDisconnected() {
                        connectInformationTV.setText("Device disconnected");
                        connectBtn.setVisibility(View.VISIBLE);
                        controlsLayout.setVisibility(View.INVISIBLE);
                    }
                });

                if (carBluetoothModule != null)
                    bleManager.connect(carBluetoothModule);
                else
                    connectInformationTV.setText("Couldn't find HC-05");
            }
        });

        forwardBtn.setOnTouchListener(new View.OnTouchListener() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    bleManager.sendInt(CarCommands.FORWARDS.getValue());
                }
                else
                {
                    stop(bleManager);
                }
                return true;
            }
        });

        backwardBtn.setOnTouchListener(new View.OnTouchListener() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    bleManager.sendInt(CarCommands.BACKWARDS.getValue());
                }
                else
                {
                    stop(bleManager);
                }
                return true;
            }
        });

        leftBtn.setOnTouchListener(new View.OnTouchListener() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    bleManager.sendInt(CarCommands.LEFT.getValue());
                }
                else
                {
                    stop(bleManager);
                }
                return true;
            }
        });

        rightBtn.setOnTouchListener(new View.OnTouchListener() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    bleManager.sendInt(CarCommands.RIGHT.getValue());
                }
                else
                {
                    stop(bleManager);
                }
                return true;
            }
        });

        stopBtn.setOnTouchListener(new View.OnTouchListener() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                stop(bleManager);
                return true;
            }
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleManager.disconnect();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private static void stop(BLEManager bleManager)
    {
        bleManager.sendInt(CarCommands.STOP.getValue());
    }
}
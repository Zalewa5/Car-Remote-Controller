package com.example.carremotecontroller;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Local request code, equal to or greater than 0
    final int REQUEST_ENABLE_BT = 0;

    UUID arduinoUUID = UUID.randomUUID();
    ConnectThread connectThread;
    Handler handler;
    ConnectedThread connectedThread;
    BLEManager bleManager;

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

        Button test = (Button) findViewById(R.id.button);
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
                    String test = d.getName();
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
                    }

                    @Override
                    public void onServicesDiscovered() {
                        //connectInformationTV.setText("");
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
                    }
                });

                if (carBluetoothModule != null)
                    bleManager.connect(carBluetoothModule);
                else
                    connectInformationTV.setText("Couldn't find HC-05");
            }
        });

        test.setOnClickListener(new View.OnClickListener() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void onClick(View v) {
                bleManager.sendInt(CarCommands.TEST.getValue());
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectThread.cancel();
    }
}
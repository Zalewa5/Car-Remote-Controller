package com.example.carremotecontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.nio.ByteBuffer;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // Local request code, equal to or greater than 0
    final int REQUEST_ENABLE_BT = 0;
    final int REQUEST_ENABLE_GPS = 1;
    BLEManager bleManager;
    BluetoothManager bluetoothManager;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup display
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find UI elements
        ImageButton forwardBtn = findViewById(R.id.ForwardsBtn);
        ImageButton backwardBtn = findViewById(R.id.BackwardsBtn);
        ImageButton stopBtn = findViewById(R.id.StopBtn);
        ImageButton leftBtn = findViewById(R.id.LeftBtn);
        ImageButton rightBtn = findViewById(R.id.RightBtn);
        ConstraintLayout controlsLayout = findViewById(R.id.Controlls);
        ImageButton connectBtn = findViewById(R.id.Connectbtn);
        TextView connectInformationTV = findViewById(R.id.ConnectInformationTV);

        connectBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    v.setAlpha(0.8F);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    v.setAlpha(1F);
                }
                return false;
            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothManager = getSystemService(BluetoothManager.class);
                BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

                if (bluetoothAdapter == null) {
                    connectInformationTV.setText("Device doesn't support Bluetooth");
                    return;
                }

                // Check bluetooth permission & request if missing
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
                        return;
                    }

                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                        return;
                    }

                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 3);
                        return;
                    }
                }

                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    onActivityResult(REQUEST_ENABLE_BT, 200, enableBtIntent);
                }

                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                if (!isGpsEnabled) {
                    startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_ENABLE_GPS);
                    connectInformationTV.setText("Enable location and connect again");
                    return;
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

                // What info is gonna show up on different BT actions
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
                    startDiscoveryAndPairHC05(bluetoothAdapter, connectInformationTV);
            }
        });

        // MOVEMENT BUTTONS ACTIONS

        forwardBtn.setOnTouchListener(new View.OnTouchListener() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    bleManager.sendInt(CarCommands.FORWARDS.getValue());
                    v.setAlpha(0.8F);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    stop(bleManager);
                    v.setAlpha(1F);
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
                    v.setAlpha(0.8F);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    stop(bleManager);
                    v.setAlpha(1F);
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
                    v.setAlpha(0.8F);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    stop(bleManager);
                    v.setAlpha(1F);
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
                    v.setAlpha(0.8F);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    stop(bleManager);
                    v.setAlpha(1F);
                }
                return true;
            }
        });

        stopBtn.setOnTouchListener(new View.OnTouchListener() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    stop(bleManager);
                    v.setAlpha(0.8F);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    stop(bleManager);
                    v.setAlpha(1F);
                }
                return true;
            }
        });
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleManager.disconnect();
    }

    // Checks if all of the permissions are granted
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 || requestCode == 2 || requestCode == 3) {
            boolean allGranted = true;
            if (grantResults == null || grantResults.length == 0) {
                allGranted = false;
            } else {
                for (int r : grantResults) {
                    if (r != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
            }

            TextView connectInformationTV = findViewById(R.id.ConnectInformationTV);
            if (allGranted) {
                if (connectInformationTV != null)
                {
                    connectInformationTV.setText("Permissions granted. Retrying...");
                }
                ImageButton connectBtn = findViewById(R.id.Connectbtn);
                if (connectBtn != null)
                {
                    connectBtn.performClick();
                }
            } else {
                if (connectInformationTV != null)
                {
                    connectInformationTV.setText("Bluetooth permission denied");
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private static void stop(BLEManager bleManager)
    {
        bleManager.sendInt(CarCommands.STOP.getValue());
    }

    // Looks for Bluetooth device called HC-05 & tries to pair with it (REQUIRES PRECISE LOCATION)
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION})
    private void startDiscoveryAndPairHC05(BluetoothAdapter bluetoothAdapter, TextView connectInformationTV) {
        connectInformationTV.setText("Scanning for HC-05...");

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        final boolean[] found = {false};

        BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
            @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null && device.getName() != null && device.getName().equals("HC-05")) {
                        found[0] = true;
                        connectInformationTV.setText("Found HC-05. Pairing...");
                        bluetoothAdapter.cancelDiscovery();

                        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                            device.createBond();
                        }

                        // Handles responses from BT pair popup
                        BroadcastReceiver bondReceiver = new BroadcastReceiver() {
                            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                            @Override
                            public void onReceive(Context context, Intent bondIntent) {
                                String bondAction = bondIntent.getAction();

                                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(bondAction)) {
                                    BluetoothDevice d = bondIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                                    if (d == null || d.getName() == null || !d.getName().equals("HC-05"))
                                    {
                                        return;
                                    }

                                    int state = bondIntent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                                    if (state == BluetoothDevice.BOND_BONDED) {
                                        connectInformationTV.setText("Paired. Connecting...");
                                        try
                                        {
                                            unregisterReceiver(this);
                                        } catch (Exception ignored) {}
                                        if (bleManager != null) {
                                            bleManager.connect(d);
                                        }
                                    } else if (state == BluetoothDevice.BOND_NONE) {
                                        connectInformationTV.setText("Pairing failed");
                                        try
                                        {
                                            unregisterReceiver(this);
                                        } catch (Exception ignored) {}
                                    }
                                }
                            }
                        };

                        IntentFilter bondFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                        registerReceiver(bondReceiver, bondFilter);

                        try
                        {
                            unregisterReceiver(this);
                        } catch (Exception ignored) {}
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    try
                    {
                        unregisterReceiver(this);
                    } catch (Exception ignored) {}
                    if (!found[0]) {
                        connectInformationTV.setText("Couldn't find HC-05");
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);

        boolean result = bluetoothAdapter.startDiscovery();
        Log.i("BT", "startDiscovery result = " + result);
    }


}
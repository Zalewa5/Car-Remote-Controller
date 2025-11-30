package com.example.carremotecontroller;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresPermission;

import java.nio.ByteBuffer;
import java.util.List;


public class BLEManager{
    public interface BLEListener {
        void onConnecting();
        void onConnected();
        void onServicesDiscovered();
        void onWritableCharacteristicFound(BluetoothGattCharacteristic characteristic);
        void onDataWritten(byte[] data);
        void onNotification(byte[] data);
        void onError(String reason);
        void onDisconnected();
    }
    private Context context;
    private Handler handler = new Handler(Looper.getMainLooper());
    private BLEListener listener;
    private BluetoothGattCharacteristic writableCharacteristic;
    private BluetoothGatt gatt;

    public BLEManager(Context context)
    {
        this.context = context;
    }

    public void setListener(BLEListener listener) {
        this.listener = listener;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void connect(BluetoothDevice device){
        if (listener == null){
            return;
        }
        listener.onConnecting();
        gatt = device.connectGatt(context, false, new BluetoothGattCallback() {

            // (Not implemented) Read data when it was changed on the device (missing observing correct characteristic)
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                byte[] data = characteristic.getValue();
                handler.post(() -> listener.onNotification(data));
            }

            // When data was sent through BLE, gives back to MainActivity what was sent through listener
            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status != BluetoothGatt.GATT_SUCCESS){
                    handler.post(() -> listener.onError("There is an issue with sending data. Status: " + status));
                    return;
                }

                byte[] value = characteristic.getValue();
                handler.post(() -> listener.onDataWritten(value));
            }

            // When connected looks for services offered by connected device, when disconnected closes the GATT client. Gives back respective info to MainActivity through listener.
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (status != BluetoothGatt.GATT_SUCCESS){
                    handler.post(() -> listener.onError("There is an issue with connection."));
                    return;
                }

                if (newState == BluetoothProfile.STATE_CONNECTED){
                    handler.post(() -> listener.onConnected());
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    handler.post(() -> listener.onDisconnected());
                    disconnect();
                }
            }

            // After services are found sends info to MainActivity through the listener & calls function to find writable data stream.
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status != BluetoothGatt.GATT_SUCCESS){
                    handler.post(() -> listener.onError("Couldn't find any services."));
                    return;
                }

                handler.post(() -> listener.onServicesDiscovered());
                findWritableCharacteristic(gatt.getServices());
            }
        });
    }

    // Looks through all of the services for characteristic that has a property that allows to send (write) data through BLE.
    private void findWritableCharacteristic(List<BluetoothGattService> services) {
        for (BluetoothGattService service : services){
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()){
                if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 || (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0){
                    writableCharacteristic = characteristic;
                    handler.post(() -> listener.onWritableCharacteristicFound(writableCharacteristic));
                    break;
                }
            }
            if (writableCharacteristic != null){
                break;
            }
        }

        if (writableCharacteristic == null){
            handler.post(() -> listener.onError("Couldn't find writable characteristic."));
        }
    }

    // Sends provided int to device connected through BLE
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void sendInt(int value) {
        if (gatt == null || writableCharacteristic == null){
            handler.post(() -> listener.onError("Connection wasn't established or couldn't find writable characteristic"));
        }
        byte[] data = ByteBuffer.allocate(4).putInt(value).array();
        writableCharacteristic.setValue(data);
        gatt.writeCharacteristic(writableCharacteristic);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void disconnect() {
        gatt.disconnect();
        gatt.close();
        gatt = null;
    }
}

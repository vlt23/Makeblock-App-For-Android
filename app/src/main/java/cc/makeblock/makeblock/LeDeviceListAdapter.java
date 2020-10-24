package cc.makeblock.makeblock;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;

public class LeDeviceListAdapter {

    // Adapter for holding devices found through scanning.
    private final ArrayList<BluetoothDevice> mLeDevices;

    public LeDeviceListAdapter() {
        super();
        mLeDevices = new ArrayList<>();
    }

    public void addDevice(BluetoothDevice device) {
        if (!mLeDevices.contains(device)) {
            mLeDevices.add(device);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
    }

    public int getCount() {
        return mLeDevices.size();
    }

}

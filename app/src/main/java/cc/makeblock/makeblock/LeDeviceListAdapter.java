package cc.makeblock.makeblock;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;

public class LeDeviceListAdapter {

    // Adapter for holding devices found through scanning.

    private ArrayList<BluetoothDevice> mLeDevices;
//	private LayoutInflater mInflator;
//	private Activity mContext;

    public LeDeviceListAdapter() {
        super();
//		mContext = c;
        mLeDevices = new ArrayList<>();
//		mInflator = mContext.getLayoutInflater();
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

    public long getItemId(int i) {
        return i;
    }

//	public View getView(int i, View view, ViewGroup viewGroup) {
//		ViewHolder viewHolder;
//		// General ListView optimization code.
//		if (view == null) {
//			view = mInflator.inflate(R.layout.listitem_bledevice, null);
//			viewHolder = new ViewHolder();
//			viewHolder.deviceAddress = (TextView) view
//					.findViewById(R.id.device_address);
//			viewHolder.deviceName = (TextView) view
//					.findViewById(R.id.device_name);
//			view.setTag(viewHolder);
//		} else {
//			viewHolder = (ViewHolder) view.getTag();
//		}
//
//		BluetoothDevice device = mLeDevices.get(i);
//		final String deviceName = device.getName();
//		if (deviceName != null && deviceName.length() > 0)
//			viewHolder.deviceName.setText(deviceName);
//		else
//			viewHolder.deviceName.setText(R.string.unknown_device);
//		viewHolder.deviceAddress.setText(device.getAddress());
//
//		return view;
//	}
//
//	class ViewHolder {
//		TextView deviceName;
//		TextView deviceAddress;
//	}
}

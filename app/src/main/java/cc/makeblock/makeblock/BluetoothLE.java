package cc.makeblock.makeblock;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import cc.makeblock.makeblock.BluetoothLeClass.OnDataAvailableListener;
import cc.makeblock.makeblock.BluetoothLeClass.OnDisconnectListener;
import cc.makeblock.makeblock.BluetoothLeClass.OnServiceDiscoverListener;

import java.util.ArrayList;
import java.util.List;

public class BluetoothLE extends Service {
    private final static String TAG = BluetoothLE.class.getSimpleName();
    private final static String UUID_KEY_DATA = "0000ffe1-0000-1000-8000-00805f9b34fb";

    private LeDeviceListAdapter mDevices;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeClass mBLE;
    private boolean mScanning;
    private Handler mHandler;
    private Context mContext;
    public Handler leHandler;
    private BluetoothDevice mCurrentDevice;
    private boolean mIsConnected = false;
    static final int MSG_CONNECTED = 1;
    static final int MSG_DISCONNECTED = 2;
    static final int MSG_RX = 3;
    static final int MSG_FOUND_DEVICE = 4;
    static final int MSG_CONNECT_FAIL = 5;
    static final int MSG_DISCOVERY_FINISHED = 6;
    static final int MSG_SCAN_START = 8;
    static final int MSG_SCAN_END = 9;
    static final int MSG_CONNECTING = 10;
    // Stops scanning after 10 seconds.  
    private static final long SCAN_PERIOD = 10000;

    static final int MODE_LINE = 0;
    static final int MODE_FORWARD = 1;
    public int commMode = MODE_LINE;

    private static BluetoothLE _instance;

    public static BluetoothLE sharedManager() {
        if (_instance == null) {
            _instance = new BluetoothLE();
        }
        return _instance;
    }

    public BluetoothLE() {
        // bluetoothLE classic
    }

    public void setup(Context context) {
        mDevices = new LeDeviceListAdapter();
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(
                Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mBluetoothAdapter.enable();

        mBLE = new BluetoothLeClass(mContext);
        if (!mBLE.initialize()) {
            Log.e(TAG, "Unable to initialize Bluetooth");
        }
        mBLE.setOnServiceDiscoverListener(mOnServiceDiscover);
        mBLE.setOnDataAvailableListener(mOnDataAvailable);
    }

    public void start() {
        scanLeDevice(true);
    }

    public void stop() {
        scanLeDevice(false);
        mBLE.disconnect();
        mIsConnected = false;
    }

    public void close() {
        mBLE.close();
        mIsConnected = false;
        mCurrentDevice = null;
    }

    public void clear() {
        mDevices.clear();
    }

    public List<String> getDeviceList() {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < mDevices.getCount(); i++) {
            BluetoothDevice dev = mDevices.getDevice(i);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            String s = dev.getName();
            if (s != null) {
                if (s.contains("null")) {
                    s = "Bluetooth";
                }
            } else {
                s = "Bluetooth";
            }
            if (mCurrentDevice != null) {
                s = s + " " + dev.getAddress() + " " + (dev.getBondState() == BluetoothDevice.BOND_NONE
                        ? (mCurrentDevice.equals(dev) ? mContext.getString(R.string.connected) :
                        mContext.getString(R.string.unbond))
                        : mContext.getString(R.string.bonded));
            } else {
                s = s + " " + dev.getAddress() + " " + (dev.getBondState() == BluetoothDevice.BOND_BONDED
                        ? mContext.getString(R.string.bonded) : mContext.getString(R.string.unbond));
            }
            data.add(s);
        }
        return data;
    }

    public boolean selectDevice(int position) {
        final BluetoothDevice device = mDevices.getDevice(position);
        if (device == null) {
            return false;
        }
        if (mScanning) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        if (leHandler != null) {
            Message msg = leHandler.obtainMessage(MSG_CONNECTING);
            leHandler.sendMessage(msg);
        }
        boolean conn = mBLE.connect(device.getAddress());
        if (conn) {
            mCurrentDevice = device;
        }
        return conn;
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    private final byte[] mBuffers = new byte[1024];
    private int mBuffersIndex = 0;

    public void writeBuffer(byte[] buf) {
        for (byte b : buf) {
            if (mBuffersIndex == 1024) {
                mBuffersIndex = 0;
            }
            mBuffers[mBuffersIndex] = b;
            mBuffersIndex++;
        }
        MeTimer.startWrite();
    }

    public boolean writeSingleBuffer() {
        BluetoothGattCharacteristic ch = characteristicForProperty(
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE);
        if (ch != null) {
            if (mBuffersIndex > 0) {
                int len = Math.min(mBuffersIndex, 20);
                byte[] buf = new byte[len];
                System.arraycopy(mBuffers, 0, buf, 0, len);
                mBuffersIndex -= len;
                byte[] _clone = mBuffers.clone();
                if (mBuffersIndex >= 0)
                    System.arraycopy(_clone, len, mBuffers, 0, mBuffersIndex);
                ch.setValue(buf);
//				Log.d("mb", "le tx:"+buf.length);
                mBLE.writeCharacteristic(ch);
                return true;
            }
        }
        return false;
    }

    private byte[] mProbeBytes;

    public void resetIO(byte[] probeBytes) {
        mProbeBytes = probeBytes;
        if (resetIndex == 0) {
            resetIndex++;
            resetHandler.postDelayed(resetRunnable, 30);
        }
    }

    private void resetLow() {
        BluetoothGattCharacteristic ch = characteristicForProperty(BluetoothGattCharacteristic.PROPERTY_WRITE
                | BluetoothGattCharacteristic.PROPERTY_READ);
        if (ch != null) {
            byte[] buf = {0};
            ch.setValue(buf);
            mBLE.writeCharacteristic(ch);
            Log.d("mb", "reset low");
        }
    }

    private void resetHigh() {
        BluetoothGattCharacteristic ch = characteristicForProperty(BluetoothGattCharacteristic.PROPERTY_WRITE
                | BluetoothGattCharacteristic.PROPERTY_READ);
        if (ch != null) {
            byte[] buf = {1};
            ch.setValue(buf);
            mBLE.writeCharacteristic(ch);
            Log.d("mb", "reset high");
        }
    }

    private int resetIndex = 0;
    Handler resetHandler = new Handler(Looper.getMainLooper());
    Runnable resetRunnable = new Runnable() {
        @Override
        public void run() {
            if (resetIndex == 4) {
                resetHigh();
            } else if (resetIndex < 4) {
                if (resetIndex % 2 == 1) {
                    resetHigh();
                } else {
                    resetLow();
                }
            } else if (resetIndex == 5) {
                writeBuffer(mProbeBytes);
            }
            resetIndex++;

            if (resetIndex < 7) {
                resetHandler.postDelayed(this, resetIndex == 5 ? 500 : (resetIndex == 6 ? 2000 : 100));
            } else {
                resetIndex = 0;
                Log.d("mb", "reset end");
            }
        }
    };

    private BluetoothGattCharacteristic characteristicForProperty(int property) {
        List<BluetoothGattService> list = mBLE.getSupportedGattServices();
        if (list == null) {
            return null;
        }
        for (BluetoothGattService gattService : list) {
            //-----Service-----//
            String uuid = gattService.getUuid().toString();
            //-----Characteristics-----//
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                int properties = gattCharacteristic.getProperties();
                int p = (property & properties);
                if (property == p) {
                    if (property == (BluetoothGattCharacteristic.PROPERTY_WRITE
                            | BluetoothGattCharacteristic.PROPERTY_READ)) {
                        if (uuid.indexOf("ffe4") > 0) {
                            return gattCharacteristic;
                        }
                    } else {
                        return gattCharacteristic;
                    }
                }
            }
        }
        return null;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.  
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    try {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        if (leHandler != null) {
                            Message msg = leHandler.obtainMessage(MSG_SCAN_END);
                            leHandler.sendMessage(msg);
                        }
                    } catch (SecurityException ignored) {}
                }
            }, SCAN_PERIOD);

            mScanning = true;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            if (leHandler != null) {
                Message msg = leHandler.obtainMessage(MSG_SCAN_START);
                leHandler.sendMessage(msg);
            }
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private final OnDisconnectListener mOnDisconnectListener = new OnDisconnectListener() {
        @Override
        public void onDisconnect(BluetoothGatt gatt) {
            stop();
            Log.d("mb", "ble disconnected");
            if (leHandler != null) {
                Message msg = leHandler.obtainMessage(MSG_DISCONNECTED);
                leHandler.sendMessage(msg);
            }
        }
    };

    private final OnServiceDiscoverListener mOnServiceDiscover = new OnServiceDiscoverListener() {
        @Override
        public void onServiceDiscover(BluetoothGatt gatt) {
            displayGattServices(mBLE.getSupportedGattServices());
        }
    };

    private final OnDataAvailableListener mOnDataAvailable = new OnDataAvailableListener() {
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                Log.d("mb", "onCharRead "
                        + " read "
                        + characteristic.getUuid().toString());
        }

        byte[] buffer = new byte[1024];
        int bytesLen;
        private final List<Integer> mRx = new ArrayList<>();

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            buffer = characteristic.getValue();

            bytesLen = buffer.length;
            if (bytesLen > 0) {
                for (int i = 0; i < bytesLen; i++) {
                    int c = buffer[i] & 0xff;
                    mRx.add(c);
                    // line end or bootloader end
                    if ((c == 0x0a && commMode == MODE_LINE) || (c == 0x10 && commMode == MODE_FORWARD)) {
                        // TODO: post msg to UI
                        //write(mReceiveString.getBytes());
                        int[] rxBytes = new int[mRx.size()]; // = mRx.toArray(new Byte[mRx.size()]);

                        StringBuilder hexStr = new StringBuilder();
                        for (int i1 = 0; i1 < rxBytes.length; i1++) {
                            rxBytes[i1] = mRx.get(i1);
                            hexStr.append(String.format("%02X ", rxBytes[i1]));
                        }
                        Log.d("mb", "le rx:" + hexStr);

                        leHandler.obtainMessage(MSG_RX, rxBytes).sendToTarget();
                        mRx.clear();
                    }
                }
            }
        }
    };

    // Device scan callback.  
    private final LeScanCallback mLeScanCallback = new LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDevices.addDevice(device);
                    // push notify
                    if (leHandler != null) {
                        Message msg = leHandler.obtainMessage(MSG_FOUND_DEVICE);
                        leHandler.sendMessage(msg);
                    }
                }
            });
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            return;
        }
        mIsConnected = true;
        for (BluetoothGattService gattService : gattServices) {
            //-----Characteristics-----//
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                if ((gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY)
                        == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                    mBLE.setCharacteristicNotification(gattCharacteristic, true);
                }
            }
        }

        if (leHandler != null) {
            Message msg = leHandler.obtainMessage(MSG_CONNECTED);
            leHandler.sendMessage(msg);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}  

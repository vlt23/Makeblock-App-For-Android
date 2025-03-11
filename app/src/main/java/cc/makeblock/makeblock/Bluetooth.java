package cc.makeblock.makeblock;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Bluetooth extends Service {
    static final String dbg = "bluetooth";
    static final int MSG_CONNECTED = 1;
    static final int MSG_DISCONNECTED = 2;
    static final int MSG_RX = 3;
    static final int MSG_FOUND_DEVICE = 4;
    static final int MSG_CONNECT_FAIL = 5;
    static final int MSG_DISCOVERY_FINISHED = 6;

    BluetoothAdapter mBTAdapter;
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    BluetoothDevice connDev;
    ConnectThread mConnectThread;
    ConnectedThread mConnectedThread;
    List<BluetoothDevice> btDevices;
    List<BluetoothDevice> prDevices; // paired bt devices

    Handler mHandler;
    static final int MODE_LINE = 0;
    static final int MODE_FORWARD = 1;
    public int commMode = MODE_LINE;

    private static Bluetooth _instance;

    public static Bluetooth sharedManager() {
        if (_instance == null) {
            _instance = new Bluetooth();
        }
        return _instance;
    }

    public Bluetooth() {
        // bluetooth classic
        btDevices = new ArrayList<>();
        prDevices = new ArrayList<>();
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBTAdapter == null) {
            Log.i(dbg, "Bluetooth not support");
        }
    }

    @Override
    public void onCreate() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBTDevDiscover, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBTDevDiscover, filter);
        _instance = this;
        if (!mBTAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mBTAdapter.enable();
        } else {
            startDiscovery();
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBTDevDiscover);
    }

    public void startDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mBTAdapter.startDiscovery();
    }

    public boolean isDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return mBTAdapter.isDiscovering();
    }

    public boolean isEnabled() {
        return mBTAdapter.isEnabled();
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket = null;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
        }

        @Override
        public void run() {
            try {
                mBTAdapter.cancelDiscovery();
                try {
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                    mmSocket.connect();
                } catch (IOException e) {
                    try {  // Fallback
                        Method m = mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                        Object[] params = new Object[]{1};
                        mmSocket = (BluetoothSocket) m.invoke(mmDevice, params);
                        mmSocket.connect();
                    } catch (IOException err) {
                        Log.d("mb", "connect:" + err.getMessage());
                        e.printStackTrace();
                        try {
                            mmSocket.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        if (mHandler != null) {
                            Message msg = mHandler.obtainMessage(MSG_CONNECT_FAIL);
                            mHandler.sendMessage(msg);
                        }
                    } catch (IllegalAccessException | IllegalArgumentException |
                             InvocationTargetException | NoSuchMethodException e1) {
                        e1.printStackTrace();
                    }
                    return;
                }
                // start connection manager in another thread
                bluetoothConnected(mmDevice, mmSocket);
            } catch (SecurityException ignored) {
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final List<Byte> mRx;
        public boolean txBusy;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            mRx = new ArrayList<>();
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    if (bytes > 0) {
                        for (int i = 0; i < bytes; i++) {
                            byte c = buffer[i];
                            mRx.add(c);
                            // line end or bootloader end
                            if ((c == 0x0a && commMode == MODE_LINE) || (c == 0x10 && commMode == MODE_FORWARD)) {
                                // TODO: post msg to UI
                                //write(mReceiveString.getBytes());
                                Byte[] rxBytes = mRx.toArray(new Byte[0]);
                                StringBuilder hexStr = new StringBuilder();
                                int[] buf = new int[mRx.size()];
                                for (int i1 = 0; i1 < rxBytes.length; i1++) {
                                    hexStr.append(String.format("%02X ", rxBytes[i1]));
                                    buf[i1] = rxBytes[i1];
                                }
                                Log.i("mb", "rx:" + hexStr);
                                mHandler.obtainMessage(MSG_RX, buf).sendToTarget();
                                mRx.clear();
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.i(dbg, "disconnected");
                    connDev = null;
                    if (mHandler != null) {
                        Message msg = mHandler.obtainMessage(MSG_DISCONNECTED);
                        mHandler.sendMessage(msg);
                    }
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                txBusy = true;
                mmOutStream.write(bytes);
                mmOutStream.flush();
                txBusy = false;
            } catch (IOException e) {
                Log.e(dbg, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(dbg, "Exception during cancel", e);
            }
        }
    }

    public void devListClear() {
        btDevices.clear();
        // don't forget the connecting device
        if (connDev != null) {
            btDevices.add(connDev);
        }
    }

    final BroadcastReceiver mBTDevDiscover = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("mb", "broadcast:" + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Log.d("mb", "bluetooth found:"+device.getName()+" "+device.getAddress()+" "+device.getBondState()+" "+BluetoothDevice.BOND_NONE+" "+BluetoothDevice.BOND_BONDED);
                if (!btDevices.contains(device)) {
                    btDevices.add(device);
                    if (mHandler != null) {
                        Message msg = mHandler.obtainMessage(MSG_FOUND_DEVICE);
                        mHandler.sendMessage(msg);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mHandler != null) {
                    Message msg = mHandler.obtainMessage(MSG_DISCOVERY_FINISHED);
                    mHandler.sendMessage(msg);
                }
                Log.i(dbg, "bluetooth discover finished");
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                Log.i(dbg, "bluetooth ACTION_STATE_CHANGED:" + mBTAdapter.isEnabled());
            }
        }
    };

    public List<String> getBtDevList() {
        List<String> data = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();
//      prDevices.clear();
        if (!pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                if (!btDevices.contains(device))
                    btDevices.add(device);
            }
        }
        for (BluetoothDevice dev : btDevices) {
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
            //String[] a = dev.getAddress().split(":");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            s = s + " " + dev.getAddress() + " " + (dev.getBondState() == BluetoothDevice.BOND_BONDED
                    ? (connDev.equals(dev) ? getString(R.string.connected) : getString(R.string.bonded))
                    : getString(R.string.unbond));
            data.add(s);
        }
        return data;
    }

    public void bluetoothWrite(String str) {
        if (mConnectedThread == null) return;
        //Log.i(dbg, "tx:"+str);
        mConnectedThread.write(str.getBytes());
    }

    public void bluetoothWrite(byte[] data) {
        if (mConnectedThread == null) {
            return;
        }
        StringBuilder hexStr = new StringBuilder();
        for (byte datum : data) {
            hexStr.append(String.format("%02X ", datum));
        }
        Log.d("mb", "tx:" + hexStr);
        if (!mConnectedThread.txBusy) {
            mConnectedThread.write(data);
        } else {
            Log.d("mb", "tx busy");
        }
    }

    public void bluetoothDisconnect(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.i(dbg, "disconnect to " + device.getName());
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    public void bluetoothConnect(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.i(dbg, "try connect to " + device.getName());
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

        Intent intent = new Intent(this, DialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("msg", getString(R.string.connecting));
        startActivity(intent);
    }

    public void bluetoothConnected(BluetoothDevice device, BluetoothSocket socket) {
        Log.i(dbg, "bluetooth connected:" + device.getAddress());
        connDev = device;
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(MSG_CONNECTED);
            mHandler.sendMessage(msg);
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        Intent intent = new Intent(this, DialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("msg", "connected");
        startActivity(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

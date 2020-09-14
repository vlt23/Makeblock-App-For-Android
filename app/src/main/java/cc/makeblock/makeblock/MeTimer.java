package cc.makeblock.makeblock;

import android.os.Handler;

public class MeTimer {
    private static byte[] mToSend;
    private static Handler mHandler = new Handler();
    private static Handler mLoopHandler = new Handler();
    private static Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            if (mToSend != null) {
//				Log.d("mb", "delay writting");
                BluetoothLE.sharedManager().writeBuffer(mToSend);
            }
        }
    };
    private static boolean isLoop = false;
    private static Runnable mLoopRunnable = new Runnable() {

        @Override
        public void run() {
            if (BluetoothLE.sharedManager().writeSingleBuffer()) {
                mLoopHandler.postDelayed(mLoopRunnable, 10);
                isLoop = true;
            } else {
                isLoop = false;
            }
        }
    };

    public static void delayWrite(byte[] toSend, int delay) {
        mToSend = toSend;
        if (mHandler == null) {
            mHandler = new Handler();
        }
//		Log.d("mb", "start delay");
        mHandler.postDelayed(mRunnable, delay);
    }

    public static void startWrite() {
        if (!isLoop) {
            mLoopHandler.postDelayed(mLoopRunnable, 60);
        }
    }
}

package cc.makeblock.modules;

import android.os.Handler;

public class MePhoto extends MeModule {

    static String devName = "ultrasonic";
    boolean isOn = false;

    public MePhoto() {
        super(devName, MeModule.DEV_PHOTO, 0, 0);
    }

    @Override
    public byte[] getQuery(int index) {
        return buildQuery(MeModule.DEV_PHOTO, 0, 0, index);
    }

    @Override
    public void setEnable(Handler handler) {
        mHandler = handler;
    }

    public void takePhoto() {
        byte[] wr = getQuery(0);
        //byte[] wr = buildWrite(MeModule.DEV_PHOTO, 0, 0, 0);
        mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
    }

}

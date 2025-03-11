package cc.makeblock.modules;

import android.os.Handler;

public class MeAIDetection extends MeModule {

    static String devName = "MeAIDetection";

    public MeAIDetection() {
        super(devName, MeModule.DEV_AI_DETECTION, 0, 0);
    }

    @Override
    public byte[] getQuery(int index) {
        return buildQuery(MeModule.DEV_AI_DETECTION, 0, 0, index);
    }

    @Override
    public void setEnable(Handler handler) {
        mHandler = handler;
    }

    public void setAIDetectionMode() {
        byte[] wr = getQuery(1);
        //byte[] wr = buildWrite(MeModule.DEV_PHOTO, 0, 0, 0);
        mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
    }

}

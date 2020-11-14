package cc.makeblock.modules;

import android.os.Handler;
import android.os.Looper;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;
import cc.makeblock.makeblock.MeDevice;
import cc.makeblock.makeblock.R;
import org.json.JSONObject;

public class MeUltrasonic extends MeModule {
    static String devName = "ultrasonic";
    private ToggleButton toggleBt;
    private final Handler mLoopHandler = new Handler(Looper.getMainLooper());

    private boolean isAuto = false;
    private float mCurrentValue = 0.0f;

    public MeUltrasonic(int port, int slot) {
        super(devName, MeModule.DEV_ULTRASONIC, port, slot);
        viewLayout = R.layout.dev_auto_driver;
        imageId = R.drawable.ultrasonic;
    }

    public MeUltrasonic(JSONObject jObj) {
        super(jObj);
        viewLayout = R.layout.dev_auto_driver;
        imageId = R.drawable.ultrasonic;
    }

    @Override
    public String getScriptRun(String var) {
        varReg = var;
        return var + " = distance(" + getPortString(port) + ")\n";
    }

    @Override
    public byte[] getQuery(int index) {
        return buildQuery(type, port, slot, index);
    }

    private int mBackTime = 0;
    private int mFrontTime = 0;
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAuto) {
                mLoopHandler.postDelayed(this, 100);
                if (view != null) {
                    int motorSpeed = MeDevice.sharedManager().motorSpeed;
                    if (!MeDevice.sharedManager().manualMode) {
                        if ((mCurrentValue > 0.0 && mCurrentValue < 40) || mBackTime > 0) {
                            if (mBackTime < 5) {
                                mBackTime++;
                                byte[] wr = buildWrite(DEV_DCMOTOR, PORT_M1, slot, -motorSpeed);
                                mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                                byte[] wr2 = buildWrite(DEV_DCMOTOR, PORT_M2, slot, -motorSpeed);
                                mHandler.obtainMessage(MSG_VALUE_CHANGED, wr2).sendToTarget();
                            } else if (mBackTime < 10) {
                                mBackTime++;
                                byte[] wr = buildWrite(DEV_DCMOTOR, PORT_M1, slot, motorSpeed);
                                mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                                byte[] wr2 = buildWrite(DEV_DCMOTOR, PORT_M2, slot, -motorSpeed);
                                mHandler.obtainMessage(MSG_VALUE_CHANGED, wr2).sendToTarget();
                            } else {
                                mBackTime = 0;
                            }
                        } else {
                            if (mFrontTime < 10) {
                                byte[] wr = buildWrite(DEV_DCMOTOR, PORT_M1, slot, motorSpeed);
                                mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                                byte[] wr2 = buildWrite(DEV_DCMOTOR, PORT_M2, slot, motorSpeed);
                                mHandler.obtainMessage(MSG_VALUE_CHANGED, wr2).sendToTarget();
                            }
                            if (mCurrentValue == 0) {
                                mFrontTime++;
                            } else {
                                mFrontTime = 0;
                            }
                        }
                    }
                }
            }
        }
    };

    @Override
    public void setEnable(Handler handler) {
        mHandler = handler;
        toggleBt = view.findViewById(R.id.autoSwitch);
        OnCheckedChangeListener listener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isAuto = isChecked;
                if (isAuto) {
                    mLoopHandler.postDelayed(mRunnable, 100);
                } else {
                    byte[] wr = buildWrite(DEV_DCMOTOR, PORT_M1, slot, 0);
                    mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                    byte[] wr2 = buildWrite(DEV_DCMOTOR, PORT_M2, slot, 0);
                    mHandler.obtainMessage(MSG_VALUE_CHANGED, wr2).sendToTarget();
                }
            }
        };
        toggleBt.setOnCheckedChangeListener(listener);
    }

    @Override
    public void setDisable() {
        toggleBt = view.findViewById(R.id.autoSwitch);
        toggleBt.setOnCheckedChangeListener(null);
    }

    @Override
    public void setEchoValue(String value) {
        TextView txt = view.findViewById(R.id.textValue);
        mCurrentValue = Float.parseFloat(value);
        txt.setText(Math.floor(mCurrentValue * 10.0) / 10.0 + " cm");
    }

}

package cc.makeblock.modules;

import android.os.Handler;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;
import cc.makeblock.makeblock.R;
import org.json.JSONObject;

public class MeUltrasonic extends MeModule {
    static String devName = "ultrasonic";
    private ToggleButton toggleBt;

    private boolean isAuto = false;

    private final int megaPiMode = 0x12;  // #define SET_MEGAPI_MODE in the firmware

    public MeUltrasonic(int port, int slot) {
        super(devName, MeModule.DEV_ULTRASONIC, port, slot);
        initRestComponents();
    }

    public MeUltrasonic(JSONObject jObj) {
        super(jObj);
        initRestComponents();
    }

    private void initRestComponents() {
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

    @Override
    public void setEnable(Handler handler) {
        mHandler = handler;
        toggleBt = view.findViewById(R.id.autoSwitch);
        toggleBt.setEnabled(true);
        OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            isAuto = isChecked;
            // type = 60, port = 0x12, slot = whatever, value = 1 or 40
            if (isAuto) {
                ultrasonicOn();
            } else {
                ultrasonicOff();
            }
        };
        toggleBt.setOnCheckedChangeListener(listener);
    }

    @Override
    public void setDisable() {
        if (mHandler != null) {
            ultrasonicOff();
        }
        toggleBt = view.findViewById(R.id.autoSwitch);
        toggleBt.setOnCheckedChangeListener(null);
        toggleBt.setChecked(false);
        toggleBt.setEnabled(false);
    }

    @Override
    public void setEchoValue(String value) {
        TextView txt = view.findViewById(R.id.textValue);
        float mCurrentValue = Float.parseFloat(value);
        txt.setText(Math.floor(mCurrentValue * 10.0) / 10.0 + " cm");
    }

    private void ultrasonicOn() {
        byte[] wr = buildWrite(DEV_COMMON_CMD, megaPiMode, 0, DEV_ULTRASONIC);
        mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
    }

    private void ultrasonicOff() {
        byte[] wr = buildWrite(DEV_COMMON_CMD, megaPiMode, 0, DEV_CAR_CONTROLLER);
        mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
    }

}

package cc.makeblock.modules;

import org.json.JSONObject;

import android.os.Handler;
import android.widget.SeekBar;
import android.widget.TextView;

import cc.makeblock.makeblock.R;

public class MeServoMotor extends MeModule implements SeekBar.OnSeekBarChangeListener {
    static String devName = "servo";
    SeekBar slider;
    TextView valueTxt;
    static int servoCount = 0;
    int servoIndex;

    public MeServoMotor(int port, int slot) {
        super(devName, MeModule.DEV_SERVO, port, slot);
        viewLayout = R.layout.dev_slider_view;
        imageId = R.drawable.servo;
        shouldSelectSlot = true;
    }

    public MeServoMotor(JSONObject jObj) {
        super(jObj);
        viewLayout = R.layout.dev_slider_view;
        imageId = R.drawable.servo;
        shouldSelectSlot = true;
    }

    @Override
    public void setEnable(Handler handler) {
        mHandler = handler;
        valueTxt = view.findViewById(R.id.slideBarValue);
        slider = view.findViewById(R.id.sliderBar);
        slider.setOnSeekBarChangeListener(this);
        slider.setProgress(0);
    }

    @Override
    public void setDisable() {
        slider = view.findViewById(R.id.sliderBar);
        slider.setOnSeekBarChangeListener(null);
        servoCount = 0;
    }

    long cTime = System.currentTimeMillis();

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int value = (int) ((float) progress / 512 * 180);
        if (valueTxt != null) {
            if (System.currentTimeMillis() - cTime > 80) {
                cTime = System.currentTimeMillis();
                valueTxt.setText(value);
                byte[] wr = buildWrite(type, port, slot, value);
                mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //slider.setProgress(0);
    }

    //@"servorun(%d,%c)\n",servoCount,variableChar
    @Override
    public String getScriptRun(String var) {
        varReg = var;
        return "servorun(" + servoIndex + "," + var + ")\n";
    }

    //@"servoattach(%@,%@,%d)\n",portStr,slotStr,servoCount]
    @Override
    public String getScriptSetup() {
        servoIndex = servoCount++;
        return "servoattach(" + getPortString(port) + "," + getSlotString(slot) + ","
                + servoIndex + ")\n";
    }

}

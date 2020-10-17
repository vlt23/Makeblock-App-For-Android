package cc.makeblock.modules;

import android.os.Looper;
import org.json.JSONObject;

import android.os.Handler;
import android.widget.SeekBar;
import android.widget.TextView;

import cc.makeblock.makeblock.R;

public class MeDcMotor extends MeModule implements SeekBar.OnSeekBarChangeListener {
    static String devName = "dcmotor";
    SeekBar slider;
    TextView valueTxt;
    private final Handler mStopHandler = new Handler(Looper.getMainLooper());
    private final Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            byte[] wr = buildWrite(type, port, slot, 0);
            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
        }
    };

    public MeDcMotor(int port, int slot) {
        super(devName, MeModule.DEV_DCMOTOR, port, slot);
        viewLayout = R.layout.dev_slider_view;
        imageId = R.drawable.motor;
    }

    public MeDcMotor(JSONObject jobj) {
        super(jobj);
        viewLayout = R.layout.dev_slider_view;
        imageId = R.drawable.motor;
    }

    @Override
    public void setEnable(Handler handler) {
        mHandler = handler;
        valueTxt = view.findViewById(R.id.slideBarValue);
        slider = view.findViewById(R.id.sliderBar);
        slider.setOnSeekBarChangeListener(this);
        slider.setProgress(256);
    }

    @Override
    public void setDisable() {
        slider = view.findViewById(R.id.sliderBar);
        slider.setOnSeekBarChangeListener(null);
    }

    long cTime = System.currentTimeMillis();

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        int value = progress - 256;
        if (valueTxt != null) {
            if (System.currentTimeMillis() - cTime > 100) {
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
        slider.setProgress(256);
    }

    //dcrun(%@,%c)
    @Override
    public String getScriptRun(String var) {
        varReg = var;
        return "dcrun(" + getPortString(port) + "," + var + ")\n";
    }

}

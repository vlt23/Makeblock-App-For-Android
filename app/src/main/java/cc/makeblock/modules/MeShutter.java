package cc.makeblock.modules;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import cc.makeblock.makeblock.R;

public class MeShutter extends MeModule implements SeekBar.OnSeekBarChangeListener {
    static String devName = "shutter";
    private EditText intervalText;
    private SeekBar timeSeekBar;
    private EditText timeText;
    private EditText startText;
    private Switch startSwitch;
    private Timer mTimer;
    private TimerTask mTask;
    private int mIndex;
    private Handler uiHandler;

    public MeShutter(int port, int slot) {
        super(devName, MeModule.DEV_SHUTTER, port, slot);
        viewLayout = R.layout.dev_shutter;
        imageId = R.drawable.shutter;
        shouldSelectSlot = true;
        mTimer = new Timer();
        setEnable(null);
    }

    public MeShutter(JSONObject jObj) {
        super(jObj);
        viewLayout = R.layout.dev_shutter;
        imageId = R.drawable.shutter;
        shouldSelectSlot = true;
        mTimer = new Timer();
        mTask = new TimerTask() {
            @Override
            public void run() {
                doShutter();
            }
        };
        setEnable(null);
    }

    @Override
    public void setEnable(Handler handler) {
        mHandler = handler;
        if (view == null) {
            return;
        }
        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 2:
                        startSwitch.setChecked(false);
                        break;
                    default:
                        break;
                }
            }
        };
        intervalText = view.findViewById(R.id.intervalText);
        timeSeekBar = view.findViewById(R.id.timeSeekBar);
        timeSeekBar.setOnSeekBarChangeListener(this);
//		timeSeekBar.setProgress(5);
        timeText = view.findViewById(R.id.timeText);
        startText = view.findViewById(R.id.startText);
        startSwitch = view.findViewById(R.id.startSwitch);
        OnCheckedChangeListener listener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                mIndex = 0;
                Log.d("mb", startText.getText().toString() + ":" + intervalText.getText().toString());
                if (arg1) {
                    if (mTimer != null) {
                        long interval = Integer.parseInt(startText.getText().toString()) * 1000;
                        long during = Integer.parseInt(intervalText.getText().toString()) * 1000;
                        Log.d("mb", "interval:" + interval + " - during:" + during);
                        mTask = new TimerTask() {
                            public void run() {
                                doShutter();
                            }
                        };
                        mTimer.cancel();
                        mTimer = new Timer();
                        mTimer.schedule(mTask, interval, during);
                    }
                } else {
                    if (mTimer != null) {
                        mTimer.cancel();
                    }
                }
            }
        };
        startSwitch.setOnCheckedChangeListener(listener);
    }

    private void doShutter() {
        mIndex++;
        byte[] wr = buildWrite(type, port, slot, 1);
        mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();

        if (Integer.parseInt(timeText.getText().toString()) > 0) {
            if (mIndex >= Integer.parseInt(timeText.getText().toString())) {
                Log.d("mb", "finish");
                if (mTimer != null)
                    mTimer.cancel();
                if (uiHandler != null) {
                    Message message = Message.obtain(uiHandler, 2);
                    uiHandler.sendMessage(message);
                }
            }
        }
    }

    @Override
    public void setDisable() {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        timeText.setText(progress);
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
//		varReg = var;
        return "shutter()\n";
    }

    //@"servoattach(%@,%@,%d)\n",portStr,slotStr,servoCount]
    @Override
    public String getScriptSetup() {
//		servoIndex = servoCount++;
        return "shutter()\n";
    }

}

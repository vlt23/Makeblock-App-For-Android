package cc.makeblock.modules;

import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONObject;

import cc.makeblock.makeblock.MeDevice;
import cc.makeblock.makeblock.R;

public class MeGripper extends MeModule {
    static String devName = "gripper";
    private ImageButton mLeftGripperBtn;  // gripper opening
    private ImageButton mRightGripperBtn;  // gripper closing
    private ImageButton mSpeedButton;
    private TextView mSpeedLabel;
    private TextView mPortLabel;
    private int motorSpeed = 100;

    private Handler mStopHandler = new Handler();
    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            byte[] wr = buildWrite(DEV_DCMOTOR, port, slot, 0);
            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
        }
    };

    public MeGripper(int port, int slot) {
        super(devName, MeModule.DEV_DCMOTOR, port, slot);
        viewLayout = R.layout.dev_gripper_controller;
        this.scale = 1.33f;
    }

    public MeGripper(JSONObject jobj) {
        super(jobj);
        viewLayout = R.layout.dev_gripper_controller;
        this.scale = 1.33f;
    }

    public void setEnable(Handler handler) {
        mHandler = handler;
        mSpeedLabel = view.findViewById(R.id.speedLabel);
        mLeftGripperBtn = view.findViewById(R.id.leftGripperBtn);
        mRightGripperBtn = view.findViewById(R.id.rightGripperBtn);
        mSpeedButton = view.findViewById(R.id.speedButton);
        mPortLabel = view.findViewById(R.id.textPort);
        if (port != 12) {  // workaround patch, check MeModule class
            mPortLabel.setText((port > 8 ? ("M" + (port - 8)) : ("PORT " + port)));
        }
        View.OnTouchListener touchListener = new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent evt) {
                if (evt.getAction() == MotionEvent.ACTION_UP) {
                    //MeDevice.sharedManager().manualMode = false;
                    byte[] wr = buildWrite(DEV_DCMOTOR, port, slot, 0);
                    mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                    mStopHandler.postDelayed(mStopRunnable, 150);
                    return true;
                } else if (evt.getAction() == MotionEvent.ACTION_DOWN) {
                    //MeDevice.sharedManager().manualMode = true;
                    Log.d("mb", "port:" + port);
                    if (v.equals(mLeftGripperBtn)) {
                        byte[] wr = buildWrite(DEV_DCMOTOR, port, slot, -motorSpeed);
                        mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                    } else if (v.equals(mRightGripperBtn)) {
                        byte[] wr = buildWrite(DEV_DCMOTOR, port, slot, motorSpeed);
                        mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                    }
                }
                return false;
            }
        };
        mLeftGripperBtn.setClickable(true);
        mRightGripperBtn.setClickable(true);
        mLeftGripperBtn.setEnabled(true);
        mRightGripperBtn.setEnabled(true);

        mLeftGripperBtn.setOnTouchListener(touchListener);
        mRightGripperBtn.setOnTouchListener(touchListener);

        mSpeedLabel.setText("Speed:" + motorSpeed);
        mSpeedButton.setClickable(true);
        mSpeedButton.setEnabled(true);
        mSpeedButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View arg0, MotionEvent evt) {
                int x = (int) evt.getX();
                int y = (int) evt.getY();
                //Log.d("mb", ""+x+":"+y);
                if (y < 48) {
                    motorSpeed += 4;
                } else {
                    motorSpeed -= 4;
                }

                motorSpeed = motorSpeed > 255 ? 255 : (Math.max(motorSpeed, 0));
                MeDevice.sharedManager().motorSpeed = motorSpeed;
                mSpeedLabel.setText("Speed:" + motorSpeed);
                return false;
            }
        });
        mSpeedButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Log.d("mb", "speed");
            }
        });
    }

    public void setDisable() {
        mLeftGripperBtn = view.findViewById(R.id.leftGripperBtn);
        mRightGripperBtn = view.findViewById(R.id.rightGripperBtn);
        mSpeedButton = view.findViewById(R.id.speedButton);
        mSpeedLabel = view.findViewById(R.id.speedLabel);
        mPortLabel = view.findViewById(R.id.textPort);
        mLeftGripperBtn.setClickable(false);
        mRightGripperBtn.setClickable(false);
        mLeftGripperBtn.setEnabled(false);
        mRightGripperBtn.setEnabled(false);
        mSpeedButton.setClickable(false);
        mSpeedButton.setEnabled(false);
        mSpeedLabel.setText("Speed:" + motorSpeed);
        mPortLabel.setText((port > 8 ? ("M" + (port - 8)) : ("PORT " + port)));
    }

}

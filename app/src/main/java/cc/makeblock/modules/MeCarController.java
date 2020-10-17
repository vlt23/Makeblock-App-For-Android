package cc.makeblock.modules;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONObject;

import cc.makeblock.makeblock.MeDevice;
import cc.makeblock.makeblock.R;

public class MeCarController extends MeModule {
    static String devName = "carcontroller";
    private ImageButton mLeftUpButton;
    private ImageButton mLeftDownButton;
    private ImageButton mRightUpButton;
    private ImageButton mRightDownButton;
    private ImageButton mLeftButton;
    private ImageButton mDownButton;
    private ImageButton mRightButton;
    private ImageButton mUpButton;
    private ImageButton mSpeedButton;
    private TextView mSpeedLabel;
    private int motorSpeed = 180;
    private final Handler mStopHandler = new Handler(Looper.getMainLooper());
    private final Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            byte[] wr = buildJoystickWrite(DEV_JOYSTICK, 0, 0);
            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
        }
    };

    public MeCarController(int port, int slot) {
        super(devName, MeModule.DEV_DCMOTOR, port, slot);
        viewLayout = R.layout.dev_car_controller;
        this.scale = 1.33f;
    }

    public MeCarController(JSONObject jobj) {
        super(jobj);
        viewLayout = R.layout.dev_car_controller;
        this.scale = 1.33f;
    }

    long ctime = System.currentTimeMillis();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setEnable(Handler handler) {
        mHandler = handler;
        findAllViewById();
        mLeftUpButton.setClickable(true);
        mRightUpButton.setClickable(true);
        mLeftDownButton.setClickable(true);
        mRightDownButton.setClickable(true);
        mLeftUpButton.setEnabled(true);
        mRightUpButton.setEnabled(true);
        mLeftDownButton.setEnabled(true);
        mRightDownButton.setEnabled(true);
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent evt) {
                v.performClick();
                if (evt.getAction() == MotionEvent.ACTION_UP) {
                    MeDevice.sharedManager().manualMode = false;
                    byte[] wr = buildJoystickWrite(DEV_JOYSTICK, 0, 0);
                    mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                    mStopHandler.postDelayed(mStopRunnable, 150);
                    return true;
                } else if (evt.getAction() == MotionEvent.ACTION_DOWN) {
                    if (System.currentTimeMillis() - ctime > 80) {
                        ctime = System.currentTimeMillis();
                        MeDevice.sharedManager().manualMode = true;
                        if (v.equals(mLeftUpButton)) {
                            byte[] wr = buildJoystickWrite(DEV_JOYSTICK, motorSpeed / 2, motorSpeed);
                            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                        } else if (v.equals(mLeftDownButton)) {
                            byte[] wr = buildJoystickWrite(DEV_JOYSTICK, -motorSpeed / 2, -motorSpeed);
                            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                        } else if (v.equals(mRightUpButton)) {
                            byte[] wr = buildJoystickWrite(DEV_JOYSTICK, motorSpeed, motorSpeed / 2);
                            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                        } else if (v.equals(mRightDownButton)) {
                            byte[] wr = buildJoystickWrite(DEV_JOYSTICK, -motorSpeed, -motorSpeed / 2);
                            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                        } else if (v.equals(mLeftButton)) {
                            byte[] wr = buildJoystickWrite(DEV_JOYSTICK, -motorSpeed, motorSpeed);
                            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                        } else if (v.equals(mRightButton)) {
                            byte[] wr = buildJoystickWrite(DEV_JOYSTICK, motorSpeed, -motorSpeed);
                            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                        } else if (v.equals(mUpButton)) {
                            byte[] wr = buildJoystickWrite(DEV_JOYSTICK, motorSpeed, motorSpeed);
                            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                        } else if (v.equals(mDownButton)) {
                            byte[] wr = buildJoystickWrite(DEV_JOYSTICK, -motorSpeed, -motorSpeed);
                            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
                        }
                    }
                }
                return false;
            }
        };
        mLeftButton.setClickable(true);
        mRightButton.setClickable(true);
        mUpButton.setClickable(true);
        mDownButton.setClickable(true);
        mLeftButton.setEnabled(true);
        mRightButton.setEnabled(true);
        mUpButton.setEnabled(true);
        mDownButton.setEnabled(true);

        mLeftButton.setOnTouchListener(touchListener);
        mRightButton.setOnTouchListener(touchListener);
        mUpButton.setOnTouchListener(touchListener);
        mDownButton.setOnTouchListener(touchListener);
        mLeftUpButton.setOnTouchListener(touchListener);
        mRightUpButton.setOnTouchListener(touchListener);
        mLeftDownButton.setOnTouchListener(touchListener);
        mRightDownButton.setOnTouchListener(touchListener);

        mSpeedButton.setClickable(true);
        mSpeedButton.setEnabled(true);
        mSpeedButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent evt) {
                v.performClick();
                int y = (int) evt.getY();
                //Log.d("mb", ""+x+":"+y);
                if (y < 48) {
                    motorSpeed += 4;
                } else {
                    motorSpeed -= 4;
                }

                motorSpeed = motorSpeed > 255 ? 255 : (Math.max(motorSpeed, 0));
                MeDevice.sharedManager().motorSpeed = motorSpeed;
                mSpeedLabel = view.findViewById(R.id.speedLabel);
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

    private void findAllViewById() {
        mLeftUpButton = view.findViewById(R.id.leftUpButton);
        mRightUpButton = view.findViewById(R.id.rightUpButton);
        mLeftDownButton = view.findViewById(R.id.leftDownButton);
        mRightDownButton = view.findViewById(R.id.rightDownButton);
        mLeftButton = view.findViewById(R.id.leftButton);
        mRightButton = view.findViewById(R.id.rightButton);
        mUpButton = view.findViewById(R.id.upButton);
        mDownButton = view.findViewById(R.id.downButton);
        mSpeedButton = view.findViewById(R.id.speedButton);
    }

    @Override
    public void setDisable() {
        findAllViewById();
        mLeftUpButton.setOnClickListener(null);
        mRightUpButton.setOnClickListener(null);
        mLeftDownButton.setOnClickListener(null);
        mRightDownButton.setOnClickListener(null);
        mLeftButton.setOnClickListener(null);
        mRightButton.setOnClickListener(null);
        mUpButton.setOnClickListener(null);
        mDownButton.setOnClickListener(null);
        mSpeedButton.setOnClickListener(null);
        mLeftUpButton.setClickable(false);
        mRightUpButton.setClickable(false);
        mLeftDownButton.setClickable(false);
        mRightDownButton.setClickable(false);
        mLeftUpButton.setEnabled(false);
        mRightUpButton.setEnabled(false);
        mLeftDownButton.setEnabled(false);
        mRightDownButton.setEnabled(false);
        mLeftButton.setClickable(false);
        mRightButton.setClickable(false);
        mUpButton.setClickable(false);
        mDownButton.setClickable(false);
        mLeftButton.setEnabled(false);
        mRightButton.setEnabled(false);
        mUpButton.setEnabled(false);
        mDownButton.setEnabled(false);
        mSpeedButton.setClickable(false);
        mSpeedButton.setEnabled(false);
    }

}

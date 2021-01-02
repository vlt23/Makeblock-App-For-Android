package cc.makeblock.modules;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import cc.makeblock.makeblock.MeDevice;
import cc.makeblock.makeblock.R;
import io.github.controlwear.virtual.joystick.android.JoystickView;
import org.json.JSONObject;

public class MeCarController extends MeModule {
    static String devName = "carcontroller";

    private JoystickView joystickView;

    private final Handler mStopHandler = new Handler(Looper.getMainLooper());
    private final Runnable mStopRunnable = () -> {
        byte[] wr = buildJoystickWrite(DEV_JOYSTICK, 0, 0);
        mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
    };

    private boolean isPreviousStopped;  // Prevent loop sending 0 for leftSpeed and rightSpeed

    public MeCarController(int port, int slot) {
        super(devName, MeModule.DEV_JOYSTICK, port, slot);
        initRestComponents();
    }

    public MeCarController(JSONObject jObj) {
        super(jObj);
        initRestComponents();
    }

    private void initRestComponents() {
        viewLayout = R.layout.dev_car_controller;
        this.scale = 1.33f;
    }

    @Override
    public void setEnable(Handler handler) {
        mHandler = handler;
        joystickView = view.findViewById(R.id.joystickCar);
        joystickView.setEnabled(true);
        joystickView.setFixedCenter(true);  // set fix (0, 0) center
        joystickView.setAutoReCenterButton(true);

        View.OnTouchListener touchListener = (v, evt) -> {
            v.performClick();
            int action = evt.getAction();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                // Reset the joystick to the center
                joystickView.resetButtonPosition();
                stop();
                return true;
            }
            return false;
        };

        joystickView.setOnTouchListener(touchListener);

        joystickView.setOnMoveListener((angle, strength) -> {
            if (strength < 10) {
                if (!isPreviousStopped) {
                    stop();
                }
            } else {
                Log.d(devName, "angle: " + angle + ", strength: " + strength);
                int speed = (int) (strength * 1.2);
                int leftSpeed, rightSpeed;
                float linearInterpolator;  // Y = ( ( X - X1 )( Y2 - Y1) / ( X2 - X1) ) + Y1
                if (angle < 90) {
                    linearInterpolator = ((float) angle - 0) * (-1 - 1) / (90) + 1;
                    leftSpeed = (int) (linearInterpolator * speed);
                    rightSpeed = speed;
                } else if (angle < 180) {
                    leftSpeed = -speed;
                    linearInterpolator = ((float) angle - 90) * (-1 - 1) / (180 - 90) + 1;
                    rightSpeed = (int) (linearInterpolator * speed);
                } else if (angle < 270) {
                    linearInterpolator = ((float) angle - 180) * (1 - (-1)) / (270 - 180) - 1;
                    leftSpeed = (int) (linearInterpolator * speed);
                    rightSpeed = -speed;
                } else {
                    leftSpeed = speed;
                    linearInterpolator = ((float) angle - 270) * (1 - (-1)) / (360 - 270) - 1;
                    rightSpeed = (int) (linearInterpolator * speed);
                }
                Log.d(devName, "linearInterpolator: " + linearInterpolator);
                Log.d(devName, "leftSpeed: " + leftSpeed + ", rightSpeed: " + rightSpeed);
                isPreviousStopped = false;
                byte[] wr = buildJoystickWrite(DEV_JOYSTICK, leftSpeed, rightSpeed);
                mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
            }
        }, 100);
    }

    @Override
    public void setDisable() {
        joystickView = view.findViewById(R.id.joystickCar);
        joystickView.setOnTouchListener(null);
        joystickView.setOnMoveListener(null);
        joystickView.setEnabled(false);
    }

    private void stop() {
        MeDevice.sharedManager().manualMode = false;
        byte[] wr = buildJoystickWrite(DEV_JOYSTICK, 0, 0);
        mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
        mStopHandler.postDelayed(mStopRunnable, 150);
        isPreviousStopped = true;
    }

}

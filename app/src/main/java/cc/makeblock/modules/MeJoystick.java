package cc.makeblock.modules;

import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.json.JSONObject;

import cc.makeblock.makeblock.R;

public class MeJoystick extends MeModule implements OnTouchListener {
    static String devName = "joystick";
    ImageView bar;
    int lastX, lastY, originX, originY;
    int lastMotorL, lastMotorR;
    int initTop, initLeft;
    int lastTop, lastLeft;
    long lastTime;

    public MeJoystick(int port, int slot) {
        super(devName, MeModule.DEV_JOYSTICK, port, slot);
        viewLayout = R.layout.dev_joystick_view;
        this.scale = 0.9f;
    }

    public MeJoystick(JSONObject jObj) {
        super(jObj);
        viewLayout = R.layout.dev_joystick_view;
        this.scale = 0.9f;
    }

    @Override
    public void setEnable(Handler handler) {
        mHandler = handler;
        if (view == null) {
            return;
        }
        bar = view.findViewById(R.id.joystickBar);
        initTop = bar.getTop();
        initLeft = bar.getLeft();
        bar.setOnTouchListener(this);
    }

    @Override
    public void setDisable() {
        if (bar != null) {
            bar.setOnTouchListener(null);
        }
    }

    @Override
    public String getScriptRun(String var, String var2) {
        varReg = var;
        varReg2 = var2;
        return "dcrun(m1," + var + ")\n" + "dcrun(m2," + var2 + ")\n";
    }

    void sendXY(int x, int y) {
        if (mHandler == null) {
            return;
        }
        //Log.i(dbg, "joystick x="+x+" y="+y);

        int dx = -x + 100;
        int dy = -y + 100;
        int motorL, motorR;

        motorL = dy;
        motorR = dy;
        motorL -= dx;
        motorR += dx;
        if (lastMotorL != motorL || lastMotorR != motorR) {
            //Log.i(dbg, "joystick l="+motorL+" r="+motorR);
            byte[] wr = buildWrite(DEV_DCMOTOR, PORT_M1, slot, motorL);
            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
            byte[] wr2 = buildWrite(DEV_DCMOTOR, PORT_M2, slot, motorR);
            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr2).sendToTarget();
            lastMotorL = motorL;
            lastMotorR = motorR;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        v.performClick();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = (int) event.getRawX();
                lastY = (int) event.getRawY();
                originX = lastX;
                originY = lastY;
                lastTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = (int) event.getRawX() - lastX;
                int dy = (int) event.getRawY() - lastY;
                int left = v.getLeft() + dx;
                int top = v.getTop() + dy;
                if (left < 0) {
                    left = 0;
                }
                if (left > 200) {
                    left = 200;
                }
                if (top < 0) {
                    top = 0;
                }
                if (top > 200) {
                    top = 200;
                }
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();
                params.gravity = Gravity.START | Gravity.TOP;
                params.topMargin = top;
                params.leftMargin = left;
                if (top != lastTop || left != lastLeft) {
                    v.setLayoutParams(params);
                    lastX = (int) event.getRawX();
                    lastY = (int) event.getRawY();
                    long time = System.currentTimeMillis();
                    if ((time - lastTime) > 100) {
                        sendXY(left, top);
                        lastTime = time;
                    }
                    lastTop = top;
                    lastLeft = left;
                }
                break;
            case MotionEvent.ACTION_UP:
                left = 100;//v.getLeft() + dx;
                top = 100;//v.getTop() + dy;
                params = (FrameLayout.LayoutParams) v.getLayoutParams();
                params.gravity = Gravity.START | Gravity.TOP;
                params.topMargin = initTop;
                params.leftMargin = initLeft;
                sendXY(left, top);
                v.setLayoutParams(params);
                break;
        }
        return true;
    }

}
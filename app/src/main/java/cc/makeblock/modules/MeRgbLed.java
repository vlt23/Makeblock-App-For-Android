package cc.makeblock.modules;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import org.json.JSONObject;

import cc.makeblock.makeblock.R;

public class MeRgbLed extends MeModule implements OnTouchListener {
    static String devName = "rgbled";
    ImageView hue;
    ImageView mask;
    Bitmap bitmap;

    public MeRgbLed(int port, int slot) {
        super(devName, MeModule.DEV_RGBLED, port, slot);
        viewLayout = R.layout.dev_rgb_view;
        imageId = R.drawable.rgbled;
        shouldSelectSlot = true;
        this.scale = 1.0f;
    }

    public MeRgbLed(JSONObject jObj) {
        super(jObj);
        viewLayout = R.layout.dev_rgb_view;
        imageId = R.drawable.rgbled;
        shouldSelectSlot = true;
        this.scale = 1.0f;
    }

    public void setEnable(Handler handler) {
        mHandler = handler;
        hue = view.findViewById(R.id.rgbColorPick);
        bitmap = ((BitmapDrawable) hue.getDrawable()).getBitmap();
        hue.setOnTouchListener(this);
        mask = view.findViewById(R.id.rgbMask);
    }

    public void setDisable() {
        if (hue != null)
            hue.setOnTouchListener(null);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                // the original hue image is in 80x80
                try {
                    int x = (int) (80 * event.getX()) / v.getMeasuredWidth();
                    int y = (int) (80 * event.getY()) / v.getMeasuredHeight();
//	            Log.d("mb", ""+x+":"+y);
                    x += 15;
                    y += 15;

                    int pixel = bitmap.getPixel(x, y);
                    int R = (int) (Color.red(pixel) * 0.6);
                    int G = (int) (Color.green(pixel) * 0.6);
                    int B = (int) (Color.blue(pixel) * 0.6);
                    sendColor(R, G, B);
                    mask.setColorFilter(pixel);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
        return true;
    }

    //rgbrun(%@,%@,%c)
    public String getScriptRun(String var) {
        varReg = var;
        return "rgbrun(" + getPortString(port) + "," + getSlotString(slot) + "," + var + ")\n";
    }

    long cTime = System.currentTimeMillis();

    void sendColor(int r, int g, int b) {
        if (System.currentTimeMillis() - cTime > 80) {
            cTime = System.currentTimeMillis();
            int iRGB = (r << 8) + (g << 16) + (b << 24);
            byte[] wr = buildWrite(type, port, slot, iRGB);
            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
        }
    }
}

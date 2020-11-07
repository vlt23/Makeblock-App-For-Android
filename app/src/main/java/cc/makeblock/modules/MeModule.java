package cc.makeblock.modules;

import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;

import cc.makeblock.makeblock.MainActivity;
import cc.makeblock.makeblock.MeDevice;
import cc.makeblock.makeblock.R;

public class MeModule {
    static final String dbg = "MeModule";
    // should be same to ios code
    public static final int DEV_VERSION = 0;
    public static final int DEV_ULTRASONIC = 1;
    public static final int DEV_TEMPERATURE = 2;
    public static final int DEV_LIGHTSENSOR = 3;
    public static final int DEV_POTENTIALMETER = 4;
    public static final int DEV_JOYSTICK = 5;
    public static final int DEV_GYRO = 6;
    public static final int DEV_SOUNDSENSOR = 7;
    public static final int DEV_RGBLED = 8;
    public static final int DEV_SEVSEG = 9;
    public static final int DEV_DCMOTOR = 10;
    public static final int DEV_SERVO = 11;
    public static final int DEV_ENCODER = 12;
    public static final int DEV_PIRMOTION = 15;
    public static final int DEV_INFRADRED = 16;
    public static final int DEV_LINEFOLLOWER = 17;
    public static final int DEV_BUTTON = 18;
    public static final int DEV_LIMITSWITCH = 19;
    public static final int DEV_SHUTTER = 20;
    public static final int DEV_PINDIGITAL = 30;
    public static final int DEV_PINANALOG = 31;
    public static final int DEV_PINPWM = 32;
    public static final int DEV_PINANGLE = 33;
    public static final int DEV_CAR_CONTROLLER = 40;
    public static final int DEV_GRIPPER_CONTROLLER = 41;

    public static final int SLOT_1 = 1; // 0
    public static final int SLOT_2 = 2; // 1

    public static final int READ_MODULE = 1;
    public static final int WRITE_MODULE = 2;

    public static final int VERSION_INDEX = 0xfa;

    public static final int PORT_1 = 1;
    public static final int PORT_2 = 2;
    public static final int PORT_3 = 3;
    public static final int PORT_4 = 4;
    public static final int PORT_5 = 5;  // unused ???
    public static final int PORT_6 = 6;  // unused ???
    public static final int PORT_7 = 7;  // unused ???
    public static final int PORT_8 = 8;  // unused ???
    public static final int PORT_M1 = 9;
    public static final int PORT_M2 = 10;
    // Begin patch for Ultimate Robot Kit v2.0
    public static final int PORT_M3 = 11;  // == PORT_11
    public static final int PORT_12 = 12;  // == PORT_M4
    // End patch for Ultimate Robot Kit v2.0

    public static final int MSG_VALUE_CHANGED = 0x10;

    public String name;
    public int port;
    public int xPosition;
    public int yPosition;
    public int type;
    public int slot;
    public int scaleType;
    public float scale = 1;

    public boolean shouldSelectSlot = false;

    // view related
    public int viewLayout;
    public int imageId;
    public View view;
    public String varReg = "";
    public String varReg2 = "";
    protected Handler mHandler;

    public MeModule(String name, int type, int port, int slot) {
        this.name = name;
        this.type = type;
        this.port = port;
        this.slot = slot;
        this.xPosition = 100;
        this.yPosition = 100;
    }

    public MeModule(JSONObject json) {
        try {
            name = json.getString("name");
            port = json.getInt("port");
            type = json.getInt("type");
            slot = json.getInt("slot");
            xPosition = json.getInt("xPosition");
            yPosition = json.getInt("yPosition");
            //Log.i(dbg, "x="+xPosition);
            //Log.i(dbg, "y="+yPosition);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setScaleType(int type) {
        if (type == 1 || type == 40) {
            yPosition = (int) (yPosition * MainActivity.screenWidth / 1196.0);
            xPosition = (int) (xPosition * (MainActivity.screenHeight - 120) / 600.0);
        }
        update();
    }

    public void setViewPortString(int port) {
        if (view == null) {
            return;
        }
        TextView textPort = view.findViewById(R.id.textPort);
        if (textPort == null) {
            return;
        }
        if (port < MeModule.PORT_M1 ||
                port == MeModule.PORT_12) {  // Because the gripper doesn't use the M4 (== port 12)
            if (type == MeModule.DEV_DCMOTOR) {
                textPort.setText("PORT " + port);
            } else {
                textPort.setText(Integer.toString(port));
            }
        } else if (port == MeModule.PORT_M1) {
            textPort.setText("M1");
        } else if (port == MeModule.PORT_M2) {
            textPort.setText("M2");
        } else if (port == MeModule.PORT_M3) {
            textPort.setText("M3");
        }
    }

    public void setViewPortImage(int imageId) {
        if (view == null || imageId == 0) {
            return;
        }
        ImageView img = view.findViewById(R.id.imageDevice);
        img.setImageResource(imageId);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", name);
            json.put("port", port);
            json.put("xPosition", xPosition);
            json.put("yPosition", yPosition);
            json.put("type", type);
            json.put("slot", slot);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public String getScriptRun(String var) {
        return "";
    }

    public String getScriptRun(String var, String var2) {
        return "";
    }

    public String getScriptSetup() {
        return "";
    }

    public byte[] getQuery(int index) {
        return null;
    }

    public String toString() {
        return this.toJson().toString();
    }

    public void setEchoValue(String value) {
    }

    public void setEnable(Handler handler) {
    }

    public void setDisable() {
    }

    public void update() {
        if (this.view != null) {
            this.view.setScaleX(MeDevice.sharedManager().scale * scale);
            this.view.setScaleY(MeDevice.sharedManager().scale * scale);
        }
    }

    static public byte[] buildQuery(int type, int port, int slot, int index) {
        //tx:FF 55 01 04 01 60 00 0A
        byte[] cmd = new byte[9];
        cmd[0] = (byte) 0xff;
        cmd[1] = (byte) 0x55;
        cmd[2] = (byte) 5;
        cmd[3] = (byte) index;
        cmd[4] = (byte) READ_MODULE;
        cmd[5] = (byte) type;
        cmd[6] = (byte) (port & 0xff);
        cmd[7] = (byte) (slot & 0xff);
        cmd[8] = (byte) '\n';

        return cmd;
    }

    static public byte[] buildWrite(int type, int port, int slot, float f) {
        byte[] cmd = new byte[13];
        //unsigned char a[11]={0xff,0x55,WRITEMODULE,7,0,0,0,0,0,0,'\n'};
        //a[4] = [type intValue];
        //a[5] = (port<<4 & 0xf0)|(slot & 0xf);
		/*
		ff 55 len idx action device port  slot  data a
		0  1  2   3   4      5      6     7     8
		*/
        cmd[0] = (byte) 0xff;
        cmd[1] = (byte) 0x55;
        cmd[2] = (byte) 9;
        cmd[3] = (byte) 0;
        cmd[4] = (byte) WRITE_MODULE;
        cmd[5] = (byte) type;
        cmd[6] = (byte) (port & 0xff);
        if (type == DEV_SEVSEG) {
            int fi = Float.floatToIntBits(f);
            cmd[7] = (byte) (fi & 0xff);
            cmd[8] = (byte) ((fi >> 8) & 0xff);
            cmd[9] = (byte) ((fi >> 16) & 0xff);
            cmd[10] = (byte) ((fi >> 24) & 0xff);
        } else {
            cmd[7] = (byte) (slot & 0xff);
            int fi = Float.floatToIntBits(f);
            cmd[8] = (byte) (fi & 0xff);
            cmd[9] = (byte) ((fi >> 8) & 0xff);
            cmd[10] = (byte) ((fi >> 16) & 0xff);
            cmd[11] = (byte) ((fi >> 24) & 0xff);
        }
        cmd[12] = (byte) '\n';
        return cmd;
    }

    static public byte[] buildJoystickWrite(int type, int leftSpeed, int rightSpeed) {
        byte[] cmd = new byte[13];
        cmd[0] = (byte) 0xff;
        cmd[1] = (byte) 0x55;
        cmd[2] = (byte) 8;
        cmd[3] = (byte) 0;
        cmd[4] = (byte) WRITE_MODULE;
        cmd[5] = (byte) type;
        final ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putShort((short) leftSpeed);
        buf.putShort((short) rightSpeed);
        buf.position(0);
        // Read back bytes
        final byte b1 = buf.get();
        final byte b2 = buf.get();
        final byte b3 = buf.get();
        final byte b4 = buf.get();
        cmd[6] = b2;
        cmd[7] = b1;
        cmd[8] = b4;
        cmd[9] = b3;
        cmd[10] = (byte) '\n';
        return cmd;
    }

    static public byte[] buildWrite(int type, int port, int slot, int value) {
        byte[] cmd = new byte[13];
		/*
		ff 55 len idx action device port  slot  data a
		0  1  2   3   4      5      6     7     8
		*/
        //unsigned char a[11]={0xff,0x55,WRITEMODULE,7,0,0,0,0,0,0,'\n'};
        //a[4] = [type intValue];
        //a[5] = (port<<4 & 0xf0)|(slot & 0xf);
        cmd[0] = (byte) 0xff;
        cmd[1] = (byte) 0x55;
        cmd[2] = (byte) 9;
        cmd[3] = (byte) 0;
        cmd[4] = (byte) WRITE_MODULE;
        cmd[5] = (byte) type;
        cmd[6] = (byte) (port & 0xff);
        cmd[7] = (byte) (slot & 0xff);
        if (type == DEV_RGBLED) {
            cmd[7] = (byte) (0);
            cmd[8] = (byte) ((value >> 8) & 0xff);
            cmd[9] = (byte) ((value >> 16) & 0xff);
            cmd[10] = (byte) ((value >> 24) & 0xff);
        } else {
            if (type == DEV_DCMOTOR) {
                final ByteBuffer buf = ByteBuffer.allocate(2);
                buf.putShort((short) value);
                buf.position(0);
                // Read back bytes
                final byte b1 = buf.get();
                final byte b2 = buf.get();
                cmd[8] = b1;
                cmd[7] = b2;

            } else if (type == DEV_SERVO) {
                cmd[8] = (byte) (value & 0xff);
            } else {
                float f = (float) value;
                int fi = Float.floatToIntBits(f);
                cmd[8] = (byte) (fi & 0xff);
                cmd[9] = (byte) ((fi >> 8) & 0xff);
                cmd[10] = (byte) ((fi >> 16) & 0xff);
                cmd[11] = (byte) ((fi >> 24) & 0xff);
            }
        }

        cmd[12] = (byte) '\n';
        return cmd;
    }

    static public String getSlotString(int slot) {
        switch (slot) {
            case SLOT_1:
                return "slot1";
            case SLOT_2:
                return "slot2";
            default:
                return "";
        }
    }

    static public String getPortString(int port) {
        switch (port) {
            case MeModule.PORT_1:
                return "port1";
            case MeModule.PORT_2:
                return "port2";
            case MeModule.PORT_3:
                return "port3";
            case MeModule.PORT_4:
                return "port4";
            case MeModule.PORT_5:
                return "port5";
            case MeModule.PORT_6:
                return "port6";
            case MeModule.PORT_7:
                return "port7";
            case MeModule.PORT_8:
                return "port8";
            case MeModule.PORT_12:
                return "port12";
            case MeModule.PORT_M1:
                return "m1";
            case MeModule.PORT_M2:
                return "m2";
            case MeModule.PORT_M3:
                return "m3";
            default:
                return "";
        }
    }

}

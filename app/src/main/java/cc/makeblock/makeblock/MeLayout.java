package cc.makeblock.makeblock;

import android.util.Log;
import cc.makeblock.modules.MeButton;
import cc.makeblock.modules.MeCarController;
import cc.makeblock.modules.MeDigiSeg;
import cc.makeblock.modules.MeGripper;
import cc.makeblock.modules.MeLightSensor;
import cc.makeblock.modules.MeLimitSwitch;
import cc.makeblock.modules.MeLineFollower;
import cc.makeblock.modules.MeModule;
import cc.makeblock.modules.MePIRSensor;
import cc.makeblock.modules.MePotential;
import cc.makeblock.modules.MeRgbLed;
import cc.makeblock.modules.MeServoMotor;
import cc.makeblock.modules.MeSoundSensor;
import cc.makeblock.modules.MeTemperature;
import cc.makeblock.modules.MeUltrasonic;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MeLayout {
    static final String dbg = "Layout";
    public String name;
    public String createTime;
    public String updateTime;
    public int type = 0;
    public List<MeModule> moduleList;

    public MeLayout(JSONObject json) {
        try {
            name = json.getString("name");
            if (json.has("type")) {
                type = json.getInt("type");
            }
            createTime = json.getString("createTime");
            updateTime = json.getString("updateTime");
            JSONArray moduleListJArray = json.getJSONArray("moduleList");
            moduleList = new ArrayList<>();
            for (int i = 0; i < moduleListJArray.length(); i++) {
                JSONObject jObj = (JSONObject) moduleListJArray.get(i);

                int modType = jObj.getInt("type");

                MeModule mod = null;
                switch (modType) {
                    case MeModule.DEV_ULTRASONIC:
                        mod = new MeUltrasonic(jObj);
                        break;
                    case MeModule.DEV_TEMPERATURE:
                        mod = new MeTemperature(jObj);
                        break;
                    case MeModule.DEV_LIGHTSENSOR:
                        mod = new MeLightSensor(jObj);
                        break;
                    case MeModule.DEV_SOUNDSENSOR:
                        mod = new MeSoundSensor(jObj);
                        break;
                    case MeModule.DEV_LINEFOLLOWER:
                        mod = new MeLineFollower(jObj);
                        break;
                    case MeModule.DEV_POTENTIALMETER:
                        mod = new MePotential(jObj);
                        break;
                    case MeModule.DEV_LIMITSWITCH:
                        mod = new MeLimitSwitch(jObj);
                        break;
                    case MeModule.DEV_BUTTON:
                        mod = new MeButton(jObj);
                        break;
                    case MeModule.DEV_PIRMOTION:
                        mod = new MePIRSensor(jObj);
                        break;
                    case MeModule.DEV_GRIPPER_CONTROLLER:
                    case MeModule.DEV_DCMOTOR:
                        mod = new MeGripper(jObj);
                        break;
                    case MeModule.DEV_SERVO:
                        mod = new MeServoMotor(jObj);
                        break;
                    case MeModule.DEV_JOYSTICK:
                    case MeModule.DEV_CAR_CONTROLLER:
                        mod = new MeCarController(jObj);
                        break;
                    case MeModule.DEV_RGBLED:
                        mod = new MeRgbLed(jObj);
                        break;
                    case MeModule.DEV_SEVSEG:
                        mod = new MeDigiSeg(jObj);
                        break;
                    default:
                        Log.i(dbg, "unknown module from json " + modType);
                        break;
                }
                if (mod != null) {
                    mod.setScaleType(type);
                    moduleList.add(mod);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setName(String v) {
        name = v;
    }

    public String getTime() {
        Date date = new Date();
        DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return sdf.format(date);
    }

    public MeLayout(String name) {
        this.name = name;
        createTime = getTime();
        updateTime = createTime;
        moduleList = new ArrayList<>();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", name);
            json.put("createTime", createTime);
            json.put("updateTime", updateTime);
            JSONArray moduleListJArray = new JSONArray();
            // jArray sequence == arraylist sequence
            for (MeModule mod : moduleList) {
                moduleListJArray.put(mod.toJson());
            }
            json.put("moduleList", moduleListJArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public MeModule addModule(int type, int port, int slot, int x, int y) {
        MeModule mod;
        updateTime = getTime();
        switch (type) {
            case MeModule.DEV_ULTRASONIC:
                mod = new MeUltrasonic(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            case MeModule.DEV_TEMPERATURE:
                mod = new MeTemperature(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            case MeModule.DEV_LIGHTSENSOR:
                mod = new MeLightSensor(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            case MeModule.DEV_SOUNDSENSOR:
                mod = new MeSoundSensor(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            case MeModule.DEV_LINEFOLLOWER:
                mod = new MeLineFollower(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            case MeModule.DEV_POTENTIALMETER:
                mod = new MePotential(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            case MeModule.DEV_LIMITSWITCH:
                mod = new MeLimitSwitch(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            case MeModule.DEV_BUTTON:
                mod = new MeButton(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            case MeModule.DEV_PIRMOTION:
                mod = new MePIRSensor(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            case MeModule.DEV_GRIPPER_CONTROLLER:
            case MeModule.DEV_DCMOTOR:
                mod = new MeGripper(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            case MeModule.DEV_SERVO:
                mod = new MeServoMotor(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            case MeModule.DEV_JOYSTICK:
                mod = new MeCarController(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            case MeModule.DEV_RGBLED:
                mod = new MeRgbLed(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            case MeModule.DEV_SEVSEG:
                mod = new MeDigiSeg(port, slot);
                mod.xPosition = x;
                mod.yPosition = y;
                break;
            default:
                Log.i(dbg, "unknown module " + type);
                mod = new MeModule(updateTime, MeModule.DEV_VERSION, 0, 0);
                break;
        }
        moduleList.add(mod);
        return mod;
    }

    @Override
    public String toString() {
        return this.toJson().toString();
    }

}

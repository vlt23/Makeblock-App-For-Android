package cc.makeblock.modules;

import org.json.JSONObject;

import android.widget.TextView;

import cc.makeblock.makeblock.R;

public class MeSoundSensor extends MeModule {
    static String devName = "soundsensor";

    public MeSoundSensor(int port, int slot) {
        super(devName, MeModule.DEV_SOUNDSENSOR, port, slot);
        viewLayout = R.layout.dev_value_view;
        imageId = R.drawable.soundsensor;
    }

    public MeSoundSensor(JSONObject jobj) {
        super(jobj);
        viewLayout = R.layout.dev_value_view;
        imageId = R.drawable.soundsensor;
    }

    public String getScriptRun(String var) {
        varReg = var;
        return var + " = soundsensor(" + getPortString(port) + ")\n";
    }

    public byte[] getQuery(int index) {
        return buildQuery(type, port, slot, index);
    }

    public void setEchoValue(String value) {
        TextView txt = view.findViewById(R.id.textValue);
        txt.setText(value);
    }
}

package cc.makeblock.modules;

import org.json.JSONObject;

import android.widget.ImageView;

import cc.makeblock.makeblock.R;

public class MePIRSensor extends MeModule {
    static String devName = "pirsensor";

    public MePIRSensor(int port, int slot) {
        super(devName, MeModule.DEV_PIRMOTION, port, slot);
        viewLayout = R.layout.dev_switch_view;
        imageId = R.drawable.pirmotion;
    }

    public MePIRSensor(JSONObject jobj) {
        super(jobj);
        viewLayout = R.layout.dev_switch_view;
        imageId = R.drawable.pirmotion;
    }

    public String getScriptRun(String var) {
        varReg = var;
        return var + " = pirsensor(" + getPortString(port) + ")\n";
    }

    public byte[] getQuery(int index) {
        return buildQuery(type, port, slot, index);
    }

    public void setEchoValue(String value) {
        int v = (int) Float.parseFloat(value);
        ImageView img = view.findViewById(R.id.switchImg);
        if (v > 0) {
            img.setImageResource(R.drawable.switch_on);
        } else {
            img.setImageResource(R.drawable.switch_off);
        }
    }

}

package cc.makeblock.modules;

import org.json.JSONObject;

import android.widget.TextView;

import cc.makeblock.makeblock.R;

public class MeButton extends MeModule {
    static String devName = "button";

    public MeButton(int port, int slot) {
        super(devName, MeModule.DEV_BUTTON, port, slot);
        viewLayout = R.layout.dev_value_view;
        imageId = R.drawable.button;
    }

    public MeButton(JSONObject jobj) {
        super(jobj);
        viewLayout = R.layout.dev_value_view;
        imageId = R.drawable.button;
    }

    public String getScriptRun(String var) {
        varReg = var;
        return var + " = button(" + getPortString(port) + ")\n";
    }

    public byte[] getQuery(int index) {
        // use the lightsensor type to read adc value
        return buildQuery(DEV_LIGHTSENSOR, port, slot, index);
    }

    public void setEchoValue(String value) {
        float adc = Float.parseFloat(value);
        TextView txt = view.findViewById(R.id.textValue);
        if (adc <= 5) {
            txt.setText("KEY1");
        } else if (adc <= 490) {
            txt.setText("KEY2");
        } else if (adc <= 653) {
            txt.setText("KEY3");
        } else if (adc <= 734) {
            txt.setText("KEY4");
        } else {
            txt.setText("NONE");
        }
    }

}

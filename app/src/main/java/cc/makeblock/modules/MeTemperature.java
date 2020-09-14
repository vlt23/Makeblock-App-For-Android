package cc.makeblock.modules;

import org.json.JSONObject;

import android.widget.TextView;

import cc.makeblock.makeblock.R;

public class MeTemperature extends MeModule {
    static String devName = "temperature";

    public MeTemperature(int port, int slot) {
        super(devName, MeModule.DEV_TEMPERATURE, port, slot);
        viewLayout = R.layout.dev_value_view;
        imageId = R.drawable.temperature;
        shouldSelectSlot = true;
    }

    public MeTemperature(JSONObject jobj) {
        super(jobj);
        viewLayout = R.layout.dev_value_view;
        imageId = R.drawable.temperature;
        shouldSelectSlot = true;
    }


    public String getScriptRun(String var) {
        varReg = var;
        return var + " = temperature(" + getPortString(port) + "," + getSlotString(slot) + ")\n";
    }

    public byte[] getQuery(int index) {
        return buildQuery(type, port, slot, index);
    }

    public void setEchoValue(String value) {
        TextView txt = view.findViewById(R.id.textValue);
        //double centi = ((double)Integer.parseInt(value)*0.0625);
        txt.setText(value);
    }


}

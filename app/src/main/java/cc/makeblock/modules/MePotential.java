package cc.makeblock.modules;

import android.widget.TextView;

import org.json.JSONObject;

import cc.makeblock.makeblock.R;

public class MePotential extends MeModule {
    static String devName = "potentiometer";

    public MePotential(int port, int slot) {
        super(devName, MeModule.DEV_POTENTIALMETER, port, slot);
        viewLayout = R.layout.dev_value_view;
        imageId = R.drawable.potentiometer;
    }

    public MePotential(JSONObject jObj) {
        super(jObj);
        viewLayout = R.layout.dev_value_view;
        imageId = R.drawable.potentiometer;
    }

    @Override
    public String getScriptRun(String var) {
        varReg = var;
        return var + " = potentiometer(" + getPortString(port) + ")\n";
    }

    @Override
    public byte[] getQuery(int index) {
        return buildQuery(type, port, slot, index);
    }

    @Override
    public void setEchoValue(String value) {
        TextView txt = view.findViewById(R.id.textValue);
        txt.setText(value);
    }

}

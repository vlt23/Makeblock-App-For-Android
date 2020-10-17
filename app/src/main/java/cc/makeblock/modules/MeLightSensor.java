package cc.makeblock.modules;

import org.json.JSONObject;

import android.os.Handler;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import cc.makeblock.makeblock.R;

public class MeLightSensor extends MeModule implements OnCheckedChangeListener {
    static String devName = "lightsensor";
    CheckBox ledCheck;

    public MeLightSensor(int port, int slot) {
        super(devName, MeModule.DEV_LIGHTSENSOR, port, slot);
        viewLayout = R.layout.dev_value_check;
        imageId = R.drawable.lightsensor;
    }

    public MeLightSensor(JSONObject jObj) {
        super(jObj);
        viewLayout = R.layout.dev_value_check;
        imageId = R.drawable.lightsensor;
    }

    @Override
    public String getScriptRun(String var) {
        varReg = var;
        return var + " = lightsensor(" + getPortString(port) + ")\n";
    }

    @Override
    public void setEnable(Handler handler) {
        mHandler = handler;
        ledCheck = view.findViewById(R.id.ledCheck);
        ledCheck.setEnabled(true);
        ledCheck.setOnCheckedChangeListener(this);
        if (ledCheck.isChecked()) {
            byte[] wr = buildWrite(type, port, slot, 1);
            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
        } else {
            byte[] wr = buildWrite(type, port, slot, 0);
            mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
        }
    }

    @Override
    public void setDisable() {
        ledCheck = view.findViewById(R.id.ledCheck);
        ledCheck.setEnabled(false);
    }

    @Override
    public byte[] getQuery(int index) {
        return buildQuery(type, port, slot, index);
    }

    @Override
    public void setEchoValue(String value) {
        TextView txt = view.findViewById(R.id.textValue);
        txt.setText(value + " lux");
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        byte[] wr;
        if (isChecked) {
            wr = buildWrite(type, port, slot, 1);
        } else {
            wr = buildWrite(type, port, slot, 0);
        }
        mHandler.obtainMessage(MSG_VALUE_CHANGED, wr).sendToTarget();
    }

}

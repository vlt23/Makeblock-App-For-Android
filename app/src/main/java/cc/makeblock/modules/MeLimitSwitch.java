package cc.makeblock.modules;

import android.widget.ImageView;

import org.json.JSONObject;

import cc.makeblock.makeblock.R;

public class MeLimitSwitch extends MeModule {
    static String devName = "limitswitch";

    public MeLimitSwitch(int port, int slot) {
        super(devName, MeModule.DEV_LIMITSWITCH, port, slot);
        viewLayout = R.layout.dev_switch_view;
        imageId = R.drawable.limitswitch;
        shouldSelectSlot = true;
    }

    public MeLimitSwitch(JSONObject jObj) {
        super(jObj);
        viewLayout = R.layout.dev_switch_view;
        imageId = R.drawable.limitswitch;
        shouldSelectSlot = true;
    }

    @Override
    public String getScriptRun(String var) {
        varReg = var;
        return var + " = limitswitch(" + getPortString(port) + "," + getSlotString(slot) + ")\n";
    }

    @Override
    public byte[] getQuery(int index) {
        // todo: add limit switch to firmware
        return buildQuery(DEV_LINEFOLLOWER, port, slot, index);
    }

    @Override
    public void setEchoValue(String value) {
        int v = (int) Float.parseFloat(value);
        ImageView img = view.findViewById(R.id.switchImg);
        if (this.slot == SLOT_2) {
            v &= 0x1;
        } else {
            v &= 0x2;
        }
        if (v > 0) {
            img.setImageResource(R.drawable.switch_off);
        } else {
            img.setImageResource(R.drawable.switch_on);
        }
    }

}

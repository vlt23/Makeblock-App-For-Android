package cc.makeblock.makeblock;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class DialogActivity extends Activity {
    public static DialogActivity shared;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dialog);
        TextView mMsgLabel = findViewById(R.id.dialogText);
        String msg = this.getIntent().getStringExtra("msg");
        mMsgLabel.setText(msg);
        if (shared != null) {
            shared.finish();
        }
        shared = this;
        if (msg != null && msg.equals(getString(R.string.connected))) {
            Timer t = new Timer();
            TimerTask task = new TimerTask() {

                @Override
                public void run() {
                    shared.finish();
                    shared = null;
                }
            };
            t.schedule(task, 1000);
        }
    }

}

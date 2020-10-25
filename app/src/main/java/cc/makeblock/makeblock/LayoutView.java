package cc.makeblock.makeblock;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;
import cc.makeblock.modules.MeModule;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class LayoutView extends AppCompatActivity {
    static final String dbg = "LayoutView";
    static final int BOARD_ARDUINO = 0;

    private int screenWidth, screenHeight;

    private int leftEdge;

    private int rightEdge = 0;

    private View content;

    private View menu;

    private LinearLayout.LayoutParams menuParams;

    private boolean isMenuVisible;

    ImageButton addModBtn;
    ImageButton runBtn;
    ListView moduleListView;

    LocalLayout layouts;
    FrameLayout contentView;

    MeLayout layout;
    PopupWindow popupWindow;
    LinearLayout popupLayout;
    RadioGroup portGroup;
    RadioGroup slotGroup;
    RadioButton rPort1, rPort2, rPort3, rPort4, rPort5, rPort6, rPort7, rPort8,
            rPort12;  // missing, check MeModule class
    RadioButton rPortM1, rPortM2,
            rPortM3;  // missing, check MeModule class
    RadioButton rSlot1, rSlot2;

    int boardType = BOARD_ARDUINO;

    // bluetooth related
    MenuItem bltIcon;
    Bluetooth blt;
    PopupWindow popupBtSelect;
    DeviceListAdapter devAdapter;

    // Mscript related
    static final int STAGE_IDLE = 0; // the layout is editable in this state
    static final int STAGE_RUN = 4;
    int engineState = STAGE_IDLE;
    Timer mTimer;
    TimerTask mTimerTask;
    int queryListIndex;
    private final Matrix imageMatrix = new Matrix();

    public LayoutView() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout);
        layouts = new LocalLayout(this);
        contentView = findViewById(R.id.content);
        contentView.getForeground().setAlpha(0);
        initValues();
        addModBtn = this.findViewById(R.id.drawModule);
        addModBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (engineState > STAGE_IDLE) {
                    return;
                }
                if (!isMenuVisible) {
                    new ScrollTask().execute(30);
                } else {
                    new ScrollTask().execute(-30);
                }
            }
        });

        runBtn = this.findViewById(R.id.runLayout);
        runBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (engineState == STAGE_IDLE) {
                    if (blt != null) {
                        if (blt.connDev == null) {
                            showBtSelect();
                        } else {
                            engineState = STAGE_RUN;
                            enableAllModule();
                            runBtn.setImageResource(R.drawable.pause_button);
                        }
                        startTimer(200);
                    } else {
                        if (BluetoothLE.sharedManager().isConnected()) {
                            engineState = STAGE_RUN;
                            enableAllModule();
                            runBtn.setImageResource(R.drawable.pause_button);
                            startTimer(200);
                        } else {
                            showBtSelect();
                        }
                    }
                } else {
                    stopTimer();
                    engineState = STAGE_IDLE;
                    disableAllModule();
                    runBtn.setImageResource(R.drawable.run_button);
                }
            }
        });

        String jsonStr = getIntent().getExtras().getString("layout");
        JSONObject json;

        try {
            json = new JSONObject(jsonStr);
            layout = new MeLayout(json);
            for (MeModule mod : layout.moduleList) {
                addViewModule(mod);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(layout.name);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        moduleListView = this.findViewById(R.id.moduleListView);
        SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.list_modulelist,
                new String[]{"title", "info", "img"},
                new int[]{R.id.title, R.id.info, R.id.img});
        moduleListView.setAdapter(adapter);
        moduleListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> dev = getData().get(position);
                int deviceType = (Integer) dev.get("dev");
                //Log.i(dbg, "add module "+deviceType);
                MeModule mod = layout.addModule(deviceType, deviceType == MeModule.DEV_DCMOTOR ? 9
                        : 3, 0, 10, 10);
                addViewModule(mod);
                saveLayout();
            }
        });

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            blt = Bluetooth.sharedManager();
            blt.mHandler = mHandler;
            devAdapter = new DeviceListAdapter(this, blt.getBtDevList(),
                    R.layout.device_list_item);
        } else {
            BluetoothLE.sharedManager().setup(this);
            BluetoothLE.sharedManager().leHandler = mLeHandler;
            devAdapter = new DeviceListAdapter(this, BluetoothLE.sharedManager().getDeviceList(),
                    R.layout.device_list_item);
            MeTimer.startWrite();
        }

        disableAllModule();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("mb", "start");
    }

    @Override
    protected void onStop() {
        stopTimer();
        super.onStop();
        Log.d("mb", "stop");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("mb", "resume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("mb", "pause");
    }

    void enableAllModule() {
        for (MeModule mod : layout.moduleList) {
            mod.setEnable(mHandler);
            mod.update();
        }
        addModBtn.setVisibility(View.INVISIBLE);
    }

    void disableAllModule() {
        for (MeModule mod : layout.moduleList) {
            mod.setDisable();
            mod.update();
        }
        addModBtn.setVisibility(View.VISIBLE);
    }

    void saveLayout() {
        try {
            layout.updateTime = layout.getTime();
            layouts.FileSave(layout.name + ".json", layout.toString());
            getIntent().removeExtra("layout");
            getIntent().putExtra("layout", layout.toString()); // update intent string
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addViewModule(MeModule mod) {
        View view = LayoutInflater.from(this).inflate(mod.viewLayout, null);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP);

        params.topMargin = mod.xPosition;
        params.leftMargin = mod.yPosition;
        view.setOnTouchListener(new ModuleOnTouch(mod));
        mod.view = view;
        mod.setViewPortString(mod.port);
        mod.setViewPortImage(mod.imageId);
        mod.update();
        contentView.addView(mod.view, params);
        contentView.requestLayout();
    }

    void showPopupWindow(MeModule mod) {
        // don't show port select for joystick
        popupLayout = (LinearLayout) LayoutInflater.from(this).inflate(
                R.layout.popup_menu, null);
        popupWindow = new PopupWindow(this);
        popupWindow.setWidth(screenWidth / 2);
        popupWindow.setHeight((int) (screenHeight * 0.8));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setContentView(popupLayout);
        popupWindow.showAtLocation(findViewById(R.id.content), Gravity.LEFT | Gravity.TOP,
                screenWidth / 4, screenHeight / 10 + 25);
        popupWindow.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                contentView.getForeground().setAlpha(0);
            }
        });

        portGroup = popupLayout.findViewById(R.id.radioGroup1);
        rPort1 = popupLayout.findViewById(R.id.port1);
        rPort2 = popupLayout.findViewById(R.id.port2);
        rPort3 = popupLayout.findViewById(R.id.port3);
        rPort4 = popupLayout.findViewById(R.id.port4);
//        rPort5 = popupLayout.findViewById(R.id.port5);
//        rPort6 = popupLayout.findViewById(R.id.port6);
//        rPort7 = popupLayout.findViewById(R.id.port7);
//        rPort8 = popupLayout.findViewById(R.id.port8);
        rPort12 = popupLayout.findViewById(R.id.port12);
        rPortM1 = popupLayout.findViewById(R.id.portm1);
        rPortM2 = popupLayout.findViewById(R.id.portm2);
        rPortM3 = popupLayout.findViewById(R.id.portm3);
        switch (mod.port) {
            case MeModule.PORT_1:
                rPort1.setChecked(true);
                break;
            case MeModule.PORT_2:
                rPort2.setChecked(true);
                break;
            case MeModule.PORT_3:
                rPort3.setChecked(true);
                break;
            case MeModule.PORT_4:
                rPort4.setChecked(true);
                break;
            case MeModule.PORT_5:
                rPort5.setChecked(true);
                break;
            case MeModule.PORT_6:
                rPort6.setChecked(true);
                break;
            case MeModule.PORT_7:
                rPort7.setChecked(true);
                break;
            case MeModule.PORT_8:
                rPort8.setChecked(true);
                break;
            case MeModule.PORT_12:
                rPort12.setChecked(true);
                break;
            case MeModule.PORT_M1:
                rPortM1.setChecked(true);
                break;
            case MeModule.PORT_M2:
                rPortM2.setChecked(true);
                break;
            case MeModule.PORT_M3:
                rPortM3.setChecked(true);
                break;
        }

        slotGroup = popupLayout.findViewById(R.id.slotRadioGroup);
        rSlot1 = popupLayout.findViewById(R.id.slot1);
        rSlot2 = popupLayout.findViewById(R.id.slot2);
        TextView joystickTip = popupLayout.findViewById(R.id.joystickInfo);

        if (mod.type == MeModule.DEV_JOYSTICK || mod.type == MeModule.DEV_CAR_CONTROLLER) {
            portGroup.setVisibility(View.GONE);
            slotGroup.setVisibility(View.GONE);
            joystickTip.setVisibility(View.VISIBLE);
        } else {
            if (!mod.shouldSelectSlot) {
                slotGroup.setVisibility(View.GONE);
            } else {
                switch (mod.slot) {
                    case MeModule.SLOT_1:
                        rSlot1.setChecked(true);
                        break;
                    case MeModule.SLOT_2:
                        rSlot2.setChecked(true);
                        break;
                }
            }
            joystickTip.setVisibility(View.GONE);
        }
        //portGroup.setOnCheckedChangeListener(new portChangeListener(mod));
        TextView txtTitle = popupLayout.findViewById(R.id.popupTitle);
        txtTitle.setText(getDevString(mod.name));
        Button btnDelete = popupLayout.findViewById(R.id.popupDelBtn);
        btnDelete.setOnClickListener(new moduleDeleteListener(mod));
        Button btnOk = popupLayout.findViewById(R.id.popupOkBtn);
        btnOk.setOnClickListener(new portChangeListener(mod));
        contentView.getForeground().setAlpha(150);
    }

    class moduleDeleteListener implements OnClickListener {
        MeModule mod;

        public moduleDeleteListener(MeModule mod) {
            this.mod = mod;
        }

        @Override
        public void onClick(View v) {
            Log.i(dbg, "delete module " + mod.name);
            contentView.removeView(mod.view);
            layout.moduleList.remove(mod);
            popupWindow.dismiss();
            saveLayout();
        }
    }

    class portChangeListener implements OnClickListener {
        MeModule mod;

        public portChangeListener(MeModule mod) {
            this.mod = mod;
        }

        @Override
        public void onClick(View v) {
            int id = portGroup.getCheckedRadioButtonId();
            switch (id) {
                case R.id.port1:
                    mod.port = MeModule.PORT_1;
                    break;
                case R.id.port2:
                    mod.port = MeModule.PORT_2;
                    break;
                case R.id.port3:
                    mod.port = MeModule.PORT_3;
                    break;
                case R.id.port4:
                    mod.port = MeModule.PORT_4;
                    break;
//                case R.id.port5:
//                    mod.port = MeModule.PORT_5;
//                    break;
//                case R.id.port6:
//                    mod.port = MeModule.PORT_6;
//                    break;
//                case R.id.port7:
//                    mod.port = MeModule.PORT_7;
//                    break;
//                case R.id.port8:
//                    mod.port = MeModule.PORT_8;
//                    break;
                case R.id.port12:
                    mod.port = MeModule.PORT_12;
                    break;
                case R.id.portm1:
                    mod.port = MeModule.PORT_M1;
                    break;
                case R.id.portm2:
                    mod.port = MeModule.PORT_M2;
                    break;
                case R.id.portm3:
                    mod.port = MeModule.PORT_M3;
                    break;
            }
            if (mod.shouldSelectSlot) {
                int slotId = slotGroup.getCheckedRadioButtonId();
                switch (slotId) {
                    case R.id.slot1:
                        mod.slot = MeModule.SLOT_1;
                        break;
                    case R.id.slot2:
                        mod.slot = MeModule.SLOT_2;
                        break;
                }
            }

            mod.setViewPortString(mod.port);
            popupWindow.dismiss();
            saveLayout();
        }
    }

    class ModuleOnTouch implements OnTouchListener {
        private int lastX, lastY;
        long startTime;
        MeModule mod;

        public ModuleOnTouch(MeModule mod) {
            this.mod = mod;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            v.performClick();
            if (engineState > STAGE_IDLE) return false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mod.view.bringToFront();
                    lastX = (int) event.getRawX();
                    lastY = (int) event.getRawY();
                    startTime = System.currentTimeMillis();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) event.getRawX() - lastX;
                    int dy = (int) event.getRawY() - lastY;
                    int left = v.getLeft() + dx;
                    int top = v.getTop() + dy;
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();
                    params.topMargin = top;
                    params.leftMargin = left;
                    v.setLayoutParams(params);
                    mod.xPosition = top;
                    mod.yPosition = left;
                    lastX = (int) event.getRawX();
                    lastY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_UP:
                    long endTime = System.currentTimeMillis();
                    if ((endTime - startTime) < 200) {
                        showPopupWindow(mod);
                    } else {
                        saveLayout();
                    }
                    break;
            }
            return true;
        }
    }

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();

        map.put("title", getString(R.string.dcmotor));
        map.put("info", "");
        map.put("img", R.drawable.motor);
        map.put("dev", MeModule.DEV_DCMOTOR);
        list.add(map);

//		map = new HashMap<String, Object>();
//		map.put("title", getString(R.string.gripper));
//		map.put("info", "");
//		map.put("img", R.drawable.servo);
//		map.put("dev", MeModule.DEV_GRIPPER_CONTROLLER);
//		list.add(map);

        map = new HashMap<>();
        map.put("title", getString(R.string.servomotor));
        map.put("info", "");
        map.put("img", R.drawable.servo);
        map.put("dev", MeModule.DEV_SERVO);
        list.add(map);

        map = new HashMap<>();
        map.put("title", getString(R.string.joystick));
        map.put("info", "");
        map.put("img", R.drawable.joystick_icon);
        map.put("dev", MeModule.DEV_JOYSTICK);
        list.add(map);

        map = new HashMap<>();
        map.put("title", getString(R.string.RGBled));
        map.put("info", "");
        map.put("img", R.drawable.rgbled);
        map.put("dev", MeModule.DEV_RGBLED);
        list.add(map);

        map = new HashMap<>();
        map.put("title", getString(R.string.digitalseg));
        map.put("info", "");
        map.put("img", R.drawable.sevseg);
        map.put("dev", MeModule.DEV_SEVSEG);
        list.add(map);

        map = new HashMap<>();
        map.put("title", getString(R.string.ultrasonic));
        map.put("info", "");
        map.put("img", R.drawable.ultrasonic);
        map.put("dev", MeModule.DEV_ULTRASONIC);
        list.add(map);

        map = new HashMap<>();
        map.put("title", getString(R.string.temperature));
        map.put("info", "");
        map.put("img", R.drawable.temperature);
        map.put("dev", MeModule.DEV_TEMPERATURE);
        list.add(map);

        map = new HashMap<>();
        map.put("title", getString(R.string.lightsensor));
        map.put("info", "");
        map.put("img", R.drawable.lightsensor);
        map.put("dev", MeModule.DEV_LIGHTSENSOR);
        list.add(map);

        map = new HashMap<>();
        map.put("title", getString(R.string.soundsensor));
        map.put("info", "");
        map.put("img", R.drawable.soundsensor);
        map.put("dev", MeModule.DEV_SOUNDSENSOR);
        list.add(map);

        map = new HashMap<>();
        map.put("title", getString(R.string.linefollow));
        map.put("info", "");
        map.put("img", R.drawable.linefinder);
        map.put("dev", MeModule.DEV_LINEFOLLOWER);
        list.add(map);

        map = new HashMap<>();
        map.put("title", getString(R.string.potentialmeter));
        map.put("info", "");
        map.put("img", R.drawable.potentiometer);
        map.put("dev", MeModule.DEV_POTENTIALMETER);
        list.add(map);

        map = new HashMap<>();
        map.put("title", getString(R.string.limitswitch));
        map.put("info", "");
        map.put("img", R.drawable.limitswitch);
        map.put("dev", MeModule.DEV_LIMITSWITCH);
        list.add(map);

        map = new HashMap<>();
        map.put("title", getString(R.string.button));
        map.put("info", "");
        map.put("img", R.drawable.button);
        map.put("dev", MeModule.DEV_BUTTON);
        list.add(map);

        map = new HashMap<>();
        map.put("title", getString(R.string.pirsensor));
        map.put("info", "");
        map.put("img", R.drawable.pirmotion);
        map.put("dev", MeModule.DEV_PIRMOTION);
        list.add(map);

        return list;
    }

    private void initValues() {
        WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        screenWidth = window.getDefaultDisplay().getWidth();
        screenHeight = window.getDefaultDisplay().getHeight();
        content = findViewById(R.id.content);
        menu = findViewById(R.id.menu);
        menuParams = (LinearLayout.LayoutParams) menu.getLayoutParams();
        //menuParams.width = screenWidth - menuPadding;
        menuParams.width = (int) (screenWidth * 0.3);
        leftEdge = -menuParams.width;
        menuParams.leftMargin = leftEdge;
        content.getLayoutParams().width = screenWidth;
    }

    class ScrollTask extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected Integer doInBackground(Integer... speed) {
            int leftMargin = menuParams.leftMargin;
            while (true) {
                leftMargin = leftMargin + speed[0];
                if (leftMargin > rightEdge) {
                    leftMargin = rightEdge;
                    break;
                }
                if (leftMargin < leftEdge) {
                    leftMargin = leftEdge;
                    break;
                }
                publishProgress(leftMargin);
                sleep(20);
            }
            isMenuVisible = speed[0] > 0;

            return leftMargin;
        }

        @Override
        protected void onProgressUpdate(Integer... leftMargin) {
            menuParams.leftMargin = leftMargin[0];
            menu.setLayoutParams(menuParams);
            rotateAddButton(45 + leftMargin[0] * 45 / (rightEdge - leftEdge));
        }

        @Override
        protected void onPostExecute(Integer leftMargin) {
            menuParams.leftMargin = leftMargin;
            menu.setLayoutParams(menuParams);
            rotateAddButton(45 + leftMargin * 45 / (rightEdge - leftEdge));
        }
    }

    private void rotateAddButton(int angle) {
        Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.add_button, null)).getBitmap();
        imageMatrix.setRotate(angle);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), imageMatrix, true);
        addModBtn.setImageBitmap(bitmap);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.layout, menu);
        bltIcon = menu.findItem(R.id.action_bluetooth);
        if (blt == null) {
            return true;
        }
        if (blt.connDev != null) {
            bltIcon.setIcon(R.drawable.bluetooth_on);
            Message msg = mHandler.obtainMessage(Bluetooth.MSG_CONNECTED);
            mHandler.sendMessage(msg);
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (blt != null) {
            if (blt.connDev != null) {
                Bluetooth.sharedManager().bluetoothDisconnect(blt.connDev);
            }
        } else {
            BluetoothLE.sharedManager().close();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_del:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.delete))
                        .setMessage(getString(R.string.delete_this_layout))
                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                layouts.FileDelete(layout.name + ".json");
                                setResult(1);
                                finish();
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
                break;
            case R.id.action_bluetooth:
                showBtSelect();
                break;
            default:
                Intent mIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivityForResult(mIntent, 0);
        }
        return true;
    }

    // --------------- BLUETOOTH BELOW ---------------------
    final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Bluetooth.MSG_CONNECTED: {
                    devListChanged();
                    bltIcon.setIcon(R.drawable.bluetooth_on);
                    Intent intent = new Intent(LayoutView.this, DialogActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("msg", getString(R.string.connected));
                    startActivity(intent);
                    startTimer(1000);
                }
                break;
                case Bluetooth.MSG_DISCONNECTED:
                    stopTimer();
                    devListChanged();
                    bltIcon.setIcon(R.drawable.bluetooth_off);
                    boardType = BOARD_ARDUINO;
                    break;
                case Bluetooth.MSG_CONNECT_FAIL: {
                    Intent intent = new Intent(LayoutView.this, DialogActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("msg", getString(R.string.connectfail));
                    startActivity(intent);
                    Log.d(dbg, "connect fail");
                }
                break;
                case Bluetooth.MSG_RX:
                    int[] rx = (int[]) msg.obj;
                    parseMsg(rx);
                    break;
                case Bluetooth.MSG_FOUND_DEVICE:
                    devListChanged();
                    break;
                case Bluetooth.MSG_DISCOVERY_FINISHED: {
                    if (btnRefresh != null) {
                        AnimationDrawable d = (AnimationDrawable) btnRefresh.getCompoundDrawables()[0];
                        d.stop();
                    }
                }
                break;
                case MeModule.MSG_VALUE_CHANGED:
                    byte[] cmd = (byte[]) msg.obj;
                    if (blt != null) {
                        blt.bluetoothWrite(cmd);
                    } else {
                        BluetoothLE.sharedManager().writeBuffer(cmd);
                    }
                    break;
            }
        }
    };

    final Handler mLeHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothLE.MSG_CONNECTED: {
                    devLEListChanged();
                    bltIcon.setIcon(R.drawable.bluetooth_on);
                    Intent intent = new Intent(LayoutView.this, DialogActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("msg", getString(R.string.connected));
                    startActivity(intent);
                    startTimer(1000);
                }
                break;
                case BluetoothLE.MSG_DISCONNECTED:
                    stopTimer();
                    devLEListChanged();
                    bltIcon.setIcon(R.drawable.bluetooth_off);
                    boardType = BOARD_ARDUINO;
                    break;
                case BluetoothLE.MSG_CONNECT_FAIL: {
                    Intent intent = new Intent(LayoutView.this, DialogActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("msg", getString(R.string.connectfail));
                    startActivity(intent);
                    Log.d(dbg, "connect fail");
                }
                break;
                case BluetoothLE.MSG_SCAN_START: {
                    if (btnRefresh != null) {
                        AnimationDrawable d = (AnimationDrawable) btnRefresh.getCompoundDrawables()[0];
                        d.start();
                    }
                }
                break;
                case BluetoothLE.MSG_SCAN_END:
                case BluetoothLE.MSG_DISCOVERY_FINISHED: {
                    if (btnRefresh != null) {
                        AnimationDrawable d = (AnimationDrawable) btnRefresh.getCompoundDrawables()[0];
                        d.stop();
                    }
                }
                break;
                case BluetoothLE.MSG_RX:
                    int[] rx = (int[]) msg.obj;
                    parseMsg(rx);
                    break;
                case BluetoothLE.MSG_FOUND_DEVICE:
                    devLEListChanged();
                    break;
                case BluetoothLE.MSG_CONNECTING: {
                    Intent intent = new Intent(LayoutView.this, DialogActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("msg", getString(R.string.connecting));
                    startActivity(intent);
                }
                break;
                case MeModule.MSG_VALUE_CHANGED:
                    byte[] cmd = (byte[]) msg.obj;
                    BluetoothLE.sharedManager().writeBuffer(cmd);
                    break;
            }
        }
    };

    void queryVersion() {
        //Log.d("mb", "queryVersion");
        byte[] cmd = new byte[7];
        //a[8]={0xff,0x55,len,VERSION_INDEX,action,device,'\n'};
        cmd[0] = (byte) 0xff;
        cmd[1] = (byte) 0x55;
        cmd[2] = (byte) 3;
        cmd[3] = (byte) MeModule.VERSION_INDEX;
        cmd[4] = (byte) MeModule.READ_MODULE;
        cmd[5] = (byte) 0;
        cmd[6] = (byte) '\n';
        if (blt != null) {
            blt.bluetoothWrite(cmd);
        } else {
            BluetoothLE.sharedManager().writeBuffer(cmd);
        }
    }

    void parseMsg(int[] msg) {
//		Log.d("mb", "parseMSG:"+msg.length);
        if (msg.length > 2) {
            if ((msg[2] & 0xff) == MeModule.VERSION_INDEX) {
                int len = msg[4];
                StringBuilder hexStr = new StringBuilder();
                for (int i = 0; i < len; i++) {
                    hexStr.append(String.format("%c", msg[5 + i]));
                }
                Log.d("mb", "version:" + hexStr);
                if (engineState == STAGE_IDLE) {
                    stopTimer();
                }
            } else {
                int moduleIndex = msg[2];
                if (msg.length < 7) {
                    return;
                }
                float f = 0.0f;
                if (msg[3] == 2) {
                    if (msg.length > 7) {
                        int tint = (msg[4] & 0xff) + ((msg[5] & 0xff) << 8)
                                + ((msg[6] & 0xff) << 16) + ((msg[7] & 0xff) << 24);
                        f = Float.intBitsToFloat(tint);
                    }
                } else if (msg[3] == 1) {
                    f = (msg[4] & 0xff);
                } else if (msg[3] == 3) {
                    f = (msg[4] & 0xff) + ((msg[5] & 0xff) << 8);
                }
                if (moduleIndex < 0 || moduleIndex > layout.moduleList.size()) {
                    return;
                }
                //rx:FF 55 04 04 07 31 2E 31 2E 31 30 32 0D 0A

                if (moduleIndex < layout.moduleList.size()) {
                    MeModule mod = layout.moduleList.get(moduleIndex);
                    mod.setEchoValue("" + f);
                }
            }
        }
    }

    public void devListChanged() {
        if (blt != null) {
            devAdapter.updateData(blt.getBtDevList());
            devAdapter.notifyDataSetChanged();
        }
    }

    public void devLEListChanged() {
        List<String> list = BluetoothLE.sharedManager().getDeviceList();
        if (devAdapter != null) {
            devAdapter.updateData(list);
            devAdapter.notifyDataSetChanged();
        }
    }

    private Button btnRefresh;

    void showBtSelect() {
        if (blt == null) {
            BluetoothLE.sharedManager().start();
        }
        LinearLayout popupBtDevLayout;
        popupBtDevLayout = (LinearLayout) LayoutInflater.from(this).inflate(
                R.layout.popup_btselect, null);
        popupBtSelect = new PopupWindow(this);
        popupBtSelect.setWidth(screenWidth / 2);
        popupBtSelect.setHeight((int) (screenHeight * 0.8));
        popupBtSelect.setOutsideTouchable(true);
        popupBtSelect.setFocusable(true);
        popupBtSelect.setContentView(popupBtDevLayout);
        popupBtSelect.showAtLocation(findViewById(R.id.content), Gravity.LEFT | Gravity.TOP,
                screenWidth / 4, screenHeight / 10 + 25);
        ListView devList = popupBtDevLayout.findViewById(R.id.btdevList);
        devList.setAdapter(devAdapter);

        popupBtSelect.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                contentView.getForeground().setAlpha(0);
            }
        });

        devList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try { // the bluetooth list may vary
                    if (blt != null) {
                        BluetoothDevice dev = blt.btDevices.get(position);
                        if (blt.connDev != null && blt.connDev.equals(dev)) {
                            // disconnect device
                            blt.bluetoothDisconnect(blt.connDev);
                            return;
                        }
                        blt.bluetoothConnect(dev);
                    } else {
                        if (BluetoothLE.sharedManager().isConnected()) {
                            BluetoothLE.sharedManager().close();
                            devLEListChanged();
                        } else {
                            BluetoothLE.sharedManager().selectDevice(position);
                        }
                    }
                } catch (Exception e) {
                    Log.e(dbg, e.toString());
                }
            }
        });

        contentView.getForeground().setAlpha(150);
        btnRefresh = popupBtDevLayout.findViewById(R.id.popupRefreshBtn);
        btnRefresh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (blt != null) {
                    if (!blt.isDiscovery()) {
                        blt.devListClear();
                        devListChanged();
                        Log.i(dbg, "startDiscovery");
                        blt.startDiscovery();
                    }
                } else {
                    BluetoothLE.sharedManager().stop();
                    BluetoothLE.sharedManager().clear();
                    devLEListChanged();
                    BluetoothLE.sharedManager().start();
                }
                AnimationDrawable d = (AnimationDrawable) btnRefresh.getCompoundDrawables()[0];
                d.start();
            }
        });

        Button btnOk = popupBtDevLayout.findViewById(R.id.popupOkBtn);
        btnOk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                popupBtSelect.dismiss();
                if (blt != null) {
                    if (blt.connDev != null) {
                        engineState = STAGE_RUN;
                        stopTimer();
                        startTimer(200);
                        enableAllModule();
                        runBtn.setImageResource(R.drawable.pause_button);
                    }
                } else {
                    if (BluetoothLE.sharedManager().isConnected()) {
                        engineState = STAGE_RUN;
                        stopTimer();
                        startTimer(200);
                        enableAllModule();
                        runBtn.setImageResource(R.drawable.pause_button);
                    }
                }
            }
        });
    }

    byte[] getQueryString() {
        if (queryListIndex >= layout.moduleList.size()) {
            return null;
        }
        MeModule mod = layout.moduleList.get(queryListIndex);
        byte[] query = mod.getQuery(queryListIndex);
        if (query == null) {
            queryListIndex++;
            if (queryListIndex == layout.moduleList.size()) {
                queryListIndex = 0;
                return null;
            }
            return getQueryString();
        }
        queryListIndex++;
        if (queryListIndex == layout.moduleList.size()) {
            queryListIndex = 0;
        }
        return query;
    }


    void startTimer(int interval) {
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    if (engineState == STAGE_RUN) {
                        byte[] queryStr = getQueryString();
                        if (queryStr != null) {
                            if (blt != null) {
                                blt.bluetoothWrite(queryStr);
                            } else {
                                BluetoothLE.sharedManager().writeBuffer(queryStr);
                            }
                        }
                    } else if (engineState == STAGE_IDLE) {
                        queryVersion();
                    }
                }
            };
            mTimer = new Timer(true);
            mTimer.schedule(mTimerTask, 600, interval);
        }
    }

    void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }

    private String getDevString(String str) {
        if (str.equals("servo")) return getString(R.string.servomotor);
        if (str.equals("dcmotor")) return getString(R.string.dcmotor);
        if (str.equals("joystick")) return getString(R.string.joystick);
        if (str.equals("rgbled")) return getString(R.string.RGBled);
        if (str.equals("digiseg")) return getString(R.string.digitalseg);
        if (str.equals("ultrasonic")) return getString(R.string.ultrasonic);
        if (str.equals("temperature")) return getString(R.string.temperature);
        if (str.equals("soundsensor")) return getString(R.string.soundsensor);
        if (str.equals("linefinder")) return getString(R.string.linefollow);
        if (str.equals("potentiometer")) return getString(R.string.potentialmeter);
        if (str.equals("limitswitch")) return getString(R.string.limitswitch);
        if (str.equals("button")) return getString(R.string.button);
        if (str.equals("pirsensor")) return getString(R.string.pirsensor);
        return "";
    }

}

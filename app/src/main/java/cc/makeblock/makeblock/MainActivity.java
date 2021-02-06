package cc.makeblock.makeblock;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    public final static String ITEM_TITLE = "title";
    public final static String ITEM_CAPTION = "caption";
    public final static String ITEM_TITLE_1 = "title1";
    public final static String ITEM_INDEX_1 = "index1";
    public final static String ITEM_IMAGE_1 = "image1";
    public final static String ITEM_TITLE_2 = "title2";
    public final static String ITEM_INDEX_2 = "index2";
    public final static String ITEM_IMAGE_2 = "image2";
    public final static String ITEM_TITLE_3 = "title3";
    public final static String ITEM_INDEX_3 = "index3";
    public final static String ITEM_IMAGE_3 = "image3";
    public static int screenWidth;
    public static int screenHeight;
    private boolean isExit = false;
    private boolean hasTask = false;
    private final Timer tExit = new Timer();
    private TimerTask task;
    private final Intent serviceIntent = new Intent("cc.makeblock.makeblock");
    LocalLayout layouts;

    ListView historyListView;
    List<MeLayout> historyList;
    List<MeLayout> exampleList;
    Map<String, String> localizedStrings = new HashMap<>();
    SeparatedListAdapter adapter;

    private static final int PERMISSION_REQUEST_GPS = 0;

    public Map<String, ?> createSimpleItem(String title, String caption) {
        Map<String, String> item = new HashMap<>();
        item.put(ITEM_TITLE, title);
        item.put(ITEM_CAPTION, caption);
        return item;
    }

    public ComplexItem createComplexItem(String title1, int imageId1, int index1, String title2,
                                         int imageId2, int index2,
                                         String title3, int imageId3, int index3) {
        ComplexItem item = new ComplexItem();
        item.put(ITEM_TITLE_1, title1);
        item.put(ITEM_IMAGE_1, imageId1);
        item.put(ITEM_INDEX_1, index1);
        item.put(ITEM_TITLE_2, title2);
        item.put(ITEM_IMAGE_2, imageId2);
        item.put(ITEM_INDEX_2, index2);
        item.put(ITEM_TITLE_3, title3);
        item.put(ITEM_IMAGE_3, imageId3);
        item.put(ITEM_INDEX_3, index3);
        return item;
    }

    // SectionHeaders
    String[] Sections;

    private void setupViews() {
        // Create and Initialize the ListView Adapter
        initializeAdapter();
        // Get a reference to the ListView holder
        historyListView = this.findViewById(R.id.centerList);
        historyListView.setOnItemClickListener((parent, view, position, id) -> {
            try {
                Log.i("listView", "item " + position + " clicked");
                MeLayout layout = historyList.get(position - 1);
                pushToLayout(layout);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        // Set the adapter on the ListView holder
        historyListView.setAdapter(adapter);
    }

    private void initializeAdapter() {
        adapter = new SeparatedListAdapter(this);

        List<Map<String, ?>> history = new LinkedList<>();
        for (MeLayout layout : historyList) {
            history.add(createSimpleItem(layout.name, layout.updateTime));
        }

        SimpleAdapter listHistoryAdapter = new SimpleAdapter(this, history,
                R.layout.list_simple,
                new String[]{ITEM_TITLE, ITEM_CAPTION},
                new int[]{R.id.list_simple_title, R.id.list_simple_caption});
        adapter.addSection(Sections[0], listHistoryAdapter);

        List<ComplexItem> demos = new LinkedList<>();
        demos.add(createComplexItem(getString(R.string.distancemeasure), R.drawable.distanceicon, 1,
                getString(R.string.temperaturemeasure), R.drawable.temperatureicon, 2,
                getString(R.string.rgbcontrol), R.drawable.rgbicon, 3));
        demos.add(createComplexItem(getString(R.string.robottank), R.drawable.car_controller, 4,
                getString(R.string.roboticarmtank), R.drawable.robotarmcar, 5,
                getString(R.string.balllauncher), R.drawable.balllauncher, 6));
        demos.add(createComplexItem(getString(R.string.drinkcar), R.drawable.beercar, 7,
                "", -1, -1, "", -1, -1));

        ComplexListAdapter listExamplesAdapter = new ComplexListAdapter(this, demos);
        listExamplesAdapter.delegate = this;
        adapter.addSection(Sections[1], listExamplesAdapter);
    }

    public void openExample(int indexId) {
        MeLayout layout = exampleList.get(indexId - 1);
        pushToLayout(layout);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Sections = new String[]{getString(R.string.history), getString(R.string.examples)};

        adapter = new SeparatedListAdapter(this);
        layouts = new LocalLayout(this);

        serviceIntent.setPackage(this.getPackageName());
        startService(serviceIntent);
        task = new TimerTask() {
            @Override
            public void run() {
                isExit = false;
                hasTask = true;
            }
        };

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        MeDevice.sharedManager().setWidth(screenWidth);
        MeDevice.sharedManager().setHeight(screenHeight);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "BLE is supported", Toast.LENGTH_SHORT).show();
        }
        requestGPSPermission();  // Android 9+ needs GPS permission to scan Bluetooth
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        readLocalLayout();
        setupViews();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_GPS && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            // User has rejected the GPS permission
            Toast.makeText(getApplicationContext(), R.string.gps_permission_denied, Toast.LENGTH_LONG).show();
        }
    }

    private void requestGPSPermission() {
        // Android 9+ needs GPS permission to scan Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_GPS);
        }
    }

    void readLocalLayout() {
        String[] fileList = layouts.fileList();
        historyList = new ArrayList<>();
        for (String filename : fileList) {
            if (!filename.contains(".json")) {
                continue;
            }

            String jsonStr;
            try {
                jsonStr = layouts.fileRead(filename);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            JSONObject json = layouts.toJson(jsonStr);
            MeLayout layout = new MeLayout(json);
            historyList.add(layout);
        }
        exampleList = new ArrayList<>();

        localizedStrings.put("Distance Measure", Integer.toString(R.string.distancemeasure));
        localizedStrings.put("Temperature Measure", Integer.toString(R.string.temperaturemeasure));
        localizedStrings.put("RGB Control", Integer.toString(R.string.rgbcontrol));
        localizedStrings.put("General Controls", Integer.toString(R.string.generalcontrols));
        localizedStrings.put("Robot Tank", Integer.toString(R.string.robottank));
        localizedStrings.put("Robotic Arm Tank", Integer.toString(R.string.roboticarmtank));
        localizedStrings.put("Ball Launcher", Integer.toString(R.string.balllauncher));
        localizedStrings.put("Drink Car", Integer.toString(R.string.drinkcar));

        for (int i = 1; i < 8; i++) {
            String jsonStr;
            try {
                jsonStr = readTextFile(getAssets().open("" + i + ".json"));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            JSONObject json = layouts.toJson(jsonStr);
            MeLayout layout = new MeLayout(json);
            try {
                layout.setName(getString(Integer.parseInt(Objects.requireNonNull(
                        localizedStrings.get(json.getString("name"))))));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            exampleList.add(layout);
        }
    }

    private String readTextFile(InputStream inputStream) {
        OutputStream outputStream = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_new:
                final EditText editText = new EditText(this);
                new AlertDialog.Builder(this).setTitle(getString(R.string.input_layout_name))
                        .setView(editText).setPositiveButton(getString(R.string.ok),
                        (dialog, which) -> {
                            String name = editText.getText().toString();
                            MeLayout newLayout = new MeLayout(name);
                            historyList.add(0, newLayout);
                            setupViews();
                            try {
                                layouts.fileSave(newLayout.name + ".json", newLayout.toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            pushToLayout(newLayout);
                        }).setNegativeButton(getString(R.string.cancel), null).show();
                break;
            case R.id.action_about:
                openAbout(this);
                break;
            default:
                break;
        }

        return true;
    }

    private static void openAbout(final Context context) {
        final Intent intent = new Intent(context, AboutActivity.class);
        context.startActivity(intent);
    }

    void pushToLayout(MeLayout layout) {
        final Intent intent = new Intent(this, LayoutView.class);
        intent.putExtra("layout", layout.toString()); // use json string between activities
        startActivity(intent);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {
            if (!isExit) {
                isExit = true;
                Toast.makeText(getApplicationContext(), getString(R.string.pressbackagain),
                        Toast.LENGTH_SHORT).show();
                if (!hasTask) {
                    tExit.schedule(task, 2000);
                }
            } else {
                BluetoothAdapter.getDefaultAdapter().disable();
                stopService(serviceIntent);
                finish();
                System.exit(0);
            }
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

}

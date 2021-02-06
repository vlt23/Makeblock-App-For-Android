package cc.makeblock.makeblock;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class LocalLayout {
    private static final String dbg = "locallayout";
    private final Context context;

    public LocalLayout(Context context) {
        this.context = context;
    }

    public void fileSave(String filename, String content) throws IOException {
        FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
        fos.write(content.getBytes());
        fos.close();
    }

    public String fileRead(String filename) throws IOException {
        FileInputStream fin = context.openFileInput(filename);
        byte[] b = new byte[fin.available()];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        while ((fin.read(b)) != -1) {
            buffer.write(b);
        }
        byte[] data;
        data = buffer.toByteArray();

        buffer.close();
        fin.close();
        return new String(data);
    }

    public void fileDelete(String file) {
        String[] files = fileList();
        Log.i(dbg, "file list" + Arrays.toString(files));
        context.deleteFile(file);
        files = fileList();
        Log.i(dbg, "file list" + Arrays.toString(files));
    }

    public String[] fileList() {
        return context.fileList();
    }

    public JSONObject toJson(String jsonStr) {
        try {
            return new JSONObject(jsonStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String toJsonString(JSONObject json) {
        return json.toString();
    }

}

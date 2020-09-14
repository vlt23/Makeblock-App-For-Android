package cc.makeblock.makeblock;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class LocalLayout {
    static final String dbg = "locallayout";
    Context context;


    public LocalLayout(Context context) {
        this.context = context;
    }

    public void FileSave(String filename, String content) throws IOException {
        FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
        fos.write(content.getBytes());
        fos.close();
    }

    public String FileRead(String filename) throws IOException {
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

    public void FileDelete(String file) {
        String[] files = fileList();
        Log.i(dbg, "file list" + Arrays.toString(files));
        context.deleteFile(file);
        files = fileList();
        Log.i(dbg, "file list" + Arrays.toString(files));
    }

    public void initLocalLayout() {
        String[] allfile = fileList();
        for (String file : allfile) {
            context.deleteFile(file);
        }
		/*
		MeLayout helloworld = new MeLayout("helloworld");
		helloworld.addModule(MeModule.DEV_ULTRASOINIC, MeModule.PORT_3, MeModule.SLOT_1, 100, 100);
		try {
			FileSave(helloworld.name+".json",helloworld.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
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

package cc.makeblock.makeblock;

public class MScript {
    public int numOfCode;
    public int numOfConst;

    public MScript() {
        numOfCode = 0;
        numOfConst = 0;
    }


    public String compile(String code) {
        String n = compileJNI(code);
        String[] tmp = n.split(" ");
        numOfCode = Integer.parseInt(tmp[1].trim());
        numOfConst = Integer.parseInt(tmp[2].trim());
        return n;
    }

    public String getCode(int index) {
        return getCodeJNI(index);
    }

    public String getConst(int index) {
        return getConstJNI(index);
    }

    public String getIrq(String code) {
        return getIrqJNI(code);
    }


    // Mscript native
    public native String stringFromJNI();

    public native String compileJNI(String code);

    public native String getCodeJNI(int index);

    public native String getConstJNI(int index);

    public native String getIrqJNI(String code);

    public native String getRegName(int index);

    public native int getRegIndex(String reg);

    static {
        System.loadLibrary("hello-jni");
    }
}

package gabrielleopoldino.tools;

import java.io.IOException;
import java.io.OutputStream;
public class SSLEngineOutputStream extends OutputStream {

    private SSLEngineHelper sslEngineHelper;

    public SSLEngineOutputStream(SSLEngineHelper sslEngineHelper) {
        this.sslEngineHelper = sslEngineHelper;
    }

    public void write(int b) {
        this.sslEngineHelper.outAppData.put((byte) b);
    }

    public void write(byte[] bytes)
    {
        write(bytes, 0, bytes.length);
    }

    public void write(byte[] bytes, int off, int len){
        System.out.println("writing "+len+ " bytes");
        this.sslEngineHelper.outAppData.put(bytes, off, len);
    }

    @Override
    public void flush() {
        //super.flush();
        System.out.println("flush");
        sslEngineHelper.sendAppData();
    }
}

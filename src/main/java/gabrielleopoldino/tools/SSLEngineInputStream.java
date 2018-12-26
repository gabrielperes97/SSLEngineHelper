package gabrielleopoldino.tools;

import java.io.InputStream;

public class SSLEngineInputStream extends InputStream {

    SSLEngineHelper sslEngineHelper;

    public SSLEngineInputStream(SSLEngineHelper sslEngineHelper) {
        this.sslEngineHelper = sslEngineHelper;
    }

    public int read() {
        if (!sslEngineHelper.inAppData.hasRemaining()) {
            return -1;
        }
        return sslEngineHelper.inAppData.get() & 0xFF;
    }

    public synchronized int read(byte[] b)
    {
        return read(b, 0, b.length);
    }

    public int read(byte[] bytes, int off, int len) {
        sslEngineHelper.inAppData.flip();
        if (!sslEngineHelper.inAppData.hasRemaining()) {
            return -1;
        }
        len = Math.min(len, sslEngineHelper.inAppData.remaining());
        sslEngineHelper.inAppData.get(bytes, off, len);

        System.out.println("Reading "+ len);
        return len;
    }

    public synchronized int available() {
        return sslEngineHelper.inAppData.remaining();
    }

    public boolean markSupported() {
        return false;
    }
}

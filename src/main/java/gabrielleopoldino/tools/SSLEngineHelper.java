package gabrielleopoldino.tools;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.*;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

public class SSLEngineHelper {

    private SSLEngine sslEngine;
    private SSLEngineInputStream applicationInputStream;
    private SSLEngineOutputStream applicationOutputStream;

    private DataInputStream packetInputStream;
    private DataOutputStream packetOutputStream;
    private AlternativeIO alternativeIO;

    private ByteBuffer inNetData = ByteBuffer.allocate(65535);
    private ByteBuffer outNetData = ByteBuffer.allocate(65535);

    protected ByteBuffer inAppData = ByteBuffer.allocate(65535);
    protected ByteBuffer outAppData = ByteBuffer.allocate(65535);

    private HandshakeHandler handshakeHandler;
    private Lock handshakeLock = new ReentrantLock();
    private Condition hasHandshake = handshakeLock.newCondition();
    private ReceiveData receiveData;

    private boolean closed = true;

    private List<SSLEngineHelperListener> listeners = new LinkedList<>();

    public SSLEngineHelper(SSLContext sslContext, InputStream inputStream, OutputStream outputStream, boolean serverMode) {
        this(sslContext.createSSLEngine(), inputStream, outputStream);
        this.sslEngine.setUseClientMode(!serverMode);
    }

    public SSLEngineHelper(SSLContext sslContext, AlternativeIO alternativeIO, boolean serverMode)
    {
        this(sslContext.createSSLEngine(), alternativeIO);
        this.sslEngine.setUseClientMode(!serverMode);
    }

    public SSLEngineHelper(SSLEngine sslEngine, AlternativeIO alternativeIO)
    {
        this.sslEngine = sslEngine;
        this.alternativeIO = alternativeIO;
    }

    public SSLEngineHelper(SSLEngine sslEngine, InputStream inputStream, OutputStream outputStream) {
        this.sslEngine = sslEngine;
        packetOutputStream = new DataOutputStream(new BufferedOutputStream(outputStream));
        packetInputStream = new DataInputStream(inputStream);
        //bufferedInputStream = new BufferedInputStream(packetInputStream);
    }

    public void connect() throws SSLException {
        try {
            this.handshakeHandler = new HandshakeHandler();
            this.receiveData = new ReceiveData();
            this.sslEngine.beginHandshake();
            if (!handshakeHandler.isAlive())
                handshakeHandler.start();
            handshakeLock.lock();
            hasHandshake.await();
            applicationOutputStream = new SSLEngineOutputStream(this);
            applicationInputStream = new SSLEngineInputStream(this);
            closed = false;
            handshakeLock.unlock();
            receiveData.start();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public InputStream getInputStream() throws IOException {
        if (applicationInputStream == null)
        {
            throw new IOException("Handshake not completed");
        }
        else
            return applicationInputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        if (applicationOutputStream == null)
        {
            throw new IOException("Handshake not completed");
        }
        else
            return applicationOutputStream;
    }

    public boolean isClosed()
    {
        return closed;
    }

    public void addListener(SSLEngineHelperListener listener)
    {
        if (listener == null) { throw new NullPointerException("listener"); }

        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
        listeners.add(listener);
    }

    public void removeListener(SSLEngineHelperListener listener)
    {
        if (listener == null) { throw new NullPointerException("stateListener"); }

        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    protected synchronized void sendAppData()
    {
        SSLEngineResult.HandshakeStatus hs = sslEngine.getHandshakeStatus();
        if (hs != FINISHED && hs != NOT_HANDSHAKING)
        {
            if (!handshakeHandler.isAlive())
                handshakeHandler.start();
            try {
                handshakeLock.lock();
                hasHandshake.await();
                handshakeLock.unlock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        outAppData.flip();

        while (outAppData.hasRemaining()) {
            SSLEngineResult res = null;
            while (res == null || res.getStatus() != SSLEngineResult.Status.OK) {
                try {
                    res = sslEngine.wrap(outAppData, outNetData);
                    switch (res.getStatus()) {
                        case OK:
                            outNetData.flip();
                            //System.out.println("Sending Segment: "+segment);
                            //System.out.println("Sending: "+ Utils.toHex(outNetData.array(),0, outNetData.remaining()));
                            sendPacketData();
                            //outputLock.unlock();
                        case BUFFER_OVERFLOW:
                            int appSize = sslEngine.getSession().getApplicationBufferSize();
                            if (appSize > outAppData.capacity()) {
                                ByteBuffer buffer = ByteBuffer.allocate(appSize);
                                outAppData.flip();
                                buffer.put(outAppData);
                                outAppData = buffer;
                            }


                            int netSize = sslEngine.getSession().getPacketBufferSize();
                            if (netSize > outNetData.capacity()) {
                                //enlarge the peer network packet buffer
                                ByteBuffer buffer = ByteBuffer.allocate(netSize);
                                outNetData.flip();
                                buffer.put(outNetData);
                                outNetData = buffer;
                            }

                            break;
                        default:
                            //outputLock.unlock();
                    }
                } catch (SSLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private byte[] inputBuffer = new byte[65535];

    private synchronized void receivePacketData()
    {
        if (alternativeIO != null)
        {
            inNetData.put(alternativeIO.receive());
        }
        else
        {
            try {
                //int totalReaded = packetInputStream.read(inNetData.array(), inNetData.limit(), inNetData.capacity());
                //byte[] bytesReaded = packetInputStream.readAllBytes();
                 if (packetInputStream.available() > 0) {
                     int bytesReaded = packetInputStream.read(inputBuffer);
                     System.out.println("Received " + bytesReaded + " bytes");
                     if (bytesReaded > 0) {
                         inNetData.put(inputBuffer, 0, bytesReaded);
                     }
                 }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void sendPacketData()
    {

        if (alternativeIO != null)
        {
            alternativeIO.send(outNetData.array(), outNetData.arrayOffset(), outNetData.limit());
            outNetData.clear();
        }
        else
        {
            try {
                //outNetData.flip();
                packetOutputStream.write(outNetData.array(), outNetData.arrayOffset(), outNetData.limit());
                packetOutputStream.flush();
                System.out.println("Sending "+ outNetData.limit()+" bytes");
                outNetData.clear();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class HandshakeHandler extends Thread {

        private boolean firstHandshake = true;

        @Override
        public void run() {
            handshakeLock.lock();
            SSLEngineResult.HandshakeStatus hs = sslEngine.getHandshakeStatus();

            try {
                while (hs != FINISHED && hs != NOT_HANDSHAKING) {
                    System.out.println(hs);
                    switch (hs) {
                        case NEED_UNWRAP:
                            receivePacketData();
                            inNetData.flip();
                        case NEED_UNWRAP_AGAIN:
                            SSLEngineResult res = sslEngine.unwrap(inNetData, inAppData);
                            inNetData.compact();

                            switch (res.getStatus()) {
                                case OK:
                                    System.out.println("consumed "+res.bytesConsumed());
                                    break;
                                case CLOSED:
                                    //UdpCommon.whenSSLClosed();
                                    break;
                                case BUFFER_OVERFLOW:
                                    int appSize = sslEngine.getSession().getApplicationBufferSize();
                                    if (appSize > inAppData.capacity()) {
                                        ByteBuffer buffer = ByteBuffer.allocate(appSize);
                                        buffer.put(inAppData);
                                        inAppData = buffer;
                                    }
                                    break;
                                case BUFFER_UNDERFLOW:
                                    break;
                            }
                            break;
                        case NEED_WRAP:
                            //outNetData.clear();
                            res = sslEngine.wrap(outAppData, outNetData);
                            switch (res.getStatus()) {
                                case OK:
                                    outNetData.flip();
                                    sendPacketData();
                                    break;
                                case CLOSED:
                                    break;
                                case BUFFER_OVERFLOW:
                                    int appSize = sslEngine.getSession().getApplicationBufferSize();
                                    if (appSize > outAppData.capacity()) {
                                        ByteBuffer b = ByteBuffer.allocate(appSize);
                                        b.put(outAppData);
                                        outAppData = b;
                                    }


                                    int netSize = sslEngine.getSession().getPacketBufferSize();
                                    if (netSize > outNetData.capacity()) {
                                        ByteBuffer b = ByteBuffer.allocate(netSize);
                                        b.put(outNetData);
                                        outNetData = b;
                                    }
                                    break;
                                case BUFFER_UNDERFLOW:
                                    //UdpCommon.whenBufferUnderflow(sslEngine, outNetData);
                                    break;
                            }
                            break;
                        case NEED_TASK:
                            Runnable task;
                            while ((task = sslEngine.getDelegatedTask()) != null) {
                                //new Thread(task).start();
                                task.run();
                            }
                            break;
                        default:
                            break;
                    }
                    hs = sslEngine.getHandshakeStatus();
                }
                if (firstHandshake)
                {
                    firstHandshake = false;
                    inAppData.clear();
                    inAppData.limit(0);
                    outAppData.clear();
                    synchronized (listeners)
                    {
                        for (SSLEngineHelperListener listener : listeners)
                        {
                            listener.onFirstHandshake();
                        }
                    }
                }
            }
            catch (SSLException e) {
                //TODO add um listener para quando ocorrer erro de conexão DTLS
                System.err.println("Connection error");
                e.printStackTrace();
            } catch (BufferOverflowException e) {
                e.printStackTrace();
            } finally {
                hasHandshake.signalAll();
                handshakeLock.unlock();
            }
        }
    }

    protected class ReceiveData extends Thread
    {

        public ReceiveData() {
            super("Receiver Data Thread");
        }

        @Override
        public void run() {
            while(!isClosed()) {
                SSLEngineResult.HandshakeStatus hs = sslEngine.getHandshakeStatus();
                if (hs != FINISHED && hs != NOT_HANDSHAKING && !isClosed()) {
                    if (!handshakeHandler.isAlive())
                        handshakeHandler.start();
                    try {
                        handshakeLock.lock();
                        hasHandshake.await();
                        handshakeLock.unlock();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                int bytesProduced = 0;

                try {
                    //inNetData.clear();
                    receivePacketData();
                    inNetData.flip();
                    //inAppData.clear();

                    SSLEngineResult res = null;
                    while (res == null || res.getStatus() != SSLEngineResult.Status.OK || inNetData.hasRemaining()) {
                        res = sslEngine.unwrap(inNetData, inAppData);
                        bytesProduced += res.bytesProduced();
                        switch (res.getStatus()) {
                            case BUFFER_OVERFLOW:
                                int appSize = sslEngine.getSession().getApplicationBufferSize();
                                if (appSize > inAppData.capacity()) {
                                    ByteBuffer buffer = ByteBuffer.allocate(appSize);
                                    buffer.put(inAppData);
                                    inAppData = buffer;
                                }
                                break;
                            case BUFFER_UNDERFLOW:
                                inNetData.compact();
                                receivePacketData();
                                inNetData.flip();
                                break;
                            case CLOSED:
                                return;
                            case OK:
                                //lenFinalArray += res.bytesProduced();
                                break;
                        }
                    }

                    if (bytesProduced <= 0)
                        continue;

                    //Segment segment = Segment.parse(inAppData.array(), inAppData.arrayOffset(), bytesProduced);
                    //System.out.println("Received: "+segment.toString());
                    //scheduleReceive(segment);
                } catch (SSLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

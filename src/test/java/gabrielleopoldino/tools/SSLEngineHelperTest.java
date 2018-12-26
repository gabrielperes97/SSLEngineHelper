package gabrielleopoldino.tools;

import org.junit.Test;

import javax.net.ssl.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;

import static org.junit.Assert.*;

public class SSLEngineHelperTest {

    @Test
    public void shouldHandshake()
    {
        try {
            ServerSocket serverSocket = new ServerSocket(1234);
            Socket client = new Socket();
            client.connect(new InetSocketAddress(1234));
            Socket server = serverSocket.accept();

            assertTrue(server != null);

            SSLContext context = getSSLContext();

            SSLEngineHelper sslClient = new SSLEngineHelper(context, client.getInputStream(), client.getOutputStream(), false);
            SSLEngineHelper sslServer = new SSLEngineHelper(context, server.getInputStream(), server.getOutputStream(), true);

            Runnable clientConnect = () -> {
                try {
                    sslServer.connect();
                    System.out.println("Client connected");
                } catch (SSLException e) {
                    e.printStackTrace();
                }
            };

            SSLEngineHelperListener listener = new SSLEngineHelperListener() {
                @Override
                public void onFirstHandshake() {
                    System.out.println("Handshake");
                }

                @Override
                public void onClose() {

                }
            };
            sslClient.addListener(listener);
            sslServer.addListener(listener);

            new Thread(clientConnect).start();
            sslClient.connect();
            System.out.println("Client connect");


            Runnable serverPing = () -> {
                try {
                    PrintStream out = new PrintStream(sslServer.getOutputStream());
                    BufferedReader in = new BufferedReader(new InputStreamReader(sslServer.getInputStream()));
                    while (!server.isClosed()) {
                        String message = in.readLine();
                        if (message == null)
                            continue;
                        System.out.println("Echoing" + message);
                        out.println(message);
                        out.flush();

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            new Thread(serverPing).start();

            PrintStream outClient = new PrintStream(sslClient.getOutputStream());
            BufferedReader inClient = new BufferedReader(new InputStreamReader(sslClient.getInputStream()));
            String msg = "OLAR";
            outClient.println(msg);
            outClient.flush();

            String rcvMsg = inClient.readLine();
            System.out.println(rcvMsg);
            assertTrue(msg.equals(rcvMsg));

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }

    private SSLContext getSSLContext() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream("foobar"), "foobar".toCharArray());

        SSLContext context = SSLContext.getInstance("TLSv1.2");
        TrustManagerFactory trustFact = TrustManagerFactory.getInstance("SunX509");
        trustFact.init(keyStore);
        KeyManagerFactory keyFact = KeyManagerFactory.getInstance("SunX509");
        keyFact.init(keyStore, "foobar".toCharArray());

        context.init(keyFact.getKeyManagers(), trustFact.getTrustManagers(), new SecureRandom());
        return context;
    }
}

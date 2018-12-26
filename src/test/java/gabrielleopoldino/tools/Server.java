package gabrielleopoldino.tools;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;

public class Server {

    public static void main (String args[])
    {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream("foobar"), "foobar".toCharArray());

            SSLContext context = SSLContext.getInstance("TLSv1.2");
            TrustManagerFactory trustFact = TrustManagerFactory.getInstance("SunX509");
            trustFact.init(keyStore);
            KeyManagerFactory keyFact = KeyManagerFactory.getInstance("SunX509");
            keyFact.init(keyStore, "foobar".toCharArray());

            context.init(keyFact.getKeyManagers(), trustFact.getTrustManagers(), new SecureRandom());


            ServerSocket serverSocket = new ServerSocket(1234);
            Socket client = serverSocket.accept();
            System.out.println("TCP connected");

            SSLEngineHelper sslClient = new SSLEngineHelper(context, client.getInputStream(), client.getOutputStream(), true);

            sslClient.connect();
            System.out.println("Server connect");

            PrintStream out = new PrintStream(sslClient.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(sslClient.getInputStream()));

            //while (!socket.isClosed()) {
            while (true){
                String message = in.readLine();
                if (message == null)
                    continue;
                System.out.println(message);
                out.println(message);
                out.flush();

            }


        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

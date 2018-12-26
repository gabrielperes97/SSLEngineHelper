package gabrielleopoldino.tools;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Scanner;

public class Client {
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


            Socket client = new Socket();
            client.connect(new InetSocketAddress(1234));
            System.out.println("TCP connected");

            SSLEngineHelper sslClient = new SSLEngineHelper(context, client.getInputStream(), client.getOutputStream(), false);

            sslClient.connect();
            System.out.println("Client connect");

            Scanner s = new Scanner(System.in);
            PrintStream out = new PrintStream(sslClient.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(sslClient.getInputStream()));
            while (true) {
                String msg = s.nextLine();
                if (msg.equals(""))
                    break;
                out.println(msg);
                out.flush();
           /* if (msg.length() == 0)
                break;*/
                System.out.println(in.readLine());
            }
            System.out.println("Closing");
            //sslClient.close();
            System.exit(0);

        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

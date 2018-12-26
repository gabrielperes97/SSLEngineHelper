package gabrielleopoldino.tools;

import javafx.util.converter.ByteStringConverter;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class StreamTestClient {

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", 1234));

        System.out.println("TCP connected");

        //OutputStream out = socket.getOutputStream();
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        //PrintStream out = new PrintStream(socket.getOutputStream());
        Scanner s = new Scanner(System.in);
        String str;
        do {
            str = s.nextLine();
            //out.writeInt(str.length());
            out.write(str.getBytes());
            out.flush();
            System.out.println("Sended: "+str);
        }while (str.length() > 0);
    }
}

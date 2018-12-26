package gabrielleopoldino.tools;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class StreamTestServer {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        Socket socket = serverSocket.accept();
        System.out.println("TCP Accepted");

        /*InputStream in = socket.getInputStream();

       while (!socket.isClosed())
        {
            byte[] bytesReaded = in.readAllBytes();
            //if (bytesReaded.length != 0) {
                System.out.println("Received " + bytesReaded.length + " bytes: "+ Arrays.toString(bytesReaded));
           // }
        }*/

        DataInputStream in = new DataInputStream(socket.getInputStream());
        byte[] message = new byte[65535];
        while (true) {
            int length = in.read(message);
            if (length > 0)
            {
                System.out.println(new String(message, 0, length));
            }
            /*int length = in.readInt();
            if (length > 0) {
                byte[] message = new byte[length];
                in.readFully(message);
                System.out.println(Arrays.toString(message));
            }*/

        }

    }
}

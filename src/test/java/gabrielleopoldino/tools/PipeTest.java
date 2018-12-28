package gabrielleopoldino.tools;

import java.io.*;
import java.util.Scanner;

public class PipeTest {

    public static void main(String[] args) throws IOException {

        final PipedOutputStream serverOutput = new PipedOutputStream();
        final PipedInputStream clientInput  = new PipedInputStream(serverOutput);

        final PipedOutputStream clientOutput = new PipedOutputStream();
        final PipedInputStream serverInput  = new PipedInputStream(clientOutput);

        //Ping Server
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                PrintStream out = new PrintStream(serverOutput);
                BufferedReader in = new BufferedReader(new InputStreamReader(serverInput));

                //while (!socket.isClosed()) {
                while (true){
                    String message = null;
                    try {
                        message = in.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (message == null)
                        continue;
                    System.out.println(message);
                    out.println(message);
                    out.flush();

                }
            }
        });

        //Client Ping
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner s = new Scanner(System.in);
                PrintStream out = new PrintStream(clientOutput);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientInput));
                while (true) {
                    String msg = s.nextLine();
                    if (msg.equals(""))
                        break;
                    out.println(msg);
                    out.flush();
                    try {
                        System.out.println("lido: " + in.readLine());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread1.start();
        thread2.start();

    }
}

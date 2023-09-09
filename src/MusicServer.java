import java.io.*;
import java.net.*;
import java.util.*;

public class MusicServer {
    ArrayList<ObjectOutputStream> clientOutputStreams;
    public static void main(String[] args){
        new MusicServer().go();
    }
    public class ClientHandler implements Runnable{
        ObjectInputStream in;
        Socket clientSocket;

        public ClientHandler(Socket clienSocket){
            try {
                clientSocket = clienSocket;
                in = new ObjectInputStream(clienSocket.getInputStream());
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }

        @Override
        public void run(){
            Object o1;
            Object o2;
            try {
                while ((o1 = in.readObject()) != null){
                    o2 = in.readObject();
                    System.out.println("read two objects");
                    tellEveryOne(o1, o2);
                }
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    public void go(){
        clientOutputStreams = new ArrayList<>();
        try {
            ServerSocket serverSocket = new ServerSocket(4242);
            while (true){
                Socket clientSocket = serverSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                clientOutputStreams.add(out);
                Thread thread = new Thread(new ClientHandler(clientSocket));
                thread.start();
                System.out.println("got a connection");
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void tellEveryOne(Object one, Object two){
        Iterator it = clientOutputStreams.iterator();
        while (it.hasNext()){
            try {
                ObjectOutputStream out = (ObjectOutputStream) it.next();
                out.writeObject(one);
                out.writeObject(two);
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }
}

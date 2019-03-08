package com.company;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class ReactiveChild {

    private static final AtomicInteger ports = new AtomicInteger(30_000);

    private static int servicePort;

    private static int count;

    public static void main(String... args){

        for(String arg: args){
            if(arg.contains("servicePort")){
                servicePort = Integer.valueOf(arg.split(":")[1].trim());
            }
            if(arg.contains("startPort")){
                ports.set(Integer.valueOf(arg.split(":")[1].trim()));
            }
            if(arg.contains("count")){
                count = Integer.valueOf(arg.split(":")[1].trim());
            }
        }

        for(int i = 0; i < count; i++){
            Runnable runnable = () -> {
                Socket s;
                int port = ports.getAndIncrement();
                try{
                    ServerSocket serverSocket = new ServerSocket(port);
                    while(true){
                        s = serverSocket.accept();
                        System.out.println("New connection: " + port);
                        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                        //prev event version for current connection
                        String prevValue = null;
                        while(true){
                            Thread.sleep(100);
                            Socket clientSocket =  new Socket("localhost", servicePort);
                            BufferedReader read = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            //read new event
                            String readLine = read.readLine();
                            //event could be "null" if event queue in the main thread is empty
                            if(!readLine.equals("null")) {
                                if(!readLine.equals(prevValue)){
                                    out.println(readLine);
                                    boolean closed = out.checkError();
                                    System.out.println(readLine + " " + out.checkError());
                                    if(closed){
                                        System.out.println("Connection to port " + port + " closed by client");
                                        break;
                                    }
                                }
                                prevValue = readLine;
                            }
                            read.close();
                            clientSocket.close();
                        }
                    }
                }catch (Exception e){
                    System.out.println("Port " + port);
                    e.printStackTrace();
                }
            };
            new Thread(runnable).start();
        }
    }
}

package com.company;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {

    private static final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    private static final int servicePort = 65000;

    private static final int startPort = 30_000;

    private static final int endPort = 50_000;

    private static String getClassPathFromParent() {
        return System.getProperty("java.class.path", "./*");
    }

    private static String getJavaCmdFromParent() {
        return Objects.isNull(System.getProperty("java.home")) ? "java" : String.format("%s%sbin%sjava", System.getProperty("java.home"), File.separator, File.separator);
    }

    // JVM OPTS -Xmx10g -Xms10g -Xss2048k
    public static void main(String[] args) throws Exception{

        final String javaCmd = Main.getJavaCmdFromParent();
        final String classpath = Main.getClassPathFromParent();

        int childProcessesCount = (endPort - startPort) / 4_000;

        System.out.println("Child processes will be spawned (4000 threads each): " + childProcessesCount);

        ArrayList<Process> childs = new ArrayList<>();

        for(int i = 0; i < (endPort - startPort) / 4_000; i++){
            final ProcessBuilder proc = new ProcessBuilder(javaCmd, "-cp", classpath, Child.class.getName(), "servicePort:65000", "startPort:" + (startPort + i * 4000), "count:4000");
            proc.redirectErrorStream(true);
            proc.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            childs.add(proc.start());
        }

        new Thread(() -> {
            Socket s;
            try{
                ServerSocket serverSocket = new ServerSocket(servicePort);
                while(true){
                    s = serverSocket.accept();
                    BufferedReader read = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    queue.add(read.readLine());
                    read.close();
                    s.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            childs.forEach(Process::destroyForcibly);
            System.out.println("Destroyed");
        }));

        while(true){
            Thread.sleep(50);
            String str = queue.poll();
            if(str != null) System.out.println(str);
        }

    }
}

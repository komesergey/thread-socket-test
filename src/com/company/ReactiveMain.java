package com.company;

import java.io.File;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;

public class ReactiveMain {

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

        final String javaCmd = ReactiveMain.getJavaCmdFromParent();
        final String classpath = ReactiveMain.getClassPathFromParent();

        int childProcessesCount = (endPort - startPort) / 4_000;

        System.out.println("Child processes will be spawned (4000 threads each): " + childProcessesCount);

        ArrayList<Process> childs = new ArrayList<>();

        for(int i = 0; i < (endPort - startPort) / 4_000; i++){
            final ProcessBuilder proc = new ProcessBuilder(javaCmd, "-cp", classpath, ReactiveChild.class.getName(), "servicePort:65000", "startPort:" + (startPort + i * 4000), "count:4000");
            proc.redirectErrorStream(true);
            proc.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            childs.add(proc.start());
        }

        Thread thread = new Thread(() -> {
            Socket s;
            try{
                ServerSocket serverSocket = new ServerSocket(servicePort);
                while(true){
                    s = serverSocket.accept();
                    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                    out.println(System.currentTimeMillis());
                    out.close();
                    s.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        });

        thread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            childs.forEach(Process::destroyForcibly);
            System.out.println("Destroyed");
        }));

        thread.join();
    }
}

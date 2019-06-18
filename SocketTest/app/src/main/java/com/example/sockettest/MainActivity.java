package com.example.sockettest;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ServerThread";
    public static final int SERVER_PORT = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Thread myThread = new Thread(new ServerThread());
        myThread.start();
    }

    class ServerThread implements Runnable {
        Socket s;
        ServerSocket ss;
        InputStreamReader isr;
        BufferedReader bufferedReader;
        String message;
        PrintWriter pw;
        Handler h = new Handler();
        String connectionStatus;
        @Override
        public void run() {
            try {
                ss = new ServerSocket(SERVER_PORT);
                s = ss.accept();
                pw = new PrintWriter(s.getOutputStream());
                pw.println("Hey Client!\n");
                pw.flush();

                Thread readThread = new Thread(readFromClient);
                readThread.setPriority(Thread.MAX_PRIORITY);
                readThread.start();
                Log.e(TAG, "Sent");
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            if ( s != null ) {
                try {
                    connectionStatus = "Connection successful";
                    Log.e(TAG, connectionStatus);
                    h.post(showConnectionStatus);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private Runnable readFromClient = new Runnable() {

            @Override
            public void run() {
                try {
                    Log.e(TAG, "Reading from client");
                    isr = new InputStreamReader(s.getInputStream());
                    bufferedReader = new BufferedReader(isr);
                    while ((message = bufferedReader.readLine()) != null) {
                        Log.d("ServerActivity", message);
                    }
                    bufferedReader.close();
                    closeAll();
                    Log.e(TAG, "OUT OF WHILE");
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        public void closeAll() {
            try {
                Log.e(TAG, "Closing all");
                pw.close();
                s.close();
                ss.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        private Runnable showConnectionStatus = new Runnable() {
            @Override
            public void run() {
                try {
                    Toast.makeText(getApplicationContext(), connectionStatus, Toast.LENGTH_SHORT).show();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }
}

package com.example.sockettransfer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ServerThread";
    public static final int SERVER_PORT = 9000;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.textView);

        Thread serverThread = new Thread(new ServerThread());
        serverThread.start();
    }

    class ServerThread implements Runnable {
        Socket client;
        ServerSocket server;
        InputStreamReader isr;
        BufferedReader bufferedReader;
        String message;
        PrintWriter output;
        Handler handler = new Handler();
        String connectionStatus;

        @Override
        public void run() {
            try {
                server = new ServerSocket(SERVER_PORT);
                client = server.accept();
                output = new PrintWriter(client.getOutputStream());

                Thread readThread = new Thread(readFromClient);
                readThread.setPriority(Thread.MAX_PRIORITY);
                readThread.start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            if ( client != null ) {
                connectionStatus = "Connection successful";
                handler.post(showConnectionStatus);
            }
        }

        public void closeAll() {
            try {
                Log.e(TAG, "Closing all");
                output.close();
                client.close();
                server.close();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }

        private Runnable readFromClient = new Runnable() {
            @Override
            public void run() {
                try {
                    Log.e(TAG, "Reading from client");
                    isr = new InputStreamReader(client.getInputStream());
                    bufferedReader = new BufferedReader(isr);
                    while ((message = bufferedReader.readLine()) != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(message);
                            }
                        });
                    }
                    bufferedReader.close();
                    closeAll();
                    Log.e(TAG, "OUT OF WHILE");
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }
        };

        private Runnable showConnectionStatus = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), connectionStatus, Toast.LENGTH_SHORT).show();
            }
        };
    }
}

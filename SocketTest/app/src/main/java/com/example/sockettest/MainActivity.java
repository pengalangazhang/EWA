package com.example.sockettest;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ServerThread";
    Handler updateConversationHandler;
    Thread serverThread = null;
    private TextView textView;
    private EditText editText;
    public static final int SERVER_PORT = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = (EditText) findViewById(R.id.editText);
        Thread myThread = new Thread(new ServerThread());
        myThread.start();
    }

    class ServerThread implements Runnable {
        Socket s;
        ServerSocket ss;
        InputStreamReader isr;
        BufferedReader bufferedReader;
        String message;
        Handler h = new Handler();
        @Override
        public void run() {
            try {
                ss = new ServerSocket(SERVER_PORT);
                while(true) {
                    s = ss.accept();
                    isr = new InputStreamReader(s.getInputStream());
                    bufferedReader = new BufferedReader(isr);
                    message = bufferedReader.readLine();
                    Log.d(TAG, message + "Hello");

                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplication(),message,Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

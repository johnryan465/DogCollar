package com.stjosephscollege.john.dogcollar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
public class MainActivity extends Activity {
    Button Connect;
    Timer timer;
    MyTimerTask myTimerTask;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private static String address = "30:15:01:07:09:65";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private InputStream inStream = null;
    Handler handler = new Handler();
    byte delimiter = 10;
    boolean stopWorker = false;
    int readBufferPosition = 0;
    byte[] readBuffer = new byte[1024];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Connect = (Button) findViewById(R.id.connect);
        CheckBt();
        Connect();
        timer = new Timer();
        myTimerTask = new MyTimerTask();
        timer.schedule(myTimerTask, 1000, 1000);
    }
    private void CheckBt() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {Toast.makeText(getApplicationContext(), "Bluetooth Disabled !",Toast.LENGTH_SHORT).show();}
        if (mBluetoothAdapter == null){Toast.makeText(getApplicationContext(),"Bluetooth null !", Toast.LENGTH_SHORT).show();}
    }
    public void Connect() {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mBluetoothAdapter.cancelDiscovery();
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            btSocket.connect();
        } catch (IOException e) {
            try {btSocket.close();
            } catch (IOException e2) {}
        }
        beginListenForData();
    }
    public void onClickButton(View v){Connect();}
    private void writeData(String data) {
        try {outStream = btSocket.getOutputStream();
        } catch (IOException e) {}
        String message = data;
        byte[] msgBuffer = message.getBytes();
        try {outStream.write(msgBuffer);
        } catch (IOException e) {}
    }
    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Ping",Toast.LENGTH_SHORT).show();
                    writeData("1");
                }});
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {btSocket.close();
        } catch (IOException e) {}
    }
    public void beginListenForData()   {
        try {inStream = btSocket.getInputStream();
        } catch (IOException e) {}
        Thread workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker){
                    try{
                        int bytesAvailable = inStream.available();
                        if(bytesAvailable > 0){
                            byte[] packetBytes = new byte[bytesAvailable];
                            inStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++){
                                byte b = packetBytes[i];
                                if(b == delimiter){
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable(){public void run(){}});
                                }
                                else{readBuffer[readBufferPosition++] = b;}
                            }
                        }
                    }
                    catch (IOException ex){stopWorker = true;}
                }
            }
        });
        workerThread.start();
    }
}
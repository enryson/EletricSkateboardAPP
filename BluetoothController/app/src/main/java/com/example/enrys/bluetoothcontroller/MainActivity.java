package com.example.enrys.bluetoothcontroller;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.media.TransportMediator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import android.widget.TextView;


public class MainActivity extends Settings {
    ImageButton settingsbutton,ImgLedOn,conectionBtLogo;
    ProgressBar progressBar2;
    SeekBar  mySeekBar;
    TextView speed,toptext,textkmh,volt;
    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothDevice mBluetoothDevice = null;
    BluetoothSocket mBluetoothSocket = null;
    ConnectedThread connectedThread;
    InputStream mmInStream = null;
    OutputStream mmOutStream = null;

    boolean conection = false;
    private static final String TAG = "-->";
    private static final int BT_ACTIVATE_REQUEST = 1;
    private static final int BT_CONNECT_REQUEST = 2;
    private static final int MESSAGE_READ = 3;
    int valueProgress = 90;

    public static String sharedBluetoothMac;
    private boolean registered=false;

    StringBuilder bluetoothdata = new StringBuilder();

    private static String MAC = null;
    private Handler mHandler;



    UUID My_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public static String voltage = "Voltage: ";

    public void Connected() {
        ImageButton btn = (ImageButton)findViewById(R.id.conectionBtLogo);
        btn.setBackgroundResource(R.drawable.btconected);
    }
    public void Disconnected() {
        ImageButton btn = (ImageButton)findViewById(R.id.conectionBtLogo);
        btn.setBackgroundResource(R.drawable.btdisconected);
    }

    public void LedOn() {
        ImageButton btn = (ImageButton)findViewById(R.id.ImgLedOn);
        btn.setBackgroundResource(R.drawable.ledon);
    }
    public void LedOff() {
        ImageButton btn = (ImageButton)findViewById(R.id.ImgLedOn);
        btn.setBackgroundResource(R.drawable.ledoff);
    }


    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_layout);
        setContentView(R.layout.activity_main);

        setBoardName();

        conectionBtLogo = (ImageButton)findViewById(R.id.conectionBtLogo);
        settingsbutton = (ImageButton)findViewById(R.id.settingsbutton);
        ImgLedOn = (ImageButton)findViewById(R.id.ImgLedOn);
        speed = (TextView)findViewById(R.id.speed);
        volt = (TextView)findViewById(R.id.voltage);
        textkmh = (TextView) findViewById(R.id.textkmh);

        mySeekBar = (SeekBar) findViewById(R.id.mSeekBar);
        mySeekBar.setMax(TransportMediator.KEYCODE_MEDIA_RECORD);
        mySeekBar.setProgress(60);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/CODEBold.otf");
        speed.setTypeface(typeface);

        final Vibrator vb = (Vibrator)getSystemService(MainActivity.VIBRATOR_SERVICE);
        final ProgressBar battery = (ProgressBar) findViewById(R.id.progressBar);


        settingsbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Settings.class));
            }
        });


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null){
            Toast.makeText (getApplicationContext(), "dispositivo bluetooth nao encontrado",Toast.LENGTH_LONG).show();

        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BT_ACTIVATE_REQUEST);
        }
        bluetoothAutoConnect();


        conectionBtLogo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //vb.vibrate(100);
                if (conection){
                    try {
                        mBluetoothSocket.close();
                        conection = false;
                        Toast.makeText(getApplicationContext(), "Device Desconectado : " , Toast.LENGTH_LONG).show();
                        Disconnected();
                    }catch(IOException erro){
                        Toast.makeText(getApplicationContext(), "Erro Desconectado : "+ erro, Toast.LENGTH_LONG).show();
                    }
                }   else    {
                    Intent open_list = new Intent(MainActivity.this, DeviceList.class);
                    startActivityForResult(open_list, BT_CONNECT_REQUEST);
                }
            }
        });

        ImgLedOn.setOnClickListener(new View.OnClickListener() {
            Boolean flag = false;
            @Override
            public void onClick(View v) {

                vb.vibrate(80);
                if (conection){

                    if(flag) {
                        connectedThread.write("L");
                        LedOff();
                        flag = false;
                    } else {
                        connectedThread.write("O");
                        LedOn();
                        flag = true;
                    }
                }   else {
                    Toast.makeText(getApplicationContext(), "Device Desconectado : " , Toast.LENGTH_LONG).show();
                }
            }
        });

        mySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress , boolean fromUser) {
                //vb.vibrate(progress/10);
                valueProgress = (progress * 1) + 30;
            }
            @Override   public void onStartTrackingTouch(SeekBar seekBar) {            }
            @Override   public void onStopTrackingTouch(SeekBar seekBar) {   seekBar.setProgress(50);  }
        });

        final Handler handler1 = new Handler();                                          //Send String to the handler m
        Timer timer1 = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler1.post(new Runnable() {
                    public void run() {
                        if(conection){
                            try {
                                connectedThread.write(new StringBuilder(String.valueOf(valueProgress)).append("n").toString());
                            } catch (Exception e) {
                            }
                        } else{}
                    }
                });
            }
        };
        timer1.schedule(doAsynchronousTask, 0, 100);

        mHandler =  new Handler() {
            @Override
            public void handleMessage(Message msg) {       //Read data from the handler to recilve voltage
                if (msg.what == MESSAGE_READ){
                    int i = 1;
                    String recilvdata = (String) msg.obj;
                    bluetoothdata.append(recilvdata);
                    bluetoothdata.indexOf(recilvdata);
                        try {
                            int i2;
                            voltage = recilvdata;

                            float voltf = Float.parseFloat(voltage.replaceAll("v", "").replaceAll("\\s+", ""));
                            volt.setText("Voltage: " + voltf + "V");
                            Log.d("Voltage", String.valueOf(voltf));

                            if (voltf > 11.0f) {
                                i2 = 1;
                            } else {
                                i2 = 0;
                            }
                            if (voltf >= 30.0f) {
                                i = 0;
                            }
                            if ((i & i2) != 0) {

                                voltf = (float) (33.0d * (((double) voltf) - 22.2d));
                                if (voltf > 100){
                                    voltf = 100;
                                }
                                textkmh.setText(String.format("%.2f", voltf)+"%");
                                //textkmh.setText(voltf+"%");
                                battery.setProgress(Math.round(voltf));

                                if (voltf < 23.0f) {
                                    battery.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
                                } else {
                                    battery.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP);
                                }
                            }
                        } catch (Exception e) {
                        }
                }
            }
        };

        final Handler handler = new Handler();                                          //Send String to the handler m
        Timer timer = new Timer();
        TimerTask task2 = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            connectedThread.write("m");
                        } catch (Exception e) {
                        }
                    }
                });
            }
        };
        timer.schedule(task2, 0, 400);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case BT_ACTIVATE_REQUEST:
                if(resultCode == Activity.RESULT_OK){
                    Toast.makeText(getApplicationContext(), "bluetooth Ativado",Toast.LENGTH_LONG).show();
                }   else {
                    Toast.makeText(getApplicationContext(), "bluetooth nao ativado",Toast.LENGTH_LONG).show();
                    finish();
                }

                break;
            case BT_CONNECT_REQUEST:
                if (resultCode == Activity.RESULT_OK){

                    MAC = data.getExtras().getString(DeviceList.MAC_ADRESS);
                    Log.d("MACADRESS",MAC);

                    setMACAdress();
                    bluetoothConnectMAC();

                }   else    {
                    Toast.makeText(getApplicationContext(), "Falha MAC",Toast.LENGTH_LONG).show();
                }
        }
    }

    public void bluetoothAutoConnect () {
        String file = "MACADRESS";
        String temp="";
        try {
            FileInputStream fin = openFileInput(file);
            int c;
            while( (c = fin.read()) != -1){
                temp = temp + Character.toString((char)c);
            }
            MAC = temp;
            bluetoothConnectMAC();

        }   catch(Exception e){     }
    }

    public void setMACAdress () {
        String sharedValue1 = MAC;
        String fileName = "MACADRESS";
        FileOutputStream outputStream = null;
        try {
            outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(sharedValue1.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void bluetoothConnectMAC () {
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(MAC);
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(My_UUID);
            mBluetoothSocket.connect();
            conection = true;
            connectedThread = new ConnectedThread(mBluetoothSocket);
            connectedThread.start();
            Toast.makeText(getApplicationContext(), "Conectado : "+ MAC, Toast.LENGTH_LONG).show();
            Connected();
        }catch(IOException erro){
            conection = false;
            Toast.makeText(getApplicationContext(), "Erro Desconectado : "+ MAC, Toast.LENGTH_LONG).show();
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()


            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    String btdata = new String(buffer, 0 , bytes);

                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, btdata).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String outputwrite ) {
            byte[] msgBuffer = outputwrite.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }*/
        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    Intent intent1 = new Intent(MainActivity.this, MainActivity.class);

                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            if(registered) {
                                unregisterReceiver(mReceiver);
                                registered=false;
                            }
                            startActivity(intent1);
                            finish();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            if(registered) {
                                unregisterReceiver(mReceiver);
                                registered=false;
                            }
                            startActivity(intent1);
                            finish();
                            break;
                    }
                }
            }
        };
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            mBluetoothSocket.close();
            conection = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mBluetoothSocket.close();
            conection = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

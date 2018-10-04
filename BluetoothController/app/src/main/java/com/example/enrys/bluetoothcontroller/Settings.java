package com.example.enrys.bluetoothcontroller;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Settings extends AppCompatActivity  {
    public static String sharedValue;
    public static TextView toptext;
    public static EditText et,vMin,vMax;
    Button bt_save;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_layout);

        setContentView(R.layout.settings);
        bt_save = (Button) findViewById(R.id.bt_save);
        setBoardName();
        getBoardName();

        toptext.setText("Customize");

        bt_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedValue = et.getText().toString();
                String fileName = "NomeApp";
                FileOutputStream outputStream = null;
                try {
                    outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
                    outputStream.write(sharedValue.getBytes());
                    outputStream.close();
                    setBoardName();
                    Intent macReturn = new Intent();
                    setResult(RESULT_OK, macReturn);
                    //startActivity(new Intent(new MainActivity,Settings.this));
                    finish();
                } catch (Exception e) {     e.printStackTrace();    }
            }

        });
    }

    public String getBoardName () {
        String file = "NomeApp";
        String temp="";
        et = (EditText) findViewById(R.id.editText);
        try {
            FileInputStream fin = openFileInput(file);
            int c;
            while( (c = fin.read()) != -1){
                temp = temp + Character.toString((char)c);
            }
            et.setText(temp);
        }
        catch(Exception e){
        }
        return temp;
    }

    public String setBoardName () {
        getBoardName();
        String file = "NomeApp";
        String temp="";

        toptext = (TextView) findViewById(R.id.toptext);
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/CODEBold.otf");
        toptext.setTypeface(typeface);
        et = (EditText) findViewById(R.id.editText);
        //

        try {
            FileInputStream fin = openFileInput(file);
            int c;
            while( (c = fin.read()) != -1){
                temp = temp + Character.toString((char)c);
            }
            toptext.setText(temp);
        }
        catch(Exception e){
        }
        return temp;
    }
}


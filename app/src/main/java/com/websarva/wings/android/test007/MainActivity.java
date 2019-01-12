package com.websarva.wings.android.test007;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainListener mainListener = new MainListener();         // リスナ開始

        Button mainbt_receive_start = findViewById(R.id.mainbt_receive_start);
        mainbt_receive_start.setOnClickListener(mainListener);


    }


    // クリック関係リスナここ
    private class MainListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            int objID = view.getId();

            switch (objID) {
                case R.id.mainbt_receive_start:
                     Toast.makeText(getApplicationContext(), "mainbt_receive_start : ON", Toast.LENGTH_LONG).show();
                    break;
                default:
                    String mesg = "認識されないオブジェクトがクリックされました。:" + String.valueOf(objID);
                    Toast.makeText(getApplicationContext(), mesg, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

}

package com.example.measureheartrate;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ShowResult extends AppCompatActivity {

    public TextView txtHeartRate;
    public TextView txtAdvices;
    public Button btnHome;
    private String textShow;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_result);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Kết quả nhịp tim");

        txtHeartRate=(TextView)findViewById(R.id.heartBeat);
        txtAdvices=(TextView)findViewById(R.id.editText);
        btnHome=(Button)findViewById(R.id.btnHome);

        final Context myContext=this;
        String result=getIntent().getStringExtra("result");
        txtHeartRate.setText(result);
        int heartBeat=Integer.parseInt(result);
        if(heartBeat>=60&&heartBeat<=100){
            textShow="Tim của bạn rất khỏe ^.^ !";
            txtAdvices.setBackgroundColor(getResources().getColor(R.color.green));
        }
        if(heartBeat>40&&heartBeat<=60){
            textShow="Nhịp tim của bạn khá thấp, bạn nên đi khám sức khỏe !";
            txtAdvices.setBackgroundColor(getResources().getColor(R.color.orange));
        }
        if(heartBeat>100&&heartBeat<=120){
            textShow="Nhịp tim của bạn khá cao, bạn nên đi khám sức khỏe !";
            txtAdvices.setBackgroundColor(getResources().getColor(R.color.orange));
        }
        if(heartBeat>120){
            textShow="Nhịp tim của bạn rất cao ! Bạn cần đến khám bệnh viện sớm !";
            txtAdvices.setBackgroundColor(getResources().getColor(R.color.red));
        }
        if(heartBeat<40){
            textShow="Nhịp tim của bạn rất thấp ! Bạn cần đến khám bệnh viện sớm !";
            txtAdvices.setBackgroundColor(getResources().getColor(R.color.red));
        }
        txtAdvices.setText(textShow);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent home=new Intent(myContext,MainActivity.class);
                myContext.startActivity(home);
            }
        });
    }

}

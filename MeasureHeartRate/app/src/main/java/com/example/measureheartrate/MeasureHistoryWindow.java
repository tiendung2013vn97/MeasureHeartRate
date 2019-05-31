package com.example.measureheartrate;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

public class MeasureHistoryWindow extends AppCompatActivity {
    ListView lvMeasureHistory;
    Context myContext;
    ArrayList<MeasureResult> arrMeasureResult;
    private static final String DirName = "YourHeartRate";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_history_window);
        lvMeasureHistory=(ListView)findViewById(R.id.lvMeasureHistory);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        myContext=this;
        if(FileUtility.isExternalStorageWritable()){
            Log.i("Writable","true");
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        arrMeasureResult=new ArrayList<MeasureResult>();
        arrMeasureResult=FileUtility.readMeasureHistory(DirName,"MeasureHistory");
        MeasureHistoryAdapter measureHistoryAdapter=new MeasureHistoryAdapter(this,R.layout.measure_history_item,arrMeasureResult);
        lvMeasureHistory.setAdapter(measureHistoryAdapter);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent i=new Intent(myContext,MainActivity.class);
                startActivity(i);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i=new Intent(myContext,MainActivity.class);
        startActivity(i);
    }
}

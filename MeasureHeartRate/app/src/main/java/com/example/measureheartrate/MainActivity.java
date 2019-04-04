package com.example.measureheartrate;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera.PreviewCallback;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.security.Policy;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    public  Button btnSwitch;
    public static boolean isFlashOn;
    private boolean hasFlash;
    private static final int CAMERA_PERMISSION = 1;
    private static Context myContext;
    int ColorHeart=0;

    private ImageView imgHeart;
    private TextView txtHeartRate;

    private static SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static View image = null;
    public  TextView text = null;
    private static long startTime=0;

    private static int[] arrSumRed=new int[1000];
    private static long[] arrTime=new long[1000];
    private static int arrIndex=-1;

    private static boolean isMeasureFinished=false;
    private static boolean isShowResult=false;
    private static long[] arrResult=new long[10];
    private static int indexOfResult=0;
    private static int resetisShowResult=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnSwitch = (Button) findViewById(R.id.btnSwitch);

        imgHeart=(ImageView)findViewById(R.id.imgHeart);
        txtHeartRate=(TextView)findViewById(R.id.txtHeartRate);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext=this;
        isFlashOn=false;
        preview = (SurfaceView) findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        myContext=this;
        //check Camera Permission__________________________________________________________
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION);

        }


        //check has flash___________________________________________________________________
        hasFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (!hasFlash) {
            // device doesn't support flash
            // Show alert message and close the application
            AlertDialog alert = new AlertDialog.Builder(MainActivity.this)
                    .create();
            alert.setTitle("Error");
            alert.setMessage("Sorry, your device doesn't support flash light!");
            alert.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // closing the application
                    finish();
                }
            });
            alert.show();
            return;
        }
        //end check has flash_______________________________________________________________


        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                arrIndex=-1;
                indexOfResult=0;
                isMeasureFinished=false;
                isShowResult=false;
                btnSwitch.setEnabled(false);
                btnSwitch.setText("Đang đo...");

                isFlashOn=true;
                HeartAnimation();
                Camera.Parameters parameters = camera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                Camera.Size size = getSmallestPreviewSize(200, 200, parameters);
                if (size != null) {
                    parameters.setPreviewSize(size.width, size.height);
                    //Log.d(TAG, "Using width=" + size.width + " height=" + size.height);
                }
                camera.setParameters(parameters);
                camera.startPreview();
                startTime=System.currentTimeMillis();

            }
        });


    }//end OnCreate

    private void HeartAnimation(){
        final Handler handler = new Handler();
        Timer timer = new Timer(false);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(isFlashOn){
                            if(ColorHeart==0){
                                ColorHeart=1;
                                imgHeart.setImageResource(R.drawable.heart_red);

                            }else{
                                ColorHeart=0;
                                imgHeart.setImageResource(R.drawable.heart_orange);
                            }
                            HeartAnimation();
                        }
                    }
                });
            }
        };
        timer.schedule(timerTask,500);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case CAMERA_PERMISSION:{
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    AlertDialog alert = new AlertDialog.Builder(MainActivity.this)
                            .create();
                    alert.setTitle("Error");
                    alert.setMessage("Request Camera Permission fail!");
                    alert.setButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // closing the application
                            finish();
                        }
                    });
                    alert.show();
                }else{

                }

                break;
            }
            default:break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        camera = Camera.open();
        // startTime = System.currentTimeMillis();
    }
    @Override
    public void onPause() {
        super.onPause();

        btnSwitch.setEnabled(true);
        btnSwitch.setText("Bắt đầu đo");
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
    }


    private static PreviewCallback previewCallback = new PreviewCallback() {

        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            if (data == null) throw new NullPointerException();
            Camera.Size size = cam.getParameters().getPreviewSize();
            if (size == null) throw new NullPointerException();


            int width = size.width;
            int height = size.height;

            long curTime=System.currentTimeMillis();
            if(isMeasureFinished){Log.i("haha:", isShowResult+";"+arrIndex );
                if(isShowResult==false) {

                    MeasureHeart(MeasureHeart(0));
                    isShowResult=true;

                }
                if(resetisShowResult==1){
                    resetisShowResult=0;
                    MeasureHeart(MeasureHeart(0));
                    isShowResult=true;
                }
            }else{
                arrIndex++;
            }
            if(curTime-startTime>500&&isMeasureFinished==false){
                arrTime[arrIndex]=curTime;
                arrSumRed[arrIndex]=ImageProcessing.decodeYUV420SPtoRedSum(data.clone(),height,width);
                Log.i("information 2:", arrSumRed[arrIndex] +";"+arrIndex );
            }
            if(curTime-startTime>3500&&isMeasureFinished==false){

                isMeasureFinished=true;

            }



        }

    };
    private static int MeasureHeart(int stIndex){
        int startIndex = stIndex;
        ///Toast.makeText(myContext, ""+stIndex, Toast.LENGTH_SHORT).show();
        for (int i = stIndex; i < arrIndex; i++) {
            if (arrSumRed[startIndex] < arrSumRed[i ]) {
                startIndex = i ;
            }
        }


        //using limit time to calculate startIndex and endIndex
        int endIndex = startIndex;
        int indexMin=startIndex;
        double interval=-1;
        if(arrTime[startIndex]-arrTime[stIndex]<=500){//max nam ben trai,startIndex=max
            //Toast.makeText(myContext, "1", Toast.LENGTH_SHORT).show();
            while (arrTime[endIndex] - arrTime[startIndex] < 500)
            {
                endIndex++;
            }
            for (int i=startIndex;i<=endIndex;i++){
                if(arrSumRed[indexMin]>arrSumRed[i]&&(arrTime[i]-arrTime[startIndex]<=300)){indexMin=i;}
                Log.i("interval 1", arrSumRed[endIndex]+"; "+arrSumRed[startIndex]+"; "+arrSumRed[indexMin]+";"+arrSumRed[i]);
            }
            interval=(arrTime[indexMin]-arrTime[startIndex])*2;


        }
        else{//max nam ben phai, endIndex =max

            while (arrTime[endIndex] - arrTime[startIndex] < 500)
            {
                startIndex--;
            }
            for (int i=startIndex;i<=endIndex;i++){
                if(arrSumRed[indexMin]>arrSumRed[i]&&(arrTime[endIndex]-arrTime[i]<=300)){indexMin=i;}
                Log.i("interval 2", arrSumRed[endIndex]+"; "+arrSumRed[startIndex]+"; "+arrSumRed[indexMin]+";"+arrSumRed[i]);
            }
            interval=(arrTime[endIndex]-arrTime[indexMin])*2;
        }

        long result=Math.round(60*1000.0*0.67/(interval));
        arrResult[indexOfResult]=result;

        if(indexOfResult==1){
            long showResult=arrResult[0]>arrResult[1]?arrResult[1]:arrResult[0];
            if(showResult>110){
                Log.i("ancur:", isShowResult+";"+showResult );
                arrIndex=-1;
                startTime=System.currentTimeMillis();
                indexOfResult=0;
                isMeasureFinished=false;
                resetisShowResult=1;
            }else{
            isFlashOn=false;

            Intent showResultActivity=new Intent(myContext,ShowResult.class);
            showResultActivity.putExtra("result",showResult+"");
            myContext.startActivity(showResultActivity);
//            text.setText(showResult+"");
//            arrIndex=0;
//            isMeasureFinished=false;
//            isShowResult=false;
//            camera.stopPreview();
//            btnSwitch.setText("Bắt đầu đo");
//            btnSwitch.setEnabled(true);
        }
        }else{ indexOfResult++;}


        return endIndex;
    }

    private static SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(previewHolder);
                camera.setPreviewCallback(previewCallback);
            } catch (Throwable t) {
                Log.e("Error", "Exception in setPreviewDisplay()", t);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            //ignore
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // ignore
        }
    };

    private static Camera.Size getSmallestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            Log.i("vkl",""+size);
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea < resultArea) result = size;
                }
            }
        }

        return result;
    }



}

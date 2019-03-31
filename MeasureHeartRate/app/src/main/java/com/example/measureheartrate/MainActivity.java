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
    private Button btnSwitch;
    private Camera camera;
    private boolean isFlashOn;
    private boolean hasFlash;
    private Parameters params;
    private static final int CAMERA_PERMISSION = 1;
    private static final int FLASHLIGHT_PERMISSION = 2;
    public static Bitmap[] bitmap=new Bitmap[100];;
    private Context myContext;
    private CameraPreview mPreview;
    private Camera.PictureCallback mPicture;
    private LinearLayout cameraPreview;
    int ColorHeart=0;
    int temp=0;
    long[] sumArr=new long[100];

    private ImageView imgHeart;
    private TextView txtHeartRate;
    private CameraManager mCameraManager;
    private String mCameraId;

    long timeStart=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnSwitch = (Button) findViewById(R.id.btnSwitch);

        imgHeart=(ImageView)findViewById(R.id.imgHeart);
        txtHeartRate=(TextView)findViewById(R.id.txtHeartRate);
        cameraPreview = (LinearLayout) findViewById(R.id.cPreview);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext=this;
        isFlashOn=false;

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


        getCamera();

        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSwitch.setText("Đang đo...");
                int i=0;
                HeartAnimation();
                if (isFlashOn) {
                    turnOffFlash();
                } else {
                    turnOnFlash();
                }
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
    private void CaptureImages(int maxTime){
        long timeExpand= System.currentTimeMillis()/1000-timeStart;

        if(timeExpand<maxTime){
            mPicture = getPictureCallback();
            camera.takePicture(null, null, mPicture);
            btnSwitch.setEnabled(false);
        }else{
            turnOffFlash();
            long sum=0;
            for(int i=0;i<temp;i++){
                sumArr[i]=0;
                for(int bmW=0;bmW<bitmap[i].getWidth();bmW++){
                    for(int bmH=0;bmH<bitmap[i].getHeight();bmH++){
                        sumArr[i]+= Color.red(bitmap[i].getPixel(bmW,bmH));

                    }
                }
            }
            for(int i=0;i<temp;i++){
                Toast.makeText(myContext,String.valueOf(sumArr[i]*1.0/(bitmap[0].getWidth()*bitmap[0].getHeight())),Toast.LENGTH_SHORT).show();
            }
            btnSwitch.setEnabled(true);
            btnSwitch.setText("Bắt đầu đo");
        }

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
                    getCamera();
                }

                break;
            }
            default:break;
        }
    }

    //getCamera______________________________________________________________________________
    private void getCamera() {
        if (camera == null) {
            try {
                camera = Camera.open(0);

                camera.setDisplayOrientation(90);
                mPreview = new CameraPreview(myContext, camera);
                cameraPreview.addView(mPreview);
                android.view.ViewGroup.LayoutParams lp=mPreview.getLayoutParams();
                lp.height=1;
                lp.width=1;
                mPreview.setLayoutParams(lp);

                params = camera.getParameters();
                params.setFlashMode(Parameters.FLASH_MODE_OFF);
                camera.setParameters(params);
                camera.startPreview();

            } catch (RuntimeException e) {
                Log.e("Failed Camera. Error: ", e.getMessage());
                Toast.makeText(this,"Loi mo den",Toast.LENGTH_SHORT).show();

            }
        }
    }
    //end getCamera__________________________________________________________________________

    private void turnOnFlash() {
        if (!isFlashOn) {
            if (camera == null || params == null) {
                return;
            }
            timeStart= System.currentTimeMillis()/1000;
            params.setFlashMode(Parameters.FLASH_MODE_TORCH);
            camera.setParameters(params);
            camera.startPreview();
            isFlashOn = true;
            CaptureImages(10);

        }
    }

    private void turnOffFlash() {
        if (isFlashOn) {
            timeStart=0;
            if (camera == null || params == null) {
                return;
            }

            releaseCamera();

        }
    }private Camera.PictureCallback getPictureCallback() {
        Camera.PictureCallback picture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                bitmap[temp] = BitmapFactory.decodeByteArray(data, 0, data.length);

                //Toast.makeText(myContext, String.valueOf(data), Toast.LENGTH_LONG).show();
                params.setFlashMode(Parameters.FLASH_MODE_TORCH);
                camera.setParameters(params);
                mPreview.refreshCamera(camera);
                temp++;
                CaptureImages(10);


            }
        };
        return picture;
    }


//
//    public void onResume() {
//
//        super.onResume();
//        if(camera == null) {
////            camera = Camera.open();
////            camera.setDisplayOrientation(90);
////            if(isFlashOn){
////                params.setFlashMode(Parameters.FLASH_MODE_TORCH);
////                camera.setParameters(params);
////            }
////
////            mPreview.refreshCamera(camera);
//            Log.d("nu", "null");
//        }else {
//            Log.d("nu","no null");
//        }
//
//    }
    @Override
    protected void onPause() {
        super.onPause();
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera != null) {
            if(isFlashOn){
                params.setFlashMode(Parameters.FLASH_MODE_OFF);
                camera.setParameters(params);
            }
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

    private void releaseCamera() {
        // stop and release camera
        if (camera != null) {
            if(isFlashOn){
                params.setFlashMode(Parameters.FLASH_MODE_OFF);
                camera.setParameters(params);
                isFlashOn=false;
            }
            camera.stopPreview();
            camera.setPreviewCallback(null);
//            camera.release();
//            camera = null;
        }
    }

}

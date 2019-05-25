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
    public Button btnSwitch;
    public static boolean isFlashOn;
    private boolean hasFlash;
    private static final int CAMERA_PERMISSION = 1;
    private static Context myContext;
    int ColorHeart;

    private ImageView imgHeart;
    private TextView txtHeartRate;

    private static SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static View image = null;
    public TextView text = null;
    private static long startTime;

    private static long[] arrSumRed ;
    private static long[] arrTime ;
    private static int arrIndex;

    private static boolean isMeasureFinished;

    private static float minInterval;
    private static float maxInterval;
    private static float intervalDeiationRate;

    private static double arrResult[][] ;
    private static int arrResultIndex ;
    private static int peaks[];
    private static int peakIndex;

    private static boolean loop ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toast.makeText(this,"ONCREATE",Toast.LENGTH_SHORT).show();
        btnSwitch = (Button) findViewById(R.id.btnSwitch);

        imgHeart = (ImageView) findViewById(R.id.imgHeart);
        txtHeartRate = (TextView) findViewById(R.id.txtHeartRate);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        isFlashOn = false;
        preview = (SurfaceView) findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        myContext = this;

        minInterval = 500f;//max beat per second is 120
        maxInterval = 1500f;//min beat persecond is 40
        intervalDeiationRate = 0.01f;


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

        peakIndex = 0;
        arrResultIndex = 0;
        arrIndex = -1;
        isMeasureFinished = false;
        loop = true;
        ColorHeart = 0;
        startTime = 0;
        arrSumRed = new long[3000];
        arrTime = new long[3000];
        arrResult = new double[30000][3];
        peaks = new int[256];

        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Log.i("loop",loop+"");
                arrIndex = -1;
                isMeasureFinished = false;
                btnSwitch.setEnabled(false);
                btnSwitch.setText("Đang đo...");

                isFlashOn = true;
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
                startTime = System.currentTimeMillis();

            }
        });


    }//end OnCreate

    private void HeartAnimation() {
        final Handler handler = new Handler();
        Timer timer = new Timer(false);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFlashOn) {
                            if (ColorHeart == 0) {
                                ColorHeart = 1;
                                imgHeart.setImageResource(R.drawable.heart_red);

                            } else {
                                ColorHeart = 0;
                                imgHeart.setImageResource(R.drawable.heart_orange);
                            }
                            HeartAnimation();
                        }
                    }
                });
            }
        };
        timer.schedule(timerTask, 500);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSION: {
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
                } else {

                }

                break;
            }
            default:
                break;
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

            long curTime = System.currentTimeMillis();
            if (isMeasureFinished) {

                if(loop){
                    MeasureHeartBeat();
                }

            } else {
                arrIndex++;
            }
            if (curTime - startTime > 1000 && isMeasureFinished == false) {
                arrTime[arrIndex] = curTime;
                arrSumRed[arrIndex] = ImageProcessing.decodeYUV420SPtoRedSum(data.clone(), height, width);
                Log.i("arrSumRed:", arrSumRed[arrIndex] + ";" + (curTime % 100000) * 1.0 / 1000);
            }
            if (curTime - startTime > 15000 && isMeasureFinished == false) {

                isMeasureFinished = true;

            }


        }

    };

    private static int pickHighestPeak(int startIndexOfPeaks, int endIndexOfPeaks) {
        int maxPeak = startIndexOfPeaks;

        for (int i = startIndexOfPeaks; i <= endIndexOfPeaks; i++) {

            if (arrSumRed[peaks[maxPeak]] < arrSumRed[peaks[i]]) {
                maxPeak = i;
            }

        }

        return maxPeak;
    }

    //Count same interval between on peaks
    private static double[] CountSameIntervalAndAverageInterval(int i, int j) { //i,j is index of peaks
        double[] result = new double[3];
        int count = 1;

        float targetInterval = arrTime[peaks[j]] - arrTime[peaks[i]];
        float sumInterval = targetInterval;
        long sumRed = arrSumRed[i] + arrSumRed[j];
        //Loop toward the head of array peaks
        for (int idBefore = i; idBefore >= 0; ) {

            boolean flag = false;

            int startIndexOfPeaks = idBefore;
            for (; startIndexOfPeaks >= 0; startIndexOfPeaks--) {

                if ((arrTime[peaks[idBefore]] - arrTime[peaks[startIndexOfPeaks]]) > (targetInterval + targetInterval * intervalDeiationRate)) {
                    startIndexOfPeaks--;
                    break;
                }

            }

            if (startIndexOfPeaks == -1) {// maybe still existing interval >=arrTime[peaks[endIndexOfPeaks]]
                startIndexOfPeaks = 0;
            }


            int endIndexOfPeaks = idBefore;
            for (; endIndexOfPeaks >= 0; endIndexOfPeaks--) {

                if ((arrTime[peaks[idBefore]] - arrTime[peaks[endIndexOfPeaks]]) >= (targetInterval - targetInterval * intervalDeiationRate)) {
                    flag = true;
                    break;
                }

            }

            if (flag == true) {//flag =true mean exsiting fit peak
                count++;
                int maxPeakLocal = pickHighestPeak(startIndexOfPeaks, endIndexOfPeaks);
                Log.i("timer:", maxPeakLocal + ";" + idBefore);
                sumRed += arrSumRed[peaks[maxPeakLocal]];
                sumInterval += (arrTime[peaks[idBefore]] - arrTime[peaks[maxPeakLocal]]);
                idBefore = maxPeakLocal;
            } else {
                idBefore--;
            }

        }

        //Loop toward the end of array peaks
        for (int idAfter = j; idAfter <= peakIndex; ) {

            boolean flag = false;

            int startIndexOfPeaks = idAfter;
            for (; startIndexOfPeaks <= peakIndex; startIndexOfPeaks++) {

                if ((arrTime[peaks[startIndexOfPeaks]] - arrTime[peaks[idAfter]]) >= (targetInterval - targetInterval * intervalDeiationRate)) {
                    flag = true;
                    break;
                }

            }

            if (startIndexOfPeaks > peakIndex) {//stop loop toward the end of peaks
                idAfter = peakIndex + 1;
                break;
            }

            int endIndexOfPeaks = idAfter;
            for (; endIndexOfPeaks <= peakIndex; endIndexOfPeaks++) {

                if ((arrTime[peaks[endIndexOfPeaks]] - arrTime[peaks[idAfter]]) > (targetInterval + targetInterval * intervalDeiationRate)) {
                    break;
                }

            }

            endIndexOfPeaks--;

            if (flag == true) {//flag =true mean exsiting fit peak
                count++;
                int maxPeakLocal = pickHighestPeak(startIndexOfPeaks, endIndexOfPeaks);
                Log.i("timer2:", maxPeakLocal + ";" + idAfter);
                sumRed += arrSumRed[peaks[maxPeakLocal]];
                sumInterval += (arrTime[peaks[maxPeakLocal]] - arrTime[peaks[idAfter]]);
                idAfter = maxPeakLocal;
            } else {
                idAfter++;
            }

        }

        result[0] = count;
        result[1] = sumInterval / count;
        result[2] = sumRed;
        return result;
    }


    //Measure HeartBeat
    private static int MeasureHeartBeat() {

        int heartBeat = 0;

        for (int i = 1; i < arrIndex - 1; i++) {

            if ((arrSumRed[i] > arrSumRed[i - 1] && arrSumRed[i] >= arrSumRed[i + 1])
                    || (arrSumRed[i] >= arrSumRed[i - 1] && arrSumRed[i] > arrSumRed[i + 1])) {
                peaks[peakIndex] = i;
                Log.i("peaks: ", peakIndex + ";" + i);
                peakIndex++;
            }

        }
        peakIndex--;

        for (int i = 0; i < peakIndex; i++) {
            for (int j = i + 1; j <= peakIndex; j++) {

                float interval = arrTime[peaks[j]] - arrTime[peaks[i]];

                if (interval >= minInterval && interval <= maxInterval) {

                    Log.i("oday", "" + i + ";" + j + ";" + interval);
                    double[] resultCount;
                    resultCount = CountSameIntervalAndAverageInterval(i, j);
                    arrResult[arrResultIndex][0] = resultCount[0];//count interval repeat
                    arrResult[arrResultIndex][1] = resultCount[1];//interval
                    arrResult[arrResultIndex][2] = resultCount[2];//sum red

                    Log.i("beatAverage: ",arrResultIndex+";"+ resultCount[0] + "; " + resultCount[1] + ";" + resultCount[2]);

                    arrResultIndex++;
                }

            }
        }
        arrResultIndex--;
        double maxCount = 0;

        for (int i = 0; i <= arrResultIndex; i++) {

            if (arrResult[i][0] > maxCount) {
                maxCount = arrResult[i][0];
            }

        }
        Log.i("maxCount",maxCount+"");
        double maxRedSum=0;
        int indexMaxRedSum=-1;
        for (int i = 0; i <= arrResultIndex; i++) {

            if (arrResult[i][0] == maxCount&&arrResult[i][2]>maxRedSum) {
                maxRedSum=arrResult[i][2];
                indexMaxRedSum=i;
            }

        }

        heartBeat=(int)( 60000f/(arrResult[indexMaxRedSum][1]) );
    Log.i("rsult",arrResult[indexMaxRedSum][1]+";"+heartBeat);
        Intent showResultActivity=new Intent(myContext,ShowResult.class);
            showResultActivity.putExtra("result",heartBeat+"");
            myContext.startActivity(showResultActivity);

        loop=false;
        return heartBeat;

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
            //Log.i("vkl",""+size);
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

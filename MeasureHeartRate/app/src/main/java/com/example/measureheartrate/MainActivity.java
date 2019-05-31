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

import java.io.File;
import java.security.Policy;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    public static Button btnSwitch;
    public static Button btnStopMeasure;

    public Button btnMesureHistory;
    public Button btnFollowPractice;
    public Button btnMesureGuide;

    public static boolean isFlashOn;
    private boolean hasFlash;
    private static final int CAMERA_PERMISSION = 1;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION = 2;
    public static Context myContext;
    int ColorHeart;

    private ImageView imgHeart;
    private static TextView txtHeartRate;

    private static SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static View image = null;
    private static long startTime;

    private static long[] arrSumRed;
    private static long[] arrTime;
    private static int arrIndex;

    private static boolean isMeasureFinished;

    private static float minInterval;
    private static float maxInterval;
    private static float intervalDeviationRate;

    private static double arrResult[][];
    private static int arrResultIndex;
    private static int peaks[];
    private static int peakIndex;

    private static long arrIntervalPeak[][];//contain red value of peaks in set which is checked fitness
    private static int arrIntervalPeakIndexs[];


    private static int recordTime;

    private static int startCalIndex;
    private static int endCalIndex;
    private static boolean isProcessingData;
    private static int heartBeat;
    private static int turn;

    private static boolean isCameraAvailable;
    private static boolean isMustRequest;

    private static final String DirName = "YourHeartRate";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSwitch = (Button) findViewById(R.id.btnStartMeasure);
        btnStopMeasure = (Button) findViewById(R.id.btnStopMeasure);
        btnMesureGuide = (Button) findViewById(R.id.btnMesureGuide);
        btnMesureHistory = (Button) findViewById(R.id.btnMesureHistory);


        imgHeart = (ImageView) findViewById(R.id.imgHeart);
        txtHeartRate = (TextView) findViewById(R.id.txtHeartRate);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        isFlashOn = false;



        minInterval = 400f;//max beat per second is 120
        maxInterval = 1500f;//min beat persecond is 40
        intervalDeviationRate = 0.05f;
        recordTime = 5000;//15s
        isMustRequest=false;
        //check Camera Permission__________________________________________________________
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION);
            isMustRequest=true;
        }else{
            isCameraAvailable=true;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_PERMISSION);
            Log.i("Request permisson Write","here");

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

        ColorHeart = 0;
        startTime = 0;
        arrSumRed = new long[3000];
        arrTime = new long[3000];
        arrResult = new double[30000][3];
        peaks = new int[256];

        arrIntervalPeak = new long[30000][256];
        arrIntervalPeakIndexs = new int[30000];

        isCameraAvailable=false;
        startCalIndex = 0;
        endCalIndex = -1;
        isProcessingData = false;
        heartBeat = -1;
        turn = 2;
        btnStopMeasure.setEnabled(false);


        btnMesureHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(myContext,MeasureHistoryWindow.class);
                startActivity(i);
            }
        });

        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isCameraAvailable||(!isMustRequest)){
                    arrIndex = -1;
                    isMeasureFinished = false;
                    peakIndex = 0;
                    arrResultIndex = 0;
                    arrIndex = -1;
                    isMeasureFinished = false;
                    startCalIndex = 0;
                    endCalIndex = -1;
                    isProcessingData = false;
                    heartBeat = -1;
                    turn = 2;

                    camera = Camera.open();
                    preview = (SurfaceView) findViewById(R.id.preview);
                    previewHolder = preview.getHolder();
                    previewHolder.addCallback(surfaceCallback);
                    previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

                    try {
                        camera.setPreviewDisplay(previewHolder);
                        camera.setPreviewCallback(previewCallback);
                    } catch (Throwable t) {
                        Log.e("Error", "Exception in setPreviewDisplay()", t);
                        finish();
                    }



                    ColorHeart = 0;
                    btnSwitch.setEnabled(false);
                    btnSwitch.setText("Đang đo...");
                    txtHeartRate.setText("0");

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
                    btnStopMeasure.setEnabled(true);
                }
            }
        });

        btnStopMeasure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSwitch.setEnabled(true);
                btnSwitch.setText("Bắt đầu đo");
                txtHeartRate.setText("0");
                isMeasureFinished=true;
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
                btnStopMeasure.setEnabled(false);
            }
        });

        btnMesureGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(myContext,GuideWindow.class);
                startActivity(i);
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
                if( grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.i("Camera request sucess","true");
                    //camera = Camera.open();
                    isCameraAvailable=true;
                }else{
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
                }


                break;
            }
            case WRITE_EXTERNAL_STORAGE_PERMISSION: {
                if ( grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    FileUtility.createFolderIfNotExist("YourHeartRate");
                    FileUtility.writeFile("YourHeartRate","MeasureHistory","ab\ncd\nef");
                    FileUtility.readMeasureHistory("YourHeartRate","MeasureHistory");
                } else {
                    AlertDialog alert = new AlertDialog.Builder(MainActivity.this)
                            .create();
                    alert.setTitle("Error");
                    alert.setMessage("Request 'Write external storage'  Permission fail!");
                    alert.setButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // closing the application
                            finish();
                        }
                    });
                    alert.show();
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
//        camera = Camera.open();
        // startTime = System.currentTimeMillis();
    }

    @Override
    public void onPause() {
        super.onPause();

        if(isFlashOn){
            btnSwitch.setEnabled(true);
            btnSwitch.setText("Bắt đầu đo");
            txtHeartRate.setText("0");
            isMeasureFinished=true;
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
            btnStopMeasure.setEnabled(false);
        }

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

            if (!isMeasureFinished) {


                if (curTime - startTime > 1000 && isMeasureFinished == false) {
                    arrIndex++;
                    arrTime[arrIndex] = curTime;
                    arrSumRed[arrIndex] = ImageProcessing.decodeYUV420SPtoRedSum(data.clone(), height, width);
                    Log.i("arrSumRed:", arrSumRed[arrIndex] + ";" + (curTime % 100000) * 1.0 / 1000);
                }

                if ((curTime - startTime) > (1500 + 1500 * turn) && isProcessingData == false) {
                    endCalIndex = Find3SecondsIndex(startCalIndex);
                    heartBeat = MeasureHeartBeat();

                    if((1500+1500*turn)>=30000){
                        isFlashOn=false;
                        isMeasureFinished = true;
                        btnSwitch.setEnabled(true);
                        btnSwitch.setText("Bắt đầu đo");
                        camera.setPreviewCallback(null);
                        camera.stopPreview();
                        camera.release();
                        camera = null;
                        btnStopMeasure.setEnabled(false);

                        AlertDialog alert = new AlertDialog.Builder(myContext)
                                .create();
                        alert.setTitle("Vui lòng đo lại");
                        alert.setMessage("Thời gian đo vượt quá 30s.\n Vui lòng giữ yên ngón tay, không đè mạnh và đo lại !");
                        alert.setButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // closing the application

                            }
                        });
                        alert.show();
                    }
                    if (heartBeat == -1) {
                        turn++;
                        isProcessingData = false;
                        startCalIndex=FindFitSecondsIndex(startCalIndex,800,arrIndex);
                        Log.i("heartBeat","-1");
                    } else {
                        isMeasureFinished = true;
                        btnSwitch.setEnabled(true);
                        btnSwitch.setText("Bắt đầu đo");
                        txtHeartRate.setText(""+heartBeat);
                        camera.setPreviewCallback(null);
                        camera.stopPreview();
                        camera.release();
                        camera = null;
                        btnStopMeasure.setEnabled(false);
                        isFlashOn=false;
                        //FileUtility.writeFile("YourHeartRate","temp","1\n2aab\naa");
                        //FileUtility.readMeasureHistory("YourHeartRate","temp");

                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                        String strDate = sdf.format(c.getTime());

                        int judge=1;
                        String judgeT="";
                        if(heartBeat<50){
                            judge=1;
                            judgeT="Nếu bạn là vận động viên, thì tim bạn rất khỏe!\nNếu bạn không phải là vận động viên ,\n hãy đến phòng khám để kiểm tra lại sức khỏe tim!";
                        }
                        if(heartBeat>=50 &&heartBeat<=110){
                            judge=2;
                            judgeT="Sức khỏe tim bạn rất tốt!";
                        }
                        if(heartBeat>110 &&heartBeat<=120){
                            judge=3;
                            judgeT="Nhịp tim/phút bạn khá cao,\nbạn nên đến phòng khám để kiểm tra lại!";
                        }
                        if(heartBeat>120){
                            judge=4;
                            judgeT="Nhịp tim/phút bạn quá cao,\nbạn cần đến phòng khám kiểm tra sức khỏe tim gấp!";
                        }
                        final MeasureResult temp=new MeasureResult(strDate,heartBeat+"",judge+"");


                        AlertDialog.Builder builder = new AlertDialog.Builder(myContext);
                        builder.setMessage(judgeT)
                                .setPositiveButton("Không lưu", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                    }
                                })
                                .setNegativeButton("Lưu", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        FileUtility.writeMeasureHistory(DirName,"MeasureHistory",temp);
                                    }
                                });
                        // Create the AlertDialog object and return it
                         builder.create();
                         builder.show();


                        Log.i("heartBeat",heartBeat+"");
                    }

                }

            }//end if isMeasureFinished

        }

    };

    private static int Find3SecondsIndex(int startCalId) {
        int i = startCalId;

        while (arrTime[i] - arrTime[startCalId] < 3000) {
            i++;
        }

        return i;
    }

    private static int FindFitSecondsIndex(int startCalId, float interval,int maxIndex) {
        int i = startCalId;
        while (arrTime[i] - arrTime[startCalId] < interval) {
            i++;
            if(i>maxIndex){
                return -1;
            }
        }

        return i;
    }

    private static int[] Find3HighestLocalPeaks(int startCalId, int endCalId) {
        int highestLocalPeak[] = new int[3];
        int localPeaks[]=new int[endCalId-startCalId];
        int localPeaksIndex=-1;
        for (int i = startCalId+1; i < endCalId; i++) {

            if ((arrSumRed[i] > arrSumRed[i - 1] && arrSumRed[i] >= arrSumRed[i + 1])
                    || (arrSumRed[i] >= arrSumRed[i - 1] && arrSumRed[i] > arrSumRed[i + 1])) {
                localPeaksIndex++;
                localPeaks[localPeaksIndex] = i;
            }

        }

        if(localPeaksIndex<2){
            highestLocalPeak[0]=-1;
            return highestLocalPeak;
        }
        int idPeak1, idPeak2, idPeak3;
        idPeak1 = localPeaks[0];
        idPeak2 = -1;
        idPeak3 = -1;

        for (int i = 0; i <= localPeaksIndex; i++) {
            if (arrSumRed[localPeaks[i]] > arrSumRed[idPeak1]) {
                idPeak1 = localPeaks[i];
            }
        }

        for (int i = 0; i <= localPeaksIndex; i++) {

            if (idPeak2 == -1) {
                if (arrSumRed[localPeaks[i]] <= arrSumRed[idPeak1] && idPeak1 != localPeaks[i]) {
                    idPeak2 = localPeaks[i];
                }
            } else {
                if (localPeaks[i] != idPeak1 && arrSumRed[localPeaks[i]] > arrSumRed[idPeak2]) {
                    idPeak2 = localPeaks[i];
                }
            }

        }//end for

        for (int i = 0; i <= localPeaksIndex; i++) {

            if (idPeak3 == -1) {
                if (arrSumRed[localPeaks[i]] <= arrSumRed[idPeak2] && idPeak2 != localPeaks[i] && localPeaks[i] != idPeak1) {
                    idPeak3 = localPeaks[i];
                }
            } else {
                if (localPeaks[i] != idPeak1 && localPeaks[i] != idPeak2 && arrSumRed[localPeaks[i]] > arrSumRed[idPeak3]) {
                    idPeak3 = localPeaks[i];
                }
            }

        }//end for


        highestLocalPeak[0] = idPeak1;
        highestLocalPeak[1] = idPeak2;
        highestLocalPeak[2] = idPeak3;

        for (int i = 0; i <= 1; i++) {
            for (int j = i + 1; j <= 2; j++) {
                if (highestLocalPeak[j] < highestLocalPeak[i]) {
                    int temp = highestLocalPeak[j];
                    highestLocalPeak[j] = highestLocalPeak[i];
                    highestLocalPeak[i] = temp;
                }
            }
        }

        return highestLocalPeak;
    }
    private static int[] Find2LowestLocalPeaks(int peak1,int peak2,int peak3) {
        int result[]=new int[2];
        result[0]=peak1;
        for(int i=peak1;i<=peak2;i++){
            if(arrSumRed[result[0]]>arrSumRed[i]){
                result[0]=i;
            }
        }

        result[1]=peak2;
        for(int i=peak2;i<=peak3;i++){
            if(arrSumRed[result[1]]>arrSumRed[i]){
                result[1]=i;
            }
        }
        return result;
    }

    private static int FindHighestGlobal3sPeak() {
        int highestPeak = startCalIndex;
        for (int i = startCalIndex; i <= endCalIndex; i++) {
            if (arrSumRed[highestPeak] < arrSumRed[i]) {
                highestPeak = i;
            }
        }
        return highestPeak;
    }

    private static int FindLowestGlobal3sPeak() {
        int lowestPeak = startCalIndex;
        for (int i = startCalIndex; i <= endCalIndex; i++) {
            if (arrSumRed[lowestPeak] > arrSumRed[i]) {
                lowestPeak = i;
            }
        }
        return lowestPeak;
    }

    private static int FindLowestLocalPeak(int startCalId, int endCalId) {
        int lowestPeak = startCalId;
        for (int i = startCalId; i <= endCalId; i++) {
            if (arrSumRed[lowestPeak] > arrSumRed[i]) {
                lowestPeak = i;
            }
        }
        return lowestPeak;
    }

    private static int FindHighestLocalPeak(int startCalId, int endCalId) {
        int highestPeak = startCalId;
        for (int i = startCalId; i <= endCalId; i++) {
            if (arrSumRed[highestPeak] < arrSumRed[i]) {
                highestPeak = i;
            }
        }
        return highestPeak;
    }

    private static boolean CheckNearlyAsTall(long height1, long height2, long height3) {
        long minHeight = height1;
        if (minHeight > height2) {
            minHeight = height2;
        }
        if (minHeight > height3) {
            minHeight = height3;
        }

        if (((height1 <= minHeight * 1.4) && (height3 <= minHeight * 1.4) && (height2 <= minHeight * 1.4))) {
            return true;
        }
        return false;
    }

    private static int CheckConditionOf3HighestLocalPeaks(int highestPeaks[]) {
        long interval1 = arrTime[highestPeaks[1]] - arrTime[highestPeaks[0]];
        long interval2 = arrTime[highestPeaks[2]] - arrTime[highestPeaks[1]];
        //check interval in [0.4s,1,5s]
        if (!(interval1 >= 400 && interval1 <= 1500
                && interval2 >= 400 && interval2 <= 1500)) {
            Log.i("CheckError: ","out of [0.4,1.5]");
            return -1;
        }

        //Check if the distance between them is equal or not
        if(interval1<=interval2){
            if( !(interval1>=(interval2-100)) ){
                Log.i("CheckError: ","distance between peaks isn't equal--" +interval1+";"+interval2+";"+highestPeaks[0]);
                return -1;
            }
        }
        if(interval2<=interval1){
            if( !(interval2>=(interval1-100)) ){
                Log.i("CheckError: ","distance between peaks isn't equal--"+interval1+";"+interval2+";"+highestPeaks[0]);
                return -1;
            }
        }
        //check if they are nearly as tall
        int lowestLocalPeak = FindLowestLocalPeak(highestPeaks[0], highestPeaks[2]);
        long height1 = arrSumRed[highestPeaks[0]] - arrSumRed[lowestLocalPeak];
        long height2 = arrSumRed[highestPeaks[1]] - arrSumRed[lowestLocalPeak];
        long height3 = arrSumRed[highestPeaks[2]] - arrSumRed[lowestLocalPeak];
        if (!CheckNearlyAsTall(height1, height2, height3)) {
            Log.i("CheckError: ","Peaks aren't nearly as tall--"+interval1+";"+height1+";"+height2+";"+height3);
            return -1;
        }

        //check exception : local minimum isn't close to global minimum
        long compareHeight=arrSumRed[FindHighestLocalPeak(highestPeaks[0],highestPeaks[2])] -arrSumRed[FindLowestGlobal3sPeak()];
        int lowestLocalPeaks[]=Find2LowestLocalPeaks(highestPeaks[0],highestPeaks[1],highestPeaks[2]);
        long localMinHeight1=arrSumRed[lowestLocalPeaks[0]]-arrSumRed[FindLowestGlobal3sPeak()];
        long localMinHeight2=arrSumRed[lowestLocalPeaks[1]]-arrSumRed[FindLowestGlobal3sPeak()];
        if( !(localMinHeight1<=0.4*compareHeight && localMinHeight2<=0.4*compareHeight) ){
            Log.i("CheckError: ","local minimum isn't close to global minimum--"+interval1+";"+highestPeaks[0]);
            return -1;
        }

        //check exception : local max isn't close to global max
        compareHeight=arrSumRed[FindHighestGlobal3sPeak()] -arrSumRed[lowestLocalPeaks[0]];
        long localMaxHeight1=arrSumRed[highestPeaks[0]]-arrSumRed[lowestLocalPeaks[0]];
        if( localMaxHeight1<= compareHeight*0.5){
            Log.i("CheckError: ","local max isn't close to global max--"+interval1+";"+highestPeaks[0]);
            return -1;
        }

        int heartBeat = (int) (60000f / ((interval1 + interval2) * 1.0 / 2));
        Log.i("CheckError: ","None; "+interval1+";"+interval2+";"+heartBeat);
        Log.i("CheckError: ","peak; "+highestPeaks[0]+";"+highestPeaks[1]+";"+highestPeaks[2]
                +arrSumRed[highestPeaks[0]]+";"+arrSumRed[highestPeaks[1]]+";"+arrSumRed[highestPeaks[2]]);
        return heartBeat;
    }

    private static int MeasureHeartBeat() {
        isProcessingData = true;
        int heartBeat = -1;

        float interval=800;//0.8s
        boolean hasResult=false;
        while (!hasResult) {
            int endCalId=FindFitSecondsIndex(startCalIndex,interval,endCalIndex);

            //--------------------------------------------------------------
            if(endCalId==-1){
                hasResult=true;//3s out
            }else{
                int startCalId=startCalIndex;
                boolean stillLoop=true;
                while(stillLoop){
                    int highestPeaks[]=Find3HighestLocalPeaks(startCalId,endCalId);


                    if(highestPeaks[0]!=-1)
                    {
                        int result=CheckConditionOf3HighestLocalPeaks(highestPeaks);
                        if(result!=-1)
                        {
                            heartBeat=result;
                            hasResult=true;
                            break;
                        }

                    }
                    int previousEndCalId=endCalId;
                    endCalId=FindFitSecondsIndex(endCalId,interval,endCalIndex);

                    if(endCalId==-1){
                        stillLoop=false;
                    }else{
                        startCalId=previousEndCalId;
                    }

                }//end while 2

            }
            //--------------------------------------------------------------

            interval+=200;
        }//end while 1
        return heartBeat;
    }


    private static SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i("wow", "da o dau");

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

package com.example.measureheartrate;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public final class FileUtility {
    private FileUtility(){

    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
    public static boolean createFolderIfNotExist(String dirName){
        boolean result=true;
        File dir = new File(Environment.getExternalStorageDirectory() + "/"+dirName);

        if( !(dir.exists() && dir.isDirectory()) ) {
            if( !(dir.mkdir()) ){
                result=false;
            }
        }
        if(dir.exists()){
            Log.i("FolderExisted","true");
        }
        return result;
    }

    public static boolean checkFileExist(String dirName,String fileName){
        File file=new File(Environment.getExternalStorageDirectory() + "/"+dirName+"/"+fileName);
        if(file.exists()){
            Log.i("FileStatus: ", fileName+" exist");
            return true;
        }
        return false;
    }


    public static void writeFile(String dirName,String fileName,String text){
        createFolderIfNotExist(dirName);
        File file=new File(Environment.getExternalStorageDirectory() + "/"+dirName+"/"+fileName);
        if(!checkFileExist(dirName,fileName)){
            try{
                file.createNewFile();
            }catch (IOException e){
                Log.i("Error CreateFile",e.getMessage());
            }

        }

        try{
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(text.getBytes());
            fos.close();
        }catch (IOException e){
            Log.i("Error Write File ",e.getMessage());
        }

    }

    public static ArrayList<MeasureResult> readMeasureHistory(String dirName, String fileName){
        ArrayList<MeasureResult> arrMeasureResult=new ArrayList<MeasureResult>();
        File file=new File(Environment.getExternalStorageDirectory() + "/"+dirName+"/"+fileName);
        if(checkFileExist( dirName,  fileName))
        {

            String text = "";
            try {
                FileInputStream fis = new FileInputStream(file);
                int c;
                while ((c = fis.read()) != -1)
                {
                    text += String.valueOf((char) c);
                }
                fis.close();
                Log.i("ReadText",text);

                String rawText[]=text.split("\n");
                for(int i=0;i<Integer.parseInt(rawText[0]);i++)
                {
                    arrMeasureResult.add(new MeasureResult(rawText[1+i*3],rawText[2+i*3],rawText[3+i*3]));
                }
            } catch (IOException e) {
                Log.i("Error Read File",e.getMessage());
            }

        }
        return arrMeasureResult;
    }

    public static void writeMeasureHistory(String dirName, String fileName,MeasureResult mr){
        if(checkFileExist( dirName,  fileName))
        {
            ArrayList<MeasureResult> arrMr;
            arrMr= readMeasureHistory( dirName,  fileName);
            int oldLength=arrMr.size();
            oldLength++;
            String newText=""+oldLength;

            for(int i=0;i<oldLength-1;i++){
                newText+=("\n"+arrMr.get(i).getTime()+"\n"+arrMr.get(i).getHeartBeat()+"\n"+arrMr.get(i).getJudgment());
            }
            newText+=("\n"+mr.getTime()+"\n"+mr.getHeartBeat()+"\n"+mr.getJudgment());
            writeFile(dirName,fileName,newText);
        }else{
            String text="1\n"+mr.getTime()+"\n"+mr.getHeartBeat()+"\n"+mr.getJudgment();
            writeFile(dirName,fileName,text);
        }
    }

}

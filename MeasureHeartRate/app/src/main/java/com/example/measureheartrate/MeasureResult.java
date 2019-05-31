package com.example.measureheartrate;

public class MeasureResult {
    private String time;
    private String heartBeat;
    private String judgment;

    public MeasureResult(String time,String heartBeat,String judgment){
        this.time=time;
        this.heartBeat=heartBeat;
        this.judgment=judgment;
    }

    public String getTime(){
        return time;
    }
    public String getHeartBeat(){
        return heartBeat;
    }
    public String getJudgment(){
        return judgment;
    }
}

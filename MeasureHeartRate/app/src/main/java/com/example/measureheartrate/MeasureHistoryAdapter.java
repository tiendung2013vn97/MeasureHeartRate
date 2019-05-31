package com.example.measureheartrate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MeasureHistoryAdapter extends ArrayAdapter<MeasureResult> {
    Context context;
    List<MeasureResult> mResults;

    public MeasureHistoryAdapter(Context context,int layoutToBeInflated,
                                 ArrayList<MeasureResult> mResults)
    {
        super(context,layoutToBeInflated,mResults);
        this.context=context;
        this.mResults=mResults;
    }

    @Override
    public View getView(int position,  View convertView,  ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView==null){
            convertView= LayoutInflater.from(context).inflate(R.layout.measure_history_item, parent, false);
            viewHolder=new ViewHolder();
            viewHolder.tvTime=(TextView)convertView.findViewById(R.id.timeContent);
            viewHolder.tvHeartBeat=(TextView)convertView.findViewById(R.id.heartBeatContent);
            viewHolder.tvJudgment=(TextView)convertView.findViewById(R.id.judgmentContent);
            viewHolder.tvIdRow=(TextView)convertView.findViewById(R.id.idRow);

            convertView.setTag(viewHolder);
        }else{
            viewHolder=(ViewHolder)convertView.getTag();
        }
        MeasureResult mr=mResults.get(position);
        viewHolder.tvTime.setText(mr.getTime());

        viewHolder.tvHeartBeat.setText(mr.getHeartBeat());
        viewHolder.tvIdRow.setText(position+1+"");


        int judge=Integer.parseInt(mr.getJudgment()) ;

        if(judge==1){
            viewHolder.tvJudgment.setText("Nếu bạn là vận động viên, thì tim bạn rất khỏe!\nNếu bạn không phải là vận động viên ,\n hãy đến phòng khám để kiểm tra lại sức khỏe tim!");
        }
        if(judge==2){
            viewHolder.tvJudgment.setText("Sức khỏe tim bạn rất tốt!");
        }
        if(judge==3){
            viewHolder.tvJudgment.setText("Nhịp tim/phút bạn khá cao,\nbạn nên đến phòng khám để kiểm tra lại!");
        }
        if(judge==4){
            viewHolder.tvJudgment.setText("Nhịp tim/phút bạn quá cao,\nbạn cần đến phòng khám kiểm tra sức khỏe tim gấp!");
        }
        return convertView;
    }

    public class ViewHolder{
        TextView tvTime,tvHeartBeat,tvJudgment,tvIdRow;
    }
}


package com.example.checkdatabase.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.checkdatabase.R;
import com.example.checkdatabase.activity.ReceiverActivity;

import java.util.List;

public class ReCreateAdapter extends BaseAdapter {

    private String[] deviceNameArry;
    private Context mContext;
    private TextView mDeviceNameTv;

    public ReCreateAdapter(String[] list, ReceiverActivity context) {
        this.mContext = context;
        this.deviceNameArry =list;
    }


    @Override
    public int getCount() {
        return deviceNameArry.length;
    }

    @Override
    public Object getItem(int position) {
        return deviceNameArry[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //wifi界面不停变化
        convertView= LayoutInflater.from(mContext).inflate(R.layout.listview_layout,parent,false);
        mDeviceNameTv = convertView.findViewById(R.id.DeviceName_tv);
        mDeviceNameTv.setText(deviceNameArry[position]);

        return convertView;
    }
}

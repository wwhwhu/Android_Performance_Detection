package com.wwh.updatemonitor;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CustomAdapter extends BaseAdapter {
    private Context context;
    private List<Map.Entry<String, String>> dataList;
    private boolean isCPU;

    public CustomAdapter(Context context, List<Map.Entry<String, String>> data, boolean isCPU) {
        this.context = context;
        this.dataList = data;
        this.isCPU = isCPU;
        // 使用Collections.sort对dataList进行排序
        Collections.sort(dataList, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> entry1, Map.Entry<String, String> entry2) {
                // 将String转换为数值进行比较
                double value1 = Double.parseDouble(entry1.getValue());
                double value2 = Double.parseDouble(entry2.getValue());

                // 根据数值大小进行比较
                return Double.compare(value2, value1);
            }
        });
        // 如果你需要覆盖原始列表，可以将排序后的数据重新赋值给原始列表
        this.dataList = new ArrayList<>(dataList);
        for (Map.Entry<String, String> entry : this.dataList) {
            String secondStringValue = entry.getValue();
            Log.d("SHOWSHOW", secondStringValue);
        }
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.custom_list_item, null);
        }
        Map.Entry<String, String> entry = dataList.get(position);
        TextView appNameTextView = convertView.findViewById(R.id.appNameTextView);
        TextView usageTextView = convertView.findViewById(R.id.usageTextView);
        appNameTextView.setText(entry.getKey());
        DecimalFormat decimalFormat = new DecimalFormat("#0.0");
        if(isCPU)
            usageTextView.setText(decimalFormat.format(Double.parseDouble(entry.getValue())/8.0));
        else
            usageTextView.setText(entry.getValue());
        return convertView;
    }
}

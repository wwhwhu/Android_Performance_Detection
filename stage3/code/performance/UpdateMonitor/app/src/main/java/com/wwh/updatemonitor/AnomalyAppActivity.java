package com.wwh.updatemonitor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnomalyAppActivity extends AppCompatActivity {

    private ListView cpuListView;
    private ListView memoryListView;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.anomaly_app);

        cpuListView = findViewById(R.id.cpuListView);
        memoryListView = findViewById(R.id.memoryListView);
        backButton = findViewById(R.id.backButton);

        Intent intent = getIntent();
        // 从上一页获取两个Map数据，你需要根据实际情况获取数据
        Map<String, String> cpuData = new HashMap<>();
        Map<String, String> memoryData = new HashMap<>();
        int cpu = (Integer) intent.getSerializableExtra("cpu");
        int mem = (Integer) intent.getSerializableExtra("mem");
        if(cpu!=0)
        // if(true)
        {
            cpuData = (Map<String, String>) intent.getSerializableExtra("cpuData");
        }else{
            Toast.makeText(cpuListView.getContext(), "当前没有CPU异常", Toast.LENGTH_SHORT).show();
        }
        if(mem!=0)
        // if(true)
        {
            memoryData = (Map<String, String>) intent.getSerializableExtra("memoryData");
        }else {
            Toast.makeText(memoryListView.getContext(), "当前没有内存异常", Toast.LENGTH_SHORT).show();
        }

        List<Map.Entry<String, String>> cpuDataList = new ArrayList<>(cpuData.entrySet());
        CustomAdapter cpuAdapter = new CustomAdapter(this, cpuDataList, true);

        List<Map.Entry<String, String>> memDataList = new ArrayList<>(memoryData.entrySet());
        CustomAdapter memoryAdapter = new CustomAdapter(this, memDataList,false);

        // 设置ListView适配器
        cpuListView.setAdapter(cpuAdapter);
        memoryListView.setAdapter(memoryAdapter);

        backButton.setOnClickListener(v -> finish()); // 返回上一页按钮
    }
}

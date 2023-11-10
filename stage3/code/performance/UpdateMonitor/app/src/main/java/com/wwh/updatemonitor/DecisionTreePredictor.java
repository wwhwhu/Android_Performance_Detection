package com.wwh.updatemonitor;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.icu.text.AlphabeticIndex;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.android.EvaluatorUtil;
import org.jpmml.evaluator.Computable;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.TargetField;
import org.xml.sax.SAXException;

public class DecisionTreePredictor {
    private Context mContext;

    public DecisionTreePredictor(Context context) {
        mContext = context;
    }

    public Evaluator loadPmml(String pak, int a) {
        AssetManager assetManager = mContext.getAssets();
        InputStream inputStream = null;
        //if(pak.equals("com.taobao.taobao"))
        try {
            if (pak.equals("com.taobao.taobao")) {
                if (a == 1)
                    inputStream = assetManager.open("taobaocpu.pmml.ser");
                if (a == 2)
                    inputStream = assetManager.open("taobaogpu.pmml.ser");
                if (a == 3)
                    inputStream = assetManager.open("taobaomem.pmml.ser");
            } else if (pak.equals("com.ss.android.ugc.aweme")) {
                if (a == 1)
                    inputStream = assetManager.open("douyincpu.pmml.ser");
                if (a == 2)
                    inputStream = assetManager.open("douyingpu.pmml.ser");
                if (a == 3)
                    inputStream = assetManager.open("douyinmem.pmml.ser");
            } else if (pak.equals("com.miHoYo.ys.mi") || pak.equals("com.miHoYo.Yuanshen")) {
                if (a == 1)
                    inputStream = assetManager.open("yuanshencpu.pmml.ser");
                if (a == 2)
                    inputStream = assetManager.open("yuanshengpu.pmml.ser");
                if (a == 3)
                    inputStream = assetManager.open("yuanshenmem.pmml.ser");
            } else {
                if (a == 1)
                    inputStream = assetManager.open("entirecpu.pmml.ser");
                if (a == 2)
                    inputStream = assetManager.open("entiregpu.pmml.ser");
                if (a == 3)
                    inputStream = assetManager.open("entiremem.pmml.ser");
            }
            Log.d("model", "Success: return model");
            return EvaluatorUtil.createEvaluator(inputStream);
        } catch (Exception e) {
            Log.d("model", "Error: return model null");
            e.printStackTrace();
            return null;
        }
    }

    public int predict(String pak, Evaluator evaluator, Map parsedResults, int a) {
        Map<String, Double> data = new HashMap<String, Double>();
        if (parsedResults.get("FrontAppCpuUsage") == null) {
            Log.d("model", "数据未采集");
            return -1;
        }
        if (pak.equals("com.taobao.taobao") || pak.equals("com.ss.android.ugc.aweme") || pak.equals("com.miHoYo.ys.mi") || pak.equals("com.miHoYo.Yuanshen")) {
            data.put("FrontAppCpuUsage", Double.valueOf((String) parsedResults.get("FrontAppCpuUsage")));
            data.put("usedNativeMemPercent", Double.valueOf((String) parsedResults.get("usedNativeMemPercent")));
            data.put("FrontAPPSystemMemPercent", Double.valueOf((String) parsedResults.get("FrontAPPSystemMem")) / Double.valueOf((String) parsedResults.get("MemTotal")));
            data.put("FrontAPPJavaHeapPercent", Double.valueOf((String) parsedResults.get("FrontAPPJavaHeap")) / Double.valueOf((String) parsedResults.get("MemTotal")));
            data.put("FrontAPPNativeHeapPercent", Double.valueOf((String) parsedResults.get("FrontAPPNativeHeap")) / Double.valueOf((String) parsedResults.get("MemTotal")));
            data.put("FrontAPPCodeMemPercent", Double.valueOf((String) parsedResults.get("FrontAPPCodeMem")) / Double.valueOf((String) parsedResults.get("MemTotal")));
            data.put("FrontAPPStackMemPercent", Double.valueOf((String) parsedResults.get("FrontAPPStackMem")) / Double.valueOf((String) parsedResults.get("MemTotal")));
            data.put("FrontAPPGraphicsMemPercent", Double.valueOf((String) parsedResults.get("FrontAPPGraphicsMem")) / Double.valueOf((String) parsedResults.get("MemTotal")));
            data.put("FrontAPPPrivateOtherMemPercent", Double.valueOf((String) parsedResults.get("FrontAPPPrivateOtherMem")) / Double.valueOf((String) parsedResults.get("MemTotal")));
            data.put("tempCPU0", Double.valueOf((String) parsedResults.get("tempCPU0")));
            data.put("tempCPU1", Double.valueOf((String) parsedResults.get("tempCPU1")));
            data.put("tempCPU2", Double.valueOf((String) parsedResults.get("tempCPU2")));
            data.put("tempCPU3", Double.valueOf((String) parsedResults.get("tempCPU3")));
            data.put("tempCPU4", Double.valueOf((String) parsedResults.get("tempCPU4")));
            data.put("tempCPU5", Double.valueOf((String) parsedResults.get("tempCPU5")));
            data.put("tempCPU6", Double.valueOf((String) parsedResults.get("tempCPU6")));
            data.put("tempCPU7", Double.valueOf((String) parsedResults.get("tempCPU7")));
            data.put("batteryTemp", Double.valueOf((String) parsedResults.get("batteryTemp")));
            data.put("thermalStatus", Double.valueOf((String) parsedResults.get("thermalStatus")));
            data.put("gpuUsage", Double.valueOf((String) parsedResults.get("gpuUsage")));
            data.put("GpuFreq", Double.valueOf((String) parsedResults.get("GpuFreq")));
            data.put("cpu0Freq", Double.valueOf((String) parsedResults.get("cpu0Freq")));
            data.put("cpu1Freq", Double.valueOf((String) parsedResults.get("cpu1Freq")));
            data.put("cpu2Freq", Double.valueOf((String) parsedResults.get("cpu2Freq")));
            data.put("cpu3Freq", Double.valueOf((String) parsedResults.get("cpu3Freq")));
            data.put("cpu4Freq", Double.valueOf((String) parsedResults.get("cpu4Freq")));
            data.put("cpu5Freq", Double.valueOf((String) parsedResults.get("cpu5Freq")));
            data.put("cpu6Freq", Double.valueOf((String) parsedResults.get("cpu6Freq")));
            data.put("cpu7Freq", Double.valueOf((String) parsedResults.get("cpu7Freq")));
            if(a==3)
            {
                data.put("MemAvailable", Double.valueOf((String) parsedResults.get("MemAvailable")) / Double.valueOf((String) parsedResults.get("MemTotal")));
                data.put("MemFree", Double.valueOf((String) parsedResults.get("MemFree")) / Double.valueOf((String) parsedResults.get("MemTotal")));
            }
        } else {
            data.put("tempCPU0", Double.valueOf((String) parsedResults.get("tempCPU0")));
            data.put("tempCPU1", Double.valueOf((String) parsedResults.get("tempCPU1")));
            data.put("tempCPU2", Double.valueOf((String) parsedResults.get("tempCPU2")));
            data.put("tempCPU3", Double.valueOf((String) parsedResults.get("tempCPU3")));
            data.put("tempCPU4", Double.valueOf((String) parsedResults.get("tempCPU4")));
            data.put("tempCPU5", Double.valueOf((String) parsedResults.get("tempCPU5")));
            data.put("tempCPU6", Double.valueOf((String) parsedResults.get("tempCPU6")));
            data.put("tempCPU7", Double.valueOf((String) parsedResults.get("tempCPU7")));
            data.put("batteryTemp", Double.valueOf((String) parsedResults.get("batteryTemp")));
            data.put("thermalStatus", Double.valueOf((String) parsedResults.get("thermalStatus")));
            data.put("gpuUsage", Double.valueOf((String) parsedResults.get("gpuUsage")));
            data.put("GpuFreq", Double.valueOf((String) parsedResults.get("GpuFreq")));
            data.put("cpu0Freq", Double.valueOf((String) parsedResults.get("cpu0Freq")));
            data.put("cpu1Freq", Double.valueOf((String) parsedResults.get("cpu1Freq")));
            data.put("cpu2Freq", Double.valueOf((String) parsedResults.get("cpu2Freq")));
            data.put("cpu3Freq", Double.valueOf((String) parsedResults.get("cpu3Freq")));
            data.put("cpu4Freq", Double.valueOf((String) parsedResults.get("cpu4Freq")));
            data.put("cpu5Freq", Double.valueOf((String) parsedResults.get("cpu5Freq")));
            data.put("cpu6Freq", Double.valueOf((String) parsedResults.get("cpu6Freq")));
            data.put("cpu7Freq", Double.valueOf((String) parsedResults.get("cpu7Freq")));
            data.put("MemAvailable", Double.valueOf((String) parsedResults.get("MemAvailable")) / Double.valueOf((String) parsedResults.get("MemTotal")));
            data.put("MemFree", Double.valueOf((String) parsedResults.get("MemFree")) / Double.valueOf((String) parsedResults.get("MemTotal")));
            data.put("cpuUsage", Double.valueOf((String) parsedResults.get("cpuUsage")));
        }
        List<InputField> inputFields = evaluator.getInputFields();
        //过模型的原始特征，从画像中获取数据，作为模型输入
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<FieldName, FieldValue>();
        for (InputField inputField : inputFields) {
            FieldName inputFieldName = inputField.getName();
            Object rawValue = data.get(inputFieldName.getValue());
            FieldValue inputFieldValue = inputField.prepare(rawValue);
            arguments.put(inputFieldName, inputFieldValue);
        }

        Map<FieldName, ?> results = evaluator.evaluate(arguments);
        List<TargetField> targetFields = evaluator.getTargetFields();

        TargetField targetField = targetFields.get(0);
        FieldName targetFieldName = targetField.getName();

        Object targetFieldValue = results.get(targetFieldName);
        Log.d("model", "target: " + targetFieldName.getValue().toString() + " value: " + targetFieldValue.toString());
        int primitiveValue = -1;
        if (targetFieldValue instanceof Computable) {
            Computable computable = (Computable) targetFieldValue;
            primitiveValue = (Integer) computable.getResult();
        }
        Log.d("model", "raw: " + String.valueOf(primitiveValue));
        primitiveValue = a == 1 ? (Double.valueOf((String) parsedResults.get("cpuUsage"))>= 0.7 ? 2 : primitiveValue) : primitiveValue;
        primitiveValue = a == 3 ? (Double.valueOf((String) parsedResults.get("MemFree")) / Double.valueOf((String) parsedResults.get("MemTotal")) >= 0.1 ? 0 : primitiveValue) : primitiveValue;
        // primitiveValue = a == 3 ? (Double.valueOf((String) parsedResults.get("MemAvailable")) / Double.valueOf((String) parsedResults.get("MemTotal")) < 0.1 ? 1 : primitiveValue) : primitiveValue;
        Log.d("model", "result: " + String.valueOf(primitiveValue));
        return primitiveValue;
    }
}

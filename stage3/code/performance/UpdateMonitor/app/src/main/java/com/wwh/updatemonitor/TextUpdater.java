package com.wwh.updatemonitor;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.Map;

public class TextUpdater {
    private TextView textView;
    private int buttonCounter = 0; // 记录按钮的点击次数
    private int res1 = 0;
    private int res2 = 0;
    private int res3 = 0;
    private Map<String, String> cpuMap;
    private Map<String, String> memMap;
    private Context context;
    private int cishu = 0;

    public TextUpdater(TextView textView, Context context) {
        this.textView = textView;
        this.context = context;
    }

    // 创建三个ArrayList
//    ArrayList<Map<String,String>> array_CPU = new ArrayList<>();
//    ArrayList<Map<String,String>> array_MEM = new ArrayList<>();

    // 自定义添加元素函数
//    public static void addElement(ArrayList<Map<String,String>> list, Map<String,String> element) {
//        if (list.size() < 10) {
//            // 当数组元素不足10个时，直接尾部添加
//            list.add(element);
//        } else {
//            // 当数组元素大于10个时，删除第一个元素，将其添加到最后
//            list.remove(0);
//            list.add(element);
//        }
//    }

    public void addNewText(int x1, int x2, int x3, Map<String, String> cpu, Map<String, String> mem, String s) {
        res1 = x1;
        res2 = x2;
        res3 = x3;
        String aa;
        String bb;
        String cc;
        aa = x1 == 0 ? "：CPU无异常" : "：CPU异常：";
        bb = x2 == 0 ? "，GPU无异常" : "，GPU异常：null";
        cc = x3 == 0 ? "，内存无异常" : "，内存异常：";
        if (x1 != 0)
            aa += maxAbnormal(cpu);
        if (x3 != 0)
            cc += maxAbnormal(mem);
        cishu++;
        Log.d("Model_Result", String.valueOf(cishu) + aa + bb + cc);
        this.cpuMap = cpu;
        this.memMap = mem;
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(textView.getText());
        // 添加新文本
        spannableStringBuilder.append(s);
        // if (false)
        if (x1 == 0 && x2 == 0 && x3 == 0) {
            spannableStringBuilder.append("  [当前未有异常APP]  ");
        } else {
            spannableStringBuilder.append("  [点击查看异常APP]  ");
            final int start = spannableStringBuilder.length() - "[点击查看异常APP]  ".length();
            final int end = spannableStringBuilder.length() - "  ".length();
            // 创建可点击的按钮
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    // 处理按钮点击事件
                    handleButtonClick(cpu, mem, x1, x2, x3);
                }
            };
            // 将按钮部分设置为可点击
            spannableStringBuilder.setSpan(clickableSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
//        // 获取SpannableStringBuilder的行数
//        int lineCount = getLineCount(spannableStringBuilder);
//        Log.d("Output", String.valueOf(lineCount));
//        // 如果行数大于12，删除第3行并添加新行
//        if (lineCount > 12) {
//            // 删除第3行
//            int start3 = getStartOfLine(spannableStringBuilder, 3);
//            int end3 = getEndOfLine(spannableStringBuilder, 3);
//            spannableStringBuilder.replace(start3, end3 + 1, "");
//        }
//        // 保留历史10条信息
//        addElement(array_CPU,cpuMap);
//        addElement(array_MEM,memMap);
        // 设置文本到 TextView
        textView.setText(spannableStringBuilder);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private String maxAbnormal(Map<String, String> getMostAbnormal) {
        String maxName = null;
        double maxScore = Double.MIN_VALUE;
        for (Map.Entry<String, String> entry : getMostAbnormal.entrySet()) {
            String name = entry.getKey();
            String scoreStr = entry.getValue();
            try {
                double score = Double.parseDouble(scoreStr);
                if (score > maxScore) {
                    maxScore = score;
                    maxName = name;
                }
            } catch (NumberFormatException e) {
                Log.e("MaxAbnormal", "无效的得分：" + scoreStr);
            }
        }
        return maxName;
    }

    private void handleButtonClick(Map<String, String> cpu, Map<String, String> mem, int res1, int res2, int res3) {
        if (res2 == 1 && res1 == 0 && res3 == 0)
            Toast.makeText(textView.getContext(), "只有GPU异常，无法定位APP", Toast.LENGTH_SHORT).show();
        else {
            if (res2 == 1)
                Toast.makeText(textView.getContext(), "存在GPU异常，但是无法定位APP", Toast.LENGTH_SHORT).show();
            // 创建一个Intent来跳转到界面B
            Intent intent = new Intent(this.context, AnomalyAppActivity.class);
            // 将这两个Map作为额外数据添加到Intent中
            intent.putExtra("cpuData", (Serializable) cpu);
            intent.putExtra("memoryData", (Serializable) mem);
            intent.putExtra("cpu", (Serializable) res1);
            intent.putExtra("gpu", (Serializable) res2);
            intent.putExtra("mem", (Serializable) res3);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // 启动界面B
            context.startActivity(intent);
        }
    }

    public void addNewText2(String s) {
        String newText = textView.getText() + s;
        textView.setText(newText);
    }

    // 获取SpannableStringBuilder的行数的方法
    private int getLineCount(SpannableStringBuilder spannableStringBuilder) {
        // 将SpannableStringBuilder的文本内容转换为字符串
        String text = spannableStringBuilder.toString();
        // 按换行符拆分文本为行
        String[] lines = text.split("\n");
        // 获取行数
        return lines.length;
    }

    // 获取指定行的起始位置的方法
    private int getStartOfLine(SpannableStringBuilder spannableStringBuilder, int num) {
        int start = 0;
        // 将SpannableStringBuilder的文本内容转换为字符串
        String text = spannableStringBuilder.toString();
        // 按换行符拆分文本为行
        String[] lines = text.split("\n");
        // 获取行数
        for (int i = 0; i < num - 1; i++) {
            start += lines[i].length() + 1; // 加1用于包括换行符
        }
        return start;
    }

    // 获取指定行的结束位置的方法
    private int getEndOfLine(SpannableStringBuilder spannableStringBuilder, int num) {
        int start = 0;
        // 将SpannableStringBuilder的文本内容转换为字符串
        String text = spannableStringBuilder.toString();
        // 按换行符拆分文本为行
        String[] lines = text.split("\n");
        // 获取行数
        for (int i = 0; i < num - 1; i++) {
            start += lines[i].length() + 1; // 加1用于包括换行符
        }
        return start + lines[num - 1].length();
    }
}

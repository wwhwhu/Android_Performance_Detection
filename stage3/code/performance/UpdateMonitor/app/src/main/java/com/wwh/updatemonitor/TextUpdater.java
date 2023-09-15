package com.wwh.updatemonitor;

import android.util.Log;
import android.widget.TextView;

public class TextUpdater {
    private TextView textView;
    public TextUpdater(TextView textView) {
        this.textView = textView;
    }
    public void addNewText(String s) {
        String newText = textView.getText()+s;
        int maxLines = 12;
        String[] lines = newText.split("\n");
        StringBuilder sb = new StringBuilder();
        // 如果newText的行数超过maxLines，则删除第三行
        if (lines.length > maxLines) {
            // 添加表头
            sb.append(lines[0]).append("\n").append(lines[1]);
            for (int i = 3; i < lines.length; i++) {
                    sb.append("\n").append(lines[i]);
            }
            newText = sb.toString();
        }
        textView.setText(newText);
    }
}

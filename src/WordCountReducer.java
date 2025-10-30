package com.stock.analysis;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.*;

public class WordCountReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
    
    private final IntWritable result = new IntWritable();
    private final Text outputKey = new Text();
    
    @Override
    public void reduce(Text key, Iterable<IntWritable> values, Context context) 
            throws IOException, InterruptedException {
        
        String keyStr = key.toString();
        System.out.println("Reducer received key: " + keyStr);
        
        String[] parts = keyStr.split("_", 2);
        if (parts.length != 2) {
            System.err.println("Invalid key format: " + keyStr);
            return;
        }
        
        String sentiment = parts[0];
        String word = parts[1];
        
        // 计算词频
        int sum = 0;
        for (IntWritable val : values) {
            sum += val.get();
        }
        
        System.out.println("Word: " + word + ", Sentiment: " + sentiment + ", Count: " + sum);
        
        // 直接输出 - 格式: 单词<TAB>次数<TAB>情感标签
        String output = word + "\t" + sum + "\t" + sentiment;
        outputKey.set(output);
        result.set(sum);
        
        System.out.println("Writing output: " + output);
        context.write(outputKey, result);
        System.out.println("Successfully wrote output for: " + output);
    }
    
    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        System.out.println("Reducer setup called");
    }
    
    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        System.out.println("Reducer cleanup called");
    }
}

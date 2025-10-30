package com.stock.analysis;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

public class TopKMapper extends Mapper<LongWritable, Text, Text, Text> {
    
    private Text sentimentKey = new Text();
    private Text wordCount = new Text();
    
    @Override
    public void map(LongWritable key, Text value, Context context) 
            throws IOException, InterruptedException {
        
        String line = value.toString();
        String[] parts = line.split("\t");
        if (parts.length < 2) return;
        
        String keyStr = parts[0]; // format: sentiment_word
        String count = parts[1];
        
        String[] keyParts = keyStr.split("_", 2);
        if (keyParts.length < 2) return;
        
        String sentiment = keyParts[0];
        String word = keyParts[1];
        
        sentimentKey.set(sentiment);
        wordCount.set(word + "\t" + count);
        
        context.write(sentimentKey, wordCount);
    }
}

package com.stock.analysis;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.*;

public class TopKReducer extends Reducer<Text, Text, Text, Text> {
    
    private static final int TOP_K = 100;
    
    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) 
            throws IOException, InterruptedException {
        
        // Use PriorityQueue to get top K words
        PriorityQueue<WordCount> minHeap = new PriorityQueue<>();
        
        for (Text value : values) {
            String[] parts = value.toString().split("\t");
            if (parts.length < 2) continue;
            
            String word = parts[0];
            int count = Integer.parseInt(parts[1]);
            
            WordCount wc = new WordCount(word, count);
            
            if (minHeap.size() < TOP_K) {
                minHeap.offer(wc);
            } else if (count > minHeap.peek().count) {
                minHeap.poll();
                minHeap.offer(wc);
            }
        }
        
        // Convert to list and sort in descending order
        List<WordCount> topWords = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            topWords.add(minHeap.poll());
        }
        Collections.sort(topWords, Collections.reverseOrder());
        
        // Output results
        String sentiment = key.toString();
        String sentimentName = sentiment.equals("1") ? "positive" : "negative";
        
        for (WordCount wc : topWords) {
            context.write(new Text(wc.word), new Text(wc.count + ""));
        }
    }
    
    private static class WordCount implements Comparable<WordCount> {
        String word;
        int count;
        
        WordCount(String word, int count) {
            this.word = word;
            this.count = count;
        }
        
        @Override
        public int compareTo(WordCount other) {
            return Integer.compare(this.count, other.count);
        }
    }
}

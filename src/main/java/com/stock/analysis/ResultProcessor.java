package com.stock.analysis;

import java.io.*;
import java.util.*;

public class ResultProcessor {
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: ResultProcessor <input-file> [output-dir]");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputDir = (args.length == 2) ? args[1] : "results";
        
        new File(outputDir).mkdirs();
        
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        Map<String, Integer> positiveWords = new HashMap<>();
        Map<String, Integer> negativeWords = new HashMap<>();
        
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\\t");
            if (parts.length == 2) {
                String key = parts[0];
                int count = Integer.parseInt(parts[1]);
                
                String[] keyParts = key.split(":");
                if (keyParts.length == 2) {
                    String sentiment = keyParts[0];
                    String word = keyParts[1];
                    
                    if (sentiment.equals("1")) {
                        positiveWords.put(word, count);
                    } else if (sentiment.equals("-1")) {
                        negativeWords.put(word, count);
                    }
                }
            }
        }
        reader.close();
        
        // 排序并输出正面词汇前100
        List<Map.Entry<String, Integer>> positiveList = new ArrayList<>(positiveWords.entrySet());
        positiveList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        PrintWriter positiveWriter = new PrintWriter(outputDir + "/positive_top100.txt");
        positiveWriter.println("Word\tCount");
        for (int i = 0; i < Math.min(100, positiveList.size()); i++) {
            Map.Entry<String, Integer> entry = positiveList.get(i);
            positiveWriter.println(entry.getKey() + "\t" + entry.getValue());
        }
        positiveWriter.close();
        
        // 排序并输出负面词汇前100
        List<Map.Entry<String, Integer>> negativeList = new ArrayList<>(negativeWords.entrySet());
        negativeList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        PrintWriter negativeWriter = new PrintWriter(outputDir + "/negative_top100.txt");
        negativeWriter.println("Word\tCount");
        for (int i = 0; i < Math.min(100, negativeList.size()); i++) {
            Map.Entry<String, Integer> entry = negativeList.get(i);
            negativeWriter.println(entry.getKey() + "\t" + entry.getValue());
        }
        negativeWriter.close();
        
        System.out.println("Processing completed!");
        System.out.println("Positive words found: " + positiveWords.size());
        System.out.println("Negative words found: " + negativeWords.size());
        System.out.println("Results saved to " + outputDir + " directory");
    }
}

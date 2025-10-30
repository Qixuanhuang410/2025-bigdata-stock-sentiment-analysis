package com.stock.analysis;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class WordCountMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
    
    private Set<String> stopWords = new HashSet<>();
    private Pattern punctuationPattern = Pattern.compile("[^a-zA-Z\\s]");
    private Pattern numberPattern = Pattern.compile("\\d+");
    private final static IntWritable one = new IntWritable(1);
    private Text wordKey = new Text();
    
    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        try {
            FileSystem fs = FileSystem.get(context.getConfiguration());
            Path stopWordsPath = new Path("/input/stop-word-list.txt");
            
            if (fs.exists(stopWordsPath)) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(fs.open(stopWordsPath))
                );
                String line;
                while ((line = reader.readLine()) != null) {
                    stopWords.add(line.trim().toLowerCase());
                }
                reader.close();
                System.out.println("Loaded " + stopWords.size() + " stop words");
            }
        } catch (Exception e) {
            System.err.println("Error loading stop words: " + e.getMessage());
        }
    }
    
    @Override
    public void map(LongWritable key, Text value, Context context) 
            throws IOException, InterruptedException {
        
        String line = value.toString().trim();
        
        // 跳过空行和标题行
        if (line.isEmpty() || line.startsWith("Text,Sentiment")) {
            return;
        }
        
        // 解析CSV格式："文本内容",情感标签
        String text = "";
        String sentiment = "";
        
        try {
            // 查找第一个逗号的位置（考虑引号内的逗号）
            boolean inQuotes = false;
            int commaIndex = -1;
            
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '\"') {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    commaIndex = i;
                    break;
                }
            }
            
            if (commaIndex == -1) {
                System.err.println("No comma found in line: " + line);
                return;
            }
            
            // 提取文本（移除引号）
            text = line.substring(0, commaIndex).trim();
            if (text.startsWith("\"") && text.endsWith("\"")) {
                text = text.substring(1, text.length() - 1);
            }
            
            // 提取情感标签
            sentiment = line.substring(commaIndex + 1).trim();
            
        } catch (Exception e) {
            System.err.println("Error parsing line: " + line);
            return;
        }
        
        // 验证情感标签
        if (!sentiment.equals("1") && !sentiment.equals("-1")) {
            System.err.println("Invalid sentiment: '" + sentiment + "' in line: " + line);
            return;
        }
        
        // 处理文本
        processText(context, text, sentiment);
    }
    
    private void processText(Context context, String text, String sentiment) 
            throws IOException, InterruptedException {
        
        if (text == null || text.isEmpty()) {
            return;
        }
        
        // 文本清洗
        String cleanText = cleanText(text);
        
        // 分词
        StringTokenizer tokenizer = new StringTokenizer(cleanText);
        int wordCount = 0;
        
        while (tokenizer.hasMoreTokens()) {
            String word = tokenizer.nextToken().trim();
            
            // 过滤停用词和单字符词
            if (word.length() > 1 && !stopWords.contains(word)) {
                // 输出格式: "sentiment_word"
                wordKey.set(sentiment + "_" + word);
                context.write(wordKey, one);
                wordCount++;
            }
        }
        
        // 调试信息
        if (wordCount > 0) {
            System.out.println("Processed " + wordCount + " words from sentiment " + sentiment);
        }
    }
    
    private String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // 转小写
        String cleaned = text.toLowerCase();
        // 移除标点符号
        cleaned = punctuationPattern.matcher(cleaned).replaceAll(" ");
        // 移除数字
        cleaned = numberPattern.matcher(cleaned).replaceAll("");
        // 移除多余空格
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }
}

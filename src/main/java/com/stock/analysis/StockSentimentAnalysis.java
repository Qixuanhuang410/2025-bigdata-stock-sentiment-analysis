package com.stock.analysis;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.fs.FileSystem;

public class StockSentimentAnalysis {

    public static class SentimentMapper extends Mapper<Object, Text, Text, IntWritable> {
        
        private final static IntWritable one = new IntWritable(1);
        private Text wordWithSentiment = new Text();
        private Set<String> stopWords = new HashSet<>();
        private Pattern pattern = Pattern.compile("[^a-zA-Z\\s]");
        
        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            // 从HDFS读取停用词文件
            Configuration conf = context.getConfiguration();
            String stopWordsPath = conf.get("stopwords.path");
            
            if (stopWordsPath != null) {
                try {
                    Path path = new Path(stopWordsPath);
                    FileSystem fs = FileSystem.get(conf);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(path)));
                    String stopWord;
                    while ((stopWord = reader.readLine()) != null) {
                        stopWords.add(stopWord.trim().toLowerCase());
                    }
                    reader.close();
                    System.out.println("Loaded " + stopWords.size() + " stop words from: " + stopWordsPath);
                } catch (IOException e) {
                    System.err.println("Error reading stop words file: " + e.getMessage());
                }
            } else {
                System.err.println("Stop words path not configured");
            }
        }
        
        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            
            if (line.isEmpty() || !line.contains(",")) {
                return;
            }
            
            try {
                String[] parts = line.split(",", 2);
                if (parts.length < 2) return;
                
                String text = parts[0].trim();
                String sentiment = parts[1].trim();
                
                if (!sentiment.equals("1") && !sentiment.equals("-1")) {
                    return;
                }
                
                String cleanText = pattern.matcher(text).replaceAll(" ").toLowerCase();
                String[] words = cleanText.split("\\s+");
                
                for (String word : words) {
                    word = word.trim();
                    if (!word.isEmpty() && !stopWords.contains(word) && word.length() > 1) {
                        wordWithSentiment.set(sentiment + ":" + word);
                        context.write(wordWithSentiment, one);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing line: " + line);
            }
        }
    }

    public static class SentimentReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();
        
        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context) 
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: StockSentimentAnalysis <input> <output>");
            System.exit(2);
        }
        
        Configuration conf = new Configuration();
        
        // 设置停用词文件路径
        String stopWordsPath = "/user/" + System.getProperty("user.name") + "/stopwords/stop-word-list.txt";
        conf.set("stopwords.path", stopWordsPath);
        
        Job job = Job.getInstance(conf, "stock sentiment analysis");
        job.setJarByClass(StockSentimentAnalysis.class);
        job.setMapperClass(SentimentMapper.class);
        job.setCombinerClass(SentimentReducer.class);
        job.setReducerClass(SentimentReducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        
        System.out.println("Input path: " + args[0]);
        System.out.println("Output path: " + args[1]);
        System.out.println("Stop words path: " + stopWordsPath);
        
        boolean success = job.waitForCompletion(true);
        System.exit(success ? 0 : 1);
    }
}

#!/bin/bash

echo "=== 重新组织项目结构 ==="

cd ~/stock-sentiment-analysis

# 创建标准目录结构
mkdir -p src classes lib results

echo "创建必要的源码文件..."

# 创建主分析程序
cat > src/StockSentimentAnalysis.java << 'EOF'
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.cache.DistributedCache;

public class StockSentimentAnalysis {

    public static class SentimentMapper extends Mapper<Object, Text, Text, IntWritable> {
        
        private final static IntWritable one = new IntWritable(1);
        private Text wordWithSentiment = new Text();
        private Set<String> stopWords = new HashSet<>();
        private Pattern pattern = Pattern.compile("[^a-zA-Z\\s]");
        
        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            try {
                Path[] stopWordsFiles = DistributedCache.getLocalCacheFiles(context.getConfiguration());
                if (stopWordsFiles != null && stopWordsFiles.length > 0) {
                    BufferedReader reader = new BufferedReader(new FileReader(stopWordsFiles[0].toString()));
                    String stopWord;
                    while ((stopWord = reader.readLine()) != null) {
                        stopWords.add(stopWord.trim().toLowerCase());
                    }
                    reader.close();
                    System.out.println("Loaded " + stopWords.size() + " stop words");
                }
            } catch (IOException e) {
                System.err.println("Error reading stop words file: " + e.getMessage());
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
        DistributedCache.addCacheFile(new Path("/user/" + System.getProperty("user.name") + "/stopwords/stop-word-list.txt").toUri(), conf);
        
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
        
        boolean success = job.waitForCompletion(true);
        System.exit(success ? 0 : 1);
    }
}
EOF

# 创建结果处理器
cat > src/ResultProcessor.java << 'EOF'
import java.io.*;
import java.util.*;

public class ResultProcessor {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: ResultProcessor <input-file>");
            System.exit(1);
        }
        
        String inputFile = args[0];
        String outputDir = "results";
        
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
EOF

echo "源码文件创建完成"
echo "=== 项目重组完成 ==="
echo "新的目录结构:"
tree -L 2 ~/stock-sentiment-analysis

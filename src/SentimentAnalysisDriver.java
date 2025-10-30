package com.stock.analysis;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.filecache.DistributedCache;

import java.net.URI;

public class SentimentAnalysisDriver {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: SentimentAnalysisDriver <input> <stopwords> <intermediate_output> <final_output>");
            System.exit(1);
        }

        String inputPath = args[0];
        String stopwordsPath = args[1];
        String intermediateOutput = args[2];
        String finalOutput = args[3];

        // First Job: Word Count by Sentiment
        Configuration conf1 = new Configuration();
        
        // Add stopwords to distributed cache
        Job job1 = Job.getInstance(conf1, "sentiment-word-count");
        job1.addCacheFile(new URI(stopwordsPath + "#stopwords"));
        
        job1.setJarByClass(SentimentAnalysisDriver.class);
        job1.setMapperClass(WordCountMapper.class);
        job1.setCombinerClass(WordCountReducer.class);
        job1.setReducerClass(WordCountReducer.class);
        
        job1.setOutputKeyClass(Text.class);
        job1.setOutputValueClass(IntWritable.class);
        
        job1.setInputFormatClass(TextInputFormat.class);
        job1.setOutputFormatClass(TextOutputFormat.class);
        
        FileInputFormat.addInputPath(job1, new Path(inputPath));
        FileOutputFormat.setOutputPath(job1, new Path(intermediateOutput));
        
        boolean success1 = job1.waitForCompletion(true);
        
        if (!success1) {
            System.exit(1);
        }

        // Second Job: Top 100 for each sentiment
        Configuration conf2 = new Configuration();
        conf2.set("mapreduce.output.textoutputformat.separator", "\t");
        
        Job job2 = Job.getInstance(conf2, "top-100-words");
        job2.setJarByClass(SentimentAnalysisDriver.class);
        job2.setMapperClass(TopKMapper.class);
        job2.setReducerClass(TopKReducer.class);
        
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(Text.class);
        
        job2.setInputFormatClass(TextInputFormat.class);
        job2.setOutputFormatClass(TextOutputFormat.class);
        
        FileInputFormat.addInputPath(job2, new Path(intermediateOutput));
        FileOutputFormat.setOutputPath(job2, new Path(finalOutput));
        
        boolean success2 = job2.waitForCompletion(true);
        
        System.exit(success2 ? 0 : 1);
    }
}

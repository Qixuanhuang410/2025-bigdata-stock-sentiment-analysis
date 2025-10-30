package com.stock.analysis;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class StockSentimentDriver {
    
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: StockSentimentDriver <input path> <output path>");
            System.exit(-1);
        }
        
        Configuration conf = new Configuration();
        conf.set("textinputformat.record.delimiter", "\n");
        
        Job job = Job.getInstance(conf, "stock sentiment word count");
        
        job.setJarByClass(StockSentimentDriver.class);
        job.setMapperClass(WordCountMapper.class);
        job.setCombinerClass(WordCountReducer.class);
        job.setReducerClass(WordCountReducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        
        System.out.println("Starting MapReduce job...");
        System.out.println("Input path: " + args[0]);
        System.out.println("Output path: " + args[1]);
        
        boolean success = job.waitForCompletion(true);
        System.out.println("Job completed: " + success);
        
        System.exit(success ? 0 : 1);
    }
}

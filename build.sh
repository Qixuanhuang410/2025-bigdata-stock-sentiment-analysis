#!/bin/bash

PROJECT_DIR="$HOME/stock-sentiment-analysis"
cd $PROJECT_DIR

export HADOOP_ROOT_LOGGER="WARN,console"

echo "=== 修复并运行股票情感分析 ==="

# 步骤1: 重新编译
echo "步骤1: 重新编译..."
export HADOOP_CLASSPATH=$(hadoop classpath)
javac -classpath $HADOOP_CLASSPATH -d classes src/StockSentimentAnalysis.java
javac -d classes src/ResultProcessor.java

if [ ! -f "classes/StockSentimentAnalysis.class" ]; then
    echo "错误: 编译失败"
    exit 1
fi

echo "编译成功!"

# 步骤2: 重新打包
echo "步骤2: 重新打包..."
jar -cvf lib/stock_analysis.jar -C classes . > /dev/null 2>&1

# 步骤3: 确保HDFS中有数据
echo "步骤3: 检查HDFS数据..."
hdfs dfs -test -e /user/$(whoami)/stock_analysis/stock_data.csv
if [ $? -ne 0 ]; then
    echo "上传数据到HDFS..."
    hdfs dfs -mkdir -p /user/$(whoami)/stock_analysis
    hdfs dfs -mkdir -p /user/$(whoami)/stopwords
    hdfs dfs -put data/stock_data.csv /user/$(whoami)/stock_analysis/
    hdfs dfs -put data/stop-word-list.txt /user/$(whoami)/stopwords/
fi

# 步骤4: 运行MapReduce
echo "步骤4: 运行MapReduce作业..."
hdfs dfs -rm -r /user/$(whoami)/stock_analysis/output 2>/dev/null
hadoop jar lib/stock_analysis.jar StockSentimentAnalysis \
    /user/$(whoami)/stock_analysis/stock_data.csv \
    /user/$(whoami)/stock_analysis/output

# 步骤5: 处理结果
echo "步骤5: 处理结果..."
hdfs dfs -get /user/$(whoami)/stock_analysis/output/part-r-00000 ./raw_output.txt
java -cp classes ResultProcessor raw_output.txt

echo "=== 分析完成 ==="
echo "结果文件: results/positive_top100.txt 和 results/negative_top100.txt"

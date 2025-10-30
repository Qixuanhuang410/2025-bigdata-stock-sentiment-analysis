#!/bin/bash

PROJECT_DIR="$HOME/stock-sentiment-analysis"
cd $PROJECT_DIR

export HADOOP_ROOT_LOGGER="WARN,console"

echo "=========================================="
echo "   股票情感分析 - 完整运行流程"
echo "=========================================="

log() {
    echo "[$(date '+%H:%M:%S')] $1"
}

# 步骤1: 环境检查
log "步骤1: 检查Hadoop环境..."
jps | grep -E "(NameNode|DataNode|ResourceManager|NodeManager)" > /dev/null
if [ $? -ne 0 ]; then
    echo "❌ Hadoop服务未启动，请先运行: start-dfs.sh && start-yarn.sh"
    exit 1
fi
echo "✅ Hadoop服务运行正常"

# 步骤2: 数据准备
log "步骤2: 准备数据..."
hdfs dfs -rm -r /user/$(whoami)/stock_analysis 2>/dev/null || true
hdfs dfs -rm -r /user/$(whoami)/stopwords 2>/dev/null || true
hdfs dfs -mkdir -p /user/$(whoami)/stock_analysis
hdfs dfs -mkdir -p /user/$(whoami)/stopwords
hdfs dfs -put data/stock_data.csv /user/$(whoami)/stock_analysis/
hdfs dfs -put data/stop-word-list.txt /user/$(whoami)/stopwords/
echo "✅ 数据上传完成"

# 步骤3: 项目编译
log "步骤3: 编译项目..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi
echo "✅ 项目编译成功"

# 步骤4: 运行MapReduce
log "步骤4: 运行MapReduce作业..."
hdfs dfs -rm -r /user/$(whoami)/stock_analysis/output 2>/dev/null || true
rm -rf output/* 2>/dev/null || true
mkdir -p output

start_time=$(date +%s)
# 使用 manifest 中的 Main-Class 运行（避免在某些 hadoop 版本中显式指定主类导致参数解析异常）
hadoop jar target/stock-sentiment-analysis-1.0-SNAPSHOT.jar \
    /user/$(whoami)/stock_analysis/stock_data.csv \
    /user/$(whoami)/stock_analysis/output

if [ $? -ne 0 ]; then
    echo "❌ MapReduce作业失败"
    exit 1
fi
end_time=$(date +%s)
echo "✅ MapReduce作业完成，耗时: $((end_time - start_time)) 秒"

# 步骤5: 处理结果
log "步骤5: 处理结果..."
    hdfs dfs -get /user/$(whoami)/stock_analysis/output/part-r-00000 ./output/
    # 直接使用已编译的 classes 运行 ResultProcessor；不要尝试在 target/classes 下 javac 源文件
    java -cp target/classes com.stock.analysis.ResultProcessor output/part-r-00000 output
echo "✅ 结果处理完成"

# 步骤6: 最终验证
log "步骤6: 验证最终结果..."
echo ""
echo "=========================================="
echo "           运行结果汇总"
echo "=========================================="
echo "📊 正面情感高频词 (前5个):"
head -5 output/positive_top100.txt
echo ""
echo "📊 负面情感高频词 (前5个):"
head -5 output/negative_top100.txt
echo ""
echo "📁 输出文件:"
ls -la output/
echo ""
echo "✅ 完整运行流程结束!"

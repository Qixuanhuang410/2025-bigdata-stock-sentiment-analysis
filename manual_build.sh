#!/bin/bash

cd ~/stock-sentiment-analysis

echo "=== 100%可靠手动构建 ==="

# 清理
rm -rf target
mkdir -p target/classes/com/stock/analysis

# 设置Hadoop classpath
export HADOOP_CLASSPATH=$(hadoop classpath)
echo "Hadoop classpath 设置完成"

# 找到所有Java文件
echo "寻找Java文件..."
JAVA_FILES=$(find . -name "*.java" -not -path "./target/*" | head -10)
echo "找到的Java文件:"
echo "$JAVA_FILES"

# 编译所有Java文件
echo "编译Java文件..."
javac -classpath $HADOOP_CLASSPATH \
    -d target/classes \
    $(find . -name "*.java" -not -path "./target/*")

# 检查编译结果
echo "编译结果:"
find target/classes -name "*.class" | head -10

if [ ! -f "target/classes/com/stock/analysis/StockSentimentAnalysis.class" ]; then
    echo "❌ 主要类文件未编译，尝试直接编译..."
    
    # 如果上面失败，尝试逐个编译
    for java_file in $(find . -name "*.java" -not -path "./target/*"); do
        echo "编译: $java_file"
        javac -classpath $HADOOP_CLASSPATH -d target/classes "$java_file"
    done
fi

# 创建JAR包
echo "创建JAR包..."
cd target/classes
jar -cvf ../stock-sentiment-analysis-1.0-SNAPSHOT.jar . > ../jar_creation.log 2>&1
cd ../..

# 添加Main-Class到manifest
echo "更新manifest..."
jar -uvfe target/stock-sentiment-analysis-1.0-SNAPSHOT.jar com.stock.analysis.StockSentimentAnalysis

# 最终验证
echo "=== 最终验证 ==="
echo "JAR文件大小: $(ls -la target/stock-sentiment-analysis-1.0-SNAPSHOT.jar | awk '{print $5}') bytes"
echo "JAR内容:"
jar -tf target/stock-sentiment-analysis-1.0-SNAPSHOT.jar | grep -E "(StockSentimentAnalysis|ResultProcessor)" | head -10

# 测试类加载
echo "测试类加载..."
java -cp target/stock-sentiment-analysis-1.0-SNAPSHOT.jar com.stock.analysis.StockSentimentAnalysis --help 2>&1 | head -3

echo "✅ 手动构建完成"

#!/bin/bash

cd ~/stock-sentiment-analysis

echo "=== 可靠构建过程 ==="

# 清理
rm -rf target/*
mkdir -p target/classes/com/stock/analysis

# 方法1: 尝试Maven
echo "方法1: 尝试Maven构建..."
mvn clean compile > maven_compile.log 2>&1

if [ $? -eq 0 ]; then
    echo "✅ Maven编译成功"
    mvn package -DskipTests > maven_package.log 2>&1
    if [ $? -eq 0 ]; then
        echo "✅ Maven打包成功"
        BUILD_METHOD="maven"
    else
        echo "⚠️ Maven打包失败，使用手动打包"
        BUILD_METHOD="manual"
    fi
else
    echo "⚠️ Maven编译失败，使用手动编译"
    BUILD_METHOD="manual"
fi

# 方法2: 手动编译（如果Maven失败）
if [ "$BUILD_METHOD" = "manual" ]; then
    echo "方法2: 手动编译..."
    export HADOOP_CLASSPATH=$(hadoop classpath)
    
    # 编译
    javac -classpath $HADOOP_CLASSPATH \
        -d target/classes \
        src/main/java/com/stock/analysis/StockSentimentAnalysis.java \
        src/main/java/com/stock/analysis/ResultProcessor.java
    
    # 检查编译结果
    if [ -f "target/classes/com/stock/analysis/StockSentimentAnalysis.class" ]; then
        echo "✅ 手动编译成功"
        
        # 创建JAR包
        jar -cvfm target/stock-sentiment-analysis-1.0-SNAPSHOT.jar \
            <(echo "Main-Class: com.stock.analysis.StockSentimentAnalysis") \
            -C target/classes . > jar_build.log 2>&1
        
        if [ $? -eq 0 ]; then
            echo "✅ 手动打包成功"
        else
            echo "❌ 手动打包失败"
            exit 1
        fi
    else
        echo "❌ 手动编译失败"
        exit 1
    fi
fi

# 验证最终结果
echo "=== 构建结果验证 ==="
echo "1. Class文件:"
find target/classes -name "*.class" | head -10

echo ""
echo "2. JAR文件内容:"
jar -tf target/stock-sentiment-analysis-1.0-SNAPSHOT.jar | grep -E "com/stock/analysis" | head -10

echo ""
echo "3. JAR文件信息:"
ls -la target/stock-sentiment-analysis-1.0-SNAPSHOT.jar

echo ""
echo "✅ 构建完成!"

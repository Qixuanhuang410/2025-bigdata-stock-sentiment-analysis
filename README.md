# 股票情感分析 - Stock Sentiment Analysis

## 项目简介

本项目使用Hadoop MapReduce对股票新闻标题进行情感分析，统计正面和负面情感新闻中出现频率最高的前100个单词。

## 设计思路

### 数据预处理流程
1. **文本清洗**：
   - 转换为小写字母
   - 移除所有标点符号
   - 移除数字字符
   
2. **停用词过滤**：
   - 使用提供的停用词列表（stop-word-list.txt）
   - 过滤常见的无意义词汇

3. **情感分类**：
   - 根据情感标签（1=正面，-1=负面）分别处理
   - 独立统计两种情感的词汇频率

### MapReduce设计
- **Mapper阶段**：
  - 输入：`<行号, "文本,情感标签">`
  - 处理：文本清洗、分词、停用词过滤
  - 输出：`<"情感:单词", 1>`

- **Reducer阶段**：
  - 输入：`<"情感:单词", [1,1,1,...]>`
  - 处理：对相同键的值求和
  - 输出：`<"情感:单词", 总次数>`

- **Combiner优化**：
  - 在Map端进行局部聚合，减少网络传输

### 后处理
- 分离正面和负面词汇统计结果
- 按词频降序排序
- 提取前100个高频词汇
- 格式化输出：`<单词>\t<次数>`

## 运行结果

### 正面情感高频词（示例）
```
   # 股票情感分析 - Stock Sentiment Analysis

   ## 项目简介

   本项目使用 Hadoop MapReduce 对股票相关短文本（如新闻标题、推文）进行情感词频统计：
   分别统计正/负情感文本中出现频率最高的词，最终输出每类前 100 个高频词。

   ## 目录结构（最终提交要求）

   ```
   stock-sentiment-analysis/
   ├── src/main/java/com/stock/analysis/
   │   ├── StockSentimentAnalysis.java
   │   └── ResultProcessor.java
   ├── target/
   │   └── stock-sentiment-analysis-1.0-SNAPSHOT.jar   # 保留 jar 即可
   ├── output/
   │   └── part-r-00000                                # MapReduce 原始输出（必须包含）
   ├── screenshots/                                    # 放作业运行截图（可选）
   ├── pom.xml
   ├── .gitignore
   └── README.md
   ```

   ## 构建说明

   依赖：Java、Maven；Hadoop 环境（伪分布式或集群）用于运行 MapReduce。

   在项目根目录运行：

   ```bash
   mvn clean package -DskipTests
   ```

   构建成功后 jar 位于 `target/stock-sentiment-analysis-1.0-SNAPSHOT.jar`。

   ## 运行说明

   项目提供 `run.sh` 脚本执行完整流程（上传数据、提交 MapReduce、获取输出并后处理）：

   ```bash
   ./run.sh
   ```

   脚本做的事：

   - 上传 `data/stock_data.csv` 和 `data/stop-word-list.txt` 到 HDFS
   - 提交 MapReduce：`hadoop jar target/...jar <input> <output>`（使用 JAR 的 manifest）
   - 下载 `/user/<you>/stock_analysis/output/part-r-00000` 到 `./output/`
   - 运行 `ResultProcessor` 生成 `output/positive_top100.txt` 和 `output/negative_top100.txt`

   你也可以手动按下列步骤运行：

   ```bash
   # 提交 MapReduce
   hadoop jar target/stock-sentiment-analysis-1.0-SNAPSHOT.jar \
       /user/$(whoami)/stock_analysis/stock_data.csv \
       /user/$(whoami)/stock_analysis/output

   # 下载并后处理
   hdfs dfs -get /user/$(whoami)/stock_analysis/output/part-r-00000 ./output/
   java -cp target/classes com.stock.analysis.ResultProcessor output/part-r-00000 output
   ```

   ## 运行示例（摘录）

   正面高频词（示例）：

   ```
   Word	Count
   aap	363
   user	301
   https	208
   ```

   负面高频词（示例）：

   ```
   Word	Count
   aap	326
   https	302
   short	233
   ```


   ## 已知问题与改进方向

   - 后处理（Top-K）在单机完成，建议改为第二轮 MapReduce 做分布式 Top-K
   - 使用 Hadoop 分布式缓存优化停用词加载
   - 在 pom 中将 Hadoop 相关依赖设为 `provided`，避免把 Hadoop 核心库打包进最终 jar

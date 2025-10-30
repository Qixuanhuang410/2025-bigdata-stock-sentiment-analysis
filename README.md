# 股票情感分析 - Stock Sentiment Analysis

## 项目简介

本项目使用 Hadoop MapReduce 对股票相关短文本（新闻标题 / 推文）进行情感词频统计：
分别统计正面（1）和负面（-1）文本中出现频率最高的词，并输出每类前 100 个高频词。

## 仓库结构（提交要求）

```
stock-sentiment-analysis/
├── src/main/java/com/stock/analysis/    # 源代码（含主类和后处理）
├── target/                              # 只在提交中保留 jar（target/*.jar）
├── output/                              # 包含 MapReduce 输出：part-r-00000
├── screenshots/                         # 作业成功截图（README 中引用）
├── pom.xml
├── .gitignore
└── README.md
```

## 构建（本地）

前提：已安装 Java、Maven。Hadoop（伪分布式或集群）用于运行 MapReduce。

在项目根目录运行：

```bash
mvn clean package -DskipTests
```

构建成功后，jar 位于 `target/stock-sentiment-analysis-1.0-SNAPSHOT.jar`。

## 运行（脚本）

项目提供 `run.sh` 实现：

- 上传数据到 HDFS（`data/stock_data.csv`、`data/stop-word-list.txt`）
- 提交 MapReduce：`hadoop jar target/…jar <input> <output>`（使用 JAR 的 Main-Class）
- 下载 `/user/<you>/stock_analysis/output/part-r-00000` 到本地 `output/`
- 运行 `ResultProcessor` 做后处理，生成 `output/positive_top100.txt` 和 `output/negative_top100.txt`

直接运行完整流程：

```bash
./run.sh
```

或手动运行：

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

以上示例来自一次本地运行，结果会随输入数据不同而变化。

## 提交清单（作业要求）

- `src/`（源代码）
- `target/*.jar`（只保留 jar）
- `output/part-r-00000`（MapReduce 原始输出）
- `pom.xml`
- `.gitignore`
- `README.md`
- `screenshots/`（作业成功截图，建议包含）

示例打包命令（项目根目录）：

```bash
zip -r submission.zip src target/*.jar output/part-r-00000 pom.xml .gitignore README.md screenshots
```

## 已知问题与改进

- 后处理（Top-K）目前在单机完成，遇到极大数据量时可能成为瓶颈，建议用二次 MapReduce 实现分布式 Top-K。
- 使用 Hadoop 分布式缓存优化停用词加载可减少每个 Mapper 的 HDFS 读开销。
- 在 `pom.xml` 中把 Hadoop 相关依赖设为 `provided`，避免把集群核心库打包进最终 jar。

## 如果你在 GitHub 上看到文件丢失或脚本缺失

我已在仓库中保留并提交 `build.sh`、`build_reliable.sh`、`manual_build.sh`、`organize.sh`、`run.sh` 等脚本。
如果你在 GitHub 页面仍然看不到某些文件，请刷新页面并确认你查看的分支是 `main`。如需，我可以把缺失文件再次恢复并强制推送。

---

如需我把仓库清理（例如把大文件从历史中移除）或把提交推到指定远程/更改分支名，请告诉我下一步操作。
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

   （以上示例来自一次本地运行，随输入不同会变化）

   ## 提交检查清单

   - [ ] `mvn clean package` 成功
   - [ ] `target/*.jar` 存在
   - [ ] `output/part-r-00000` 存在并已包含 MapReduce 输出
   - [ ] `output/positive_top100.txt` 和 `output/negative_top100.txt` 存在
   - [ ] 在 `screenshots/` 放一张作业成功的截图并在 README 中引用

   ## 提交打包建议

   提交时请包含：

   - `src/`
   - `target/*.jar` （仅 jar）
   - `output/part-r-00000`
   - `pom.xml`
   - `.gitignore`
   - `README.md`
   - `screenshots/`（可选）

   示例压缩命令（项目根目录）：

   ```bash
   zip -r submission.zip src target/*.jar output/part-r-00000 pom.xml .gitignore README.md screenshots || true
   ```

   ## 已知问题与改进方向

   - 后处理（Top-K）在单机完成，建议改为第二轮 MapReduce 做分布式 Top-K
   - 使用 Hadoop 分布式缓存优化停用词加载
   - 在 pom 中将 Hadoop 相关依赖设为 `provided`，避免把 Hadoop 核心库打包进最终 jar

   ## 需要我继续帮你做的事

   1. 我可以把仓库整理为一个 zip 提交包（包含上述文件）并列出打包清单；
   2. 我可以自动创建 `screenshots/` 占位并指导如何截取 YARN/JobTracker 成功截图；
   3. 或者我直接帮你把更严格的 `.gitignore` 和打包脚本加入仓库。

   请告诉我你希望下一步我代劳哪项。

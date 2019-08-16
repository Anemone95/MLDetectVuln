# 白盒漏洞挖掘

## [NDSS18]VulDeePecker: A Deep Learning-Based System for Vulnerability Detection.

使用BLSTM来检测C/C++中的安全漏洞。

![1565922880527](readme/1565922880527.png)

介绍视频：<https://www.youtube.com/watch?v=YcIzDi15zN0>

个人翻译：https://github.com/Anemone95/MLDetectVuln/blob/master/VulDeePecker_translation.md

原文：https://arxiv.org/pdf/1801.01681.pdf>

### 背景

目前研究（手工规则）的缺点：

1. 需要手工定义特征
2. 存在FN（漏报）

### 第一步：生成代码片段（code gadget）

本文以code gadget为一个漏洞单位

以library/API为point，向前进行程序切片，获取相关的代码片段，接着组装

![1565923192066](readme/1565923192066.png)

### 第二步：代码片段标记

![1565923249168](readme/1565923249168.png)

### 第三步：将代码片段转化为向量

将变量和用户函数替换为Token，接着将所有Token映射为向量

![1565923325641](readme/1565923325641.png)

### 第四步：训练BLSTM神经网络

![1565922754847](readme/1565922754847.png)

标准训练步骤，选用BLSTM因为该网络考虑了上下文

![1565923340213](readme/1565923340213.png)

### 第五步：检测

同样根据第一步到第三步将代码片段转化为向量，放入BLSTM，给结果

![1565923378866](readme/1565923378866.png)

### 实验

#### 讨论数据集的问题

选择了NVD和SARD中的漏洞

https://github.com/CGCL-codes/VulDeePecker

对于标记误报标记：用扫描器扫描历史版本，除了已发现问题的其他均为误报

#### 试验结果

![1565924694328](readme/1565924694328.png)



### 缺点

- 设计缺陷
  - 只能分析C/C++程序
  - 只能解决相关library/API方面的问题
  - 只能考虑data-flow不能考虑control-flow
- 实现缺陷
  - 受限于BLSTM
  - 代码未公开
- 评估缺陷
  - 只评估了缓冲区溢出和资源管理问题  

## [ACSAC2016]VulPecker: An Automated Vulnerability Detection System Based on Code Similarity Analysis

论文下载：http://www.cs.utsa.edu/~shxu/socs/VulPecker.pdf

VulDeePecker的前驱工作，定义一组描述补丁的特性以及代码相似度算法检测源代码片段是否存在漏洞。本文认为，一个能用在漏洞识别的相似性算法应该能够区别补丁前的代码和补丁后的代码。

本文贡献在于：

1. 本文建立了漏洞补丁数据集（VPD）和漏洞代码数据集（VCID）
2. 本文设计了一种相似度算法选择器，以针对不同的漏洞（尤其那些通过补丁推导出的漏洞）使用最优的相似度比较算法。

![1559011365745](readme/1559011365745.png)

### 如何选择相似度算法

本文利用漏洞代码和补丁代码从候选算法中挑选，构造一个<CVE, Algorithm>的映射

### 0x02 构建漏洞代码特征

本文将相似度算法的属性总结为以下三种：

- code-fragment level：代码片段等级，即算法比较的粒度
  - patch-without-context：如diff后，标号为"-"的那一部分
  - slice：通过程序依赖图（Program Dependence Graphs, PDG）做程序切片得到的子图。
  - path-with-context：diff后，标号为“-”的那一部分加上没有变化的那部分
  - function：函数
  - file/component：文件或者是一个组件

- code representation：代码表示，每一种代码片段都可以表示为
  - text：文本，但是其包含很少的语法和语义信息，不适合做相似度比较
  - metric：将其用向量表示，以此来相互比较，经常用作file/component的比较（如TF-IDF）
  - token：将一行代码或是代码中的符号用编译方法转换为token
  - tree：用树组织变量，函数调用或者其他元素，以保存语法信息
  - graph：将一个函数表示成图，节点为表达式或赋值，边表示控制流
- comparison method：比较方法
  - vector comparsion：按向量方式比较
  - approximate/exact matching：完全匹配或者部分匹配

对于一个CVE，本文diff前后版本的代码，并获取以下特征，Type2-6是对于Patch的描述。

- Type1：基本特征，CVE-ID、CWE-ID、供应商、威胁等级等
- Type2：无实质性的特征，如：代码格式、空格变化、注释等
- Type3：组件特征，如：变量名和值，常量名和值、函数声明
- Type4：表达式特征，表达式的变化，如：赋值表达式、if条件、for/while条件
- Type5：声明特征，声明的变化，如：增加，删除，移动
- Type6：函数特征：函数的变化，如：全局变量定义和宏

### 0x03 准备输入

在学习阶段的输入有NVD，VPD，VCID

我们构建VPD，它包含了CVEID对应的diff文件，由于多个漏洞版本有多个漏洞代码，因此这些代码被放入VCID。

在检测阶段，输入有CVE-ID，目标代码，CVEtoAlgorithm的映射，以及漏洞签名。

### 0x04 关于相似度算法选择

![1559028844464](readme/1559028844464.png)

候选相似度算法

![1559036087839](readme/1559036087839.png)

选择标准即该算法能区分未patched的代码和已patched的代码，并且能够有最低的漏报率。

### 0x05 提取漏洞特征

先用diff提取数“-”开头的代码和未标记的代码，接着进行预处理——去除空格，格式化，和注释——接着将“-”开头的代码和未标记的代码转换成相似度算法需要的特征，Type2和Type6可以直接从diff中得到，Type3-Type5特征需要通过一些算法（如gumtree算法https://github.com/GumTreeDiff/gumtree）结合已打补丁的代码得到。

### 0x06 漏洞检测

给定一种漏洞，目标代码，算法映射和漏洞特征，相似度算法比较相似度判断目标代码是否存在漏洞

## [NDSS19]Neural Machine Translation Inspired Binary Code Similarity Comparison beyond Function Pairs.

将汇编语言文本看做自然语言，从而使用NLP对汇编语言文本进行处理，由NLP自动挖掘汇编语言中的内在联系，最终达到相似度比较的目的

## [MAPL2017]Learning a Classifier for False Positive Error Reports Emitted by Static Code Analysis Tools

本文总结了误报模式，并且考虑用方法体/程序切片技术和机器学习降低误报率（总感觉跟NDSS18的那篇文章方法很像）

演讲视频：<https://www.youtube.com/watch?v=hWiBU_Ht3lE>

### 误报模式

- 用户自己写的取消污点函数影响了污点传播

- 当污染字符和安全字符插入同一个Hashmap/ArrayList，而只使用安全字符拼接SQL时，污点传播受到欺骗

### 数据集

Owasp benchmark和Juliet

### 方法体/程序切片

方法体：直接将警告的方法字节码作为输入

切片：使用Tuning WALA进行程序前向切片（存在分析时间过长的问题，论文中介绍了解决问题的方法）

![1558950373771](readme/1558950373771.png)

### 机器学习方法

朴素贝叶斯：对于朴素贝叶斯，我们只记录了操作码不记录操作数，但是调用的类会记录

![1558950060077](readme/1558950060077.png)

LSTM：对于LSTM，我们用空格分开每一个Token，并且记录部分操作数，但是对于外部类将其替换为UNK_CALL

![1558950269339](readme/1558950269339.png)

### 实验结果

![1558947313632](readme/1558947313632.png)

使用前向切片的分类器可以获得更少噪声，因此预测更准确。

### 未来工作

- 使用众包扩大数据
- 用RNN分析AST
- 扩展为半监督式的在线服务

## [ICST19]An Empirical Assessment of Machine Learning Approaches for Triaging Reports of a Java Static Analysis Tool

本文展开实证研究，比较手工设计特征，词袋，RNN和GNN，使用真实程序集来测试他们的效果

### 测试集构建

本文挑选了部分程序手工打标签

![1559119338210](readme/1559119338210.png)

benchmark和现实程序产生的误报原因存在不同：
1）工具发现不可能实现的流
2）工具不能发现污染源实际上被消除
3）污染源实际上没有被污染

### 方法选择

- Hand-engineered Features 手动写规则

- 程序切片，使用[Joana](https://github.com/joana-team/joana)程序切片工具，基于[WALA](http://wala.sourceforge.net/)

- Bag of Words，比较单词集合（不计顺序）的相似性，出现次数（BoW-Freq）、出现位置（BoW-Occ）

- RNN：

  - 数据清理：删除一些节点（不属于程序）

  - 抽象文字和字符串，例如两位数字用N2，三位数字用N3，更多位数字用N+,接着提取字符串列表并进行编号化，如STR1，STR2等

  - 许多程序员使用一组通用的短变量作为标识符如i,j,counter等，它对我们分析有意义，而其他变量名对程序分析无意义

    因此通过短语UNK标记一些无意义单词

  - 将camelCase和snake_case标识符拆分为组成单词  

- GNN

  - RNN不能表示程序的图结构，因此引入GNN

  - 我们使用了GNN的变体GGNN，引入三种初始化输入节点表示：

    1）种类、操作和类型字段（参照图一）

### 实验结果

#### RQ1总体结果

![1559198756774](readme/1559198756774.png)

LSTM系的效果最好，紧接着是BoW和GGNN

#### RQ2数据预处理的作用

见红字，抽象化变量对新项目有效果

#### RQ4进一步分析

不同的算法的检测能力不重叠

![1559199766366](readme/1559199766366.png)

### 威胁

- OWASP基准不能代表真实程序
- 我们扩充的14个程序的数据量不足以支持神经网络

### 未来工作

做一个投票机制，融合各个分析算法

# 修复方案生成

## [NDSS19]Automating Patching of Vulnerable Open-Source Software Versions in Application Binaries.

自动化给程序打补丁
# 评测流程

1、正常启动项目(必须包括LLM服务)
2、将要评测问题放到evalution/data目录下，如：internet.txt；将要评测问题对应的SQL也放到evalution/data目录下，如：gold_example_dusql.txt。
3、执行evalution.sh脚本，主要包括构建表数据、获取模型预测结果，执行对比逻辑。可以在命令行看到执行准确率，错误case会写到同目录的eval.json文件中。

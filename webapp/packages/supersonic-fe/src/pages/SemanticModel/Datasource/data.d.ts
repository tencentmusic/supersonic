// 任务查询结果列
export type TaskResultColumn = {
  name: string;
  type: string;
};

// 任务查询结果
export type TaskResultItem = Record<string, string | number>;

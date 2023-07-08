// 数据类型
export type DataInstanceItem = {
  sourceInstanceId: number; // 数据实例id
  sourceInstanceName: string; // 数据实例名
  defaultSourceId: number; // 查询表需要的默认datasource id
  bindSourceId: number;
};

// 任务查询结果列
export type TaskResultColumn = {
  name: string;
  type: string;
};

// 任务查询结果
export type TaskResultItem = Record<string, string | number>;

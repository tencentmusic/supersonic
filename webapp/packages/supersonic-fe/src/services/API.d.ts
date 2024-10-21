import type { SqlParamsItem } from '@/pages/data-explore/data';

declare namespace API {
  // export type CurrentUser = {
  //   avatar?: string;
  //   name?: string;
  //   title?: string;
  //   group?: string;
  //   signature?: string;
  //   tags?: {
  //     key: string;
  //     label: string;
  //   }[];
  //   userid?: string;
  //   access?: 'user' | 'guest' | 'admin';
  //   unreadCount?: number;
  // };

  export type CurrentUser = {
    staffid: string;
    staffName: string;
    orgName: string;
    access?: 'user' | 'guest' | 'admin';
  };

  export interface UserItem {
    id: number;
    name: string;
    displayName: string;
    email: string;
  }

  export interface UserAccessToken {
    createDate: string;
    expireDate: string;
    expireTime: number;
    id: number;
    name: string;
    token: string;
    userName: string;
  }

  export type LoginStateType = {
    status?: 'ok' | 'error';
    type?: string;
  };

  export type NoticeIconData = {
    id: string;
    key: string;
    avatar: string;
    title: string;
    datetime: string;
    type: string;
    read?: boolean;
    description: string;
    clickClose?: boolean;
    extra: any;
    status: string;
  };

  export type FieldItem = {
    fieldname: string; // 字段名
    fieldtype: string; // 字段类型
    fieldcomment: string; // 释义
    dimensionorindex: '0' | '1'; // 维度-0 指标-1
    rname: string; // 绑定现有维度/指标 -- name
    rguid: string; // 绑定现有维度/指标 -- guid
  };

  export type QueryChangeLogParams = Pagination & {
    typeId: string;
  };

  export type ChangeLogItem = {
    typeId: string;
    typeName: string;
    changeTime: string; // 变更时间
    owner: string; // 操作人
    comment: string; // 变更说明
    changeDetail: string; // 操作详情
  };

  export type ChangeLogRes = PaginationResponse<ChangeLogItem>;

  export type SearchParams = {
    name?: string; // 数据集名称
    description?: string; // 数据集描述
    bizcodes?: string[]; // 所属项目
    dsowner?: string; // 负责人
    startEndtime?: sting[]; // 开始-结束时间
    assetStatus?: number[]; // 数据集状态
    sensitivities?: number[]; // 敏感度
  };

  export type DataSetListParams = Pagination & SearchParams;

  export type DataSetItem = {
    guid: string;
    longid: number;
    name: string; // 数据集名称
    description: string; // 数据集描述
    datasouce: string; // 数据源名称
    datasouceId: number; // 数据源id
    tables: string; // 表名
    projectId: string; // 业务名称
    projectIdStr: string;
    owner: string; // 负责人
    updateperiod: string; // 更新周期
    updateperiodStr: string; // 更新周期
    opsource: number; // 来源
    createTimeStr: string;
    updateTimeStr: string;
    assetStatusStr: string; // 上线/下线
    isAsset: number; // 是否资产
    sensitivity: number;
    sensitivityStr: string; // 敏感度
    sql: string; // 技术口径
    sensitivity: number; //  数据敏感度：1-底 2-中 3-高
    variables: SqlParamsItem[]; // 数据集归属脚本参数
  };

  export type DataSetListRes = PaginationResponse<DataSetItem>;
  export type DataSetBasicInfo = DataSetItem;

  export type DimensionListSearchParams = {
    name: string; // 中文名
    dimensionSource: string[]; // 来源
    projectIds: string[]; // 所属项目
    dimensionbizName: string; // 英文名
    description: string; // 描述
  };

  export type QueryFieldListParams = Pagination & {
    guid: string;
    name?: string;
  };

  export type DataSetFieldItem = {
    columnXh: number; // 顺序标记：从0开始
    name: string; // 字段名
    fieldtype: string; // 字段类型
    description: string; // 释义
    dimensionorindex: '0' | '1'; // 维度-0 指标-1
    assetStatusStr: string; // 状态
    rname: string; // 绑定现有维度/指标 -- name
    rguid: string; // 绑定现有维度/指标 -- guid
  };

  export type DataSetFieldListRes = PaginationResponse<DataSetFieldItem>;

  // 搜索条件
  export type DataTableSearchParams = {
    name: string; // 表名
    description: string; // 描述详情
    extDatasouceenums: string[]; // 数据库类型
    extDwlayerenums: string[]; // 数仓分层
    extBizenums: string[]; // 所属项目
    queryRanges: string[]; // 查看范围
    storagesize: string; // 存储大小
    owner: string; // 所有者
    createtime: string; // 创建时间
  };
  export type DataTableListParams = Pagination & DataTableSearchParams;

  // 数据表列表
  export type DataTableListItem = {
    guid: string;
    name: string; // 表名
    description: string; // 描述
    projectFullName: string; // 所属项目
    isSpeedStr: string; // 是否加速
    dbName: string; // 库名
    dwLayer: string; // 数据分层
    assetSensitivityStr: string; // 数据敏感度
    storageSize: string; // 储存大小
    owner: string; // 所有者
    createTime: string; // 创建时间
    dbTableName: string; // 库名::表名
  };
  // 数据表列表
  export type DataTableListRes = PaginationResponse<DataTableListItem>;

  // 业务项目对象
  export type ProjectItem = AuthSdkType.AuthCodesItem & {
    projectId: string; // 项目ID
    projectIncreId: number; // 项目自增ID
    projectName: string; // 项目名
    projectParentId: string; // 父项目ID
    projectFullName: string; // 父项目-子项目
    projectLevel: number; // 项目层级
    comment: string; // 项目描述
    creator: string; // 项目创建人
    projectType: number; // 项目类别 0-为私有项目 1-为公共项目
    childDomainList?: DomainList;
    children?: DomainList;
    value: string;
  };

  export type DomainList = ProjectItem[];

  // 数据实例详情
  export type DataInstanceDetail = {
    type: string; // 类型 mysql、tdw
    id: string; // id
    name: string; // 名称
    description: string; // 描述
    ip?: string; // ip
    port?: string; // 端口
    bootstrap?: string; // 连接地址
  };

  export type AuthCodeItem = {
    code: string; // 权限码
    approverLevel: number;
    ext: string;
    isMenu: number;
    isVisible: number;
    no: number;
    pcode: string;
    url: string;
  };

  // 数据库查询参数
  export type DatabaseParams = {
    bindSourceId: number;
  };

  // 数据库列表子项
  export type DatabaseItem = {
    dbName: string; // 数据库名
    cnt: number; // 库中有权限表数量
  };
  // 数据类型
  export type DataInstanceItem = {
    sourceInstanceId: number; // 数据实例id
    sourceInstanceName: string; // 数据实例名
    defaultSourceId: number; // 查询表需要的默认datasource id
    bindSourceId: number;
  };
}

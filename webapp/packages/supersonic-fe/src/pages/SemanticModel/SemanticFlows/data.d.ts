import type { ISODateString, GraphConfigType, UserName } from '../data';
import type { NsGraph } from '@antv/xflow';

export type GraphConfigListItem = {
  id: number;
  domainId: number;
  config: string;
  type: GraphConfigType;
  createdAt: ISODateString;
  createdBy: UserName;
  updatedAt: ISODateString;
  updatedBy: UserName;
};

export type GraphConfig = { id: number; config: NsGraph.IGraphData };

export type RelationListItem = {
  id: number;
  domainId: number;
  datasourceFrom: number;
  datasourceTo: number;
  joinKey: string;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
};

export type RelationList = RelationListItem[];

import type {
  ICmdHooks as IHooks,
  NsGraph,
  IArgsBase,
  ICommandHandler,
  HookHub,
} from '@antv/xflow';
import { ManaSyringe } from '@antv/xflow';
import { ICommandContextProvider } from '@antv/xflow';
import { CustomCommands } from './constants';
import { getDatasourceRelaList } from '../../service';

// prettier-ignore
type ICommand = ICommandHandler<NsDataSourceRelationCmd.IArgs, NsDataSourceRelationCmd.IResult, NsDataSourceRelationCmd.ICmdHooks>;

export namespace NsDataSourceRelationCmd {
  /** Command: 用于注册named factory */
  // eslint-disable-next-line
  export const command = CustomCommands.DATASOURCE_RELATION;
  /** hook name */
  // eslint-disable-next-line
  export const hookKey = 'dataSourceRelation';
  /** hook 参数类型 */
  export interface IArgs extends IArgsBase {
    dataSourceRelationService: IDataSourceRelationService;
  }
  export interface IDataSourceRelationService {
    (meta: NsGraph.IGraphMeta, data: NsGraph.IGraphData): Promise<{ success: boolean }>;
  }
  /** hook handler 返回类型 */
  export type IResult = any[] | undefined;
  /** hooks 类型 */
  export interface ICmdHooks extends IHooks {
    dataSourceRelation: HookHub<IArgs, IResult>;
  }
}

@ManaSyringe.injectable()
/** 部署画布数据 */
export class DataSourceRelationCommand implements ICommand {
  /** api */
  @ManaSyringe.inject(ICommandContextProvider) contextProvider!: ICommand['contextProvider'];

  /** 执行Cmd */
  execute = async () => {
    const ctx = this.contextProvider();
    const { args } = ctx.getArgs();
    const hooks = ctx.getHooks();
    const graphMeta = await ctx.getGraphMeta();

    const domainId = graphMeta?.meta?.domainManger?.selectDomainId;
    if (!domainId) {
      return this;
    }
    const result = await hooks.dataSourceRelation.call(args, async () => {
      const { code, data } = await getDatasourceRelaList(domainId);
      if (code === 200) {
        return data;
      }
      return [];
    });
    ctx.setResult(result);
    ctx.setGlobal('dataSourceRelationList', result);
    return this;
  };

  /** undo cmd */
  undo = async () => {
    if (this.isUndoable()) {
      const ctx = this.contextProvider();
      ctx.undo();
    }
    return this;
  };

  /** redo cmd */
  redo = async () => {
    if (!this.isUndoable()) {
      await this.execute();
    }
    return this;
  };

  isUndoable(): boolean {
    const ctx = this.contextProvider();
    return ctx.isUndoable();
  }
}

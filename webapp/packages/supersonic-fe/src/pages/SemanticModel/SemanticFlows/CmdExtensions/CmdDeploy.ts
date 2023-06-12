import type {
  NsGraphCmd,
  ICmdHooks as IHooks,
  NsGraph,
  IArgsBase,
  ICommandHandler,
  HookHub,
} from '@antv/xflow';
import { XFlowGraphCommands, ManaSyringe } from '@antv/xflow';
import { ICommandContextProvider } from '@antv/xflow';
import { CustomCommands } from './constants';

// prettier-ignore
type ICommand = ICommandHandler<NsDeployDagCmd.IArgs, NsDeployDagCmd.IResult, NsDeployDagCmd.ICmdHooks>;

export namespace NsDeployDagCmd {
  /** Command: 用于注册named factory */
  // eslint-disable-next-line
  export const command = CustomCommands.DEPLOY_SERVICE;
  /** hook name */
  // eslint-disable-next-line
  export const hookKey = 'deployDag';
  /** hook 参数类型 */
  export interface IArgs extends IArgsBase {
    deployDagService: IDeployDagService;
  }
  export interface IDeployDagService {
    (meta: NsGraph.IGraphMeta, data: NsGraph.IGraphData): Promise<{ success: boolean }>;
  }
  /** hook handler 返回类型 */
  export interface IResult {
    success: boolean;
  }
  /** hooks 类型 */
  export interface ICmdHooks extends IHooks {
    deployDag: HookHub<IArgs, IResult>;
  }
}

@ManaSyringe.injectable()
/** 部署画布数据 */
export class DeployDagCommand implements ICommand {
  /** api */
  @ManaSyringe.inject(ICommandContextProvider) contextProvider!: ICommand['contextProvider'];

  /** 执行Cmd */
  execute = async () => {
    const ctx = this.contextProvider();
    const { args } = ctx.getArgs();
    const hooks = ctx.getHooks();
    const result = await hooks.deployDag.call(args, async (handlerArgs) => {
      const { commandService, deployDagService } = handlerArgs;
      /** 执行Command */
      await commandService!.executeCommand<NsGraphCmd.SaveGraphData.IArgs>(
        XFlowGraphCommands.SAVE_GRAPH_DATA.id,
        {
          saveGraphDataService: async (meta, graph) => {
            await deployDagService(meta, graph);
          },
        },
      );
      return { success: true };
    });
    ctx.setResult(result);
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

import type { HookHub, ICmdHooks as IHooks, NsGraph } from '@antv/xflow';
import { Deferred, ManaSyringe } from '@antv/xflow';
import { Modal, ConfigProvider } from 'antd';
import type { IArgsBase, ICommandHandler } from '@antv/xflow';
import { ICommandContextProvider } from '@antv/xflow';
import { DATASOURCE_NODE_RENDER_ID } from '../constant';
import { CustomCommands } from './constants';

// import 'antd/es/modal/style/index.css';

export namespace NsConfirmModalCmd {
  /** Command: 用于注册named factory */
  // eslint-disable-next-line
  export const command = CustomCommands.SHOW_CONFIRM_MODAL;
  /** hook name */
  // eslint-disable-next-line
  export const hookKey = 'confirmModal';
  /** hook 参数类型 */
  export interface IArgs extends IArgsBase {
    nodeConfig: NsGraph.INodeConfig;
    confirmModalCallBack: IConfirmModalService;
  }
  export interface IConfirmModalService {
    (): Promise<any>;
  }
  /** hook handler 返回类型 */
  export type IResult = any;
  /** hooks 类型 */
  export interface ICmdHooks extends IHooks {
    confirmModal: HookHub<IArgs, IResult>;
  }
}

const deleteDataSourceConfirmNode = (name: string) => {
  return (
    <>
      数据源<span style={{ color: '#296DF3', fontWeight: 'bold' }}>{name}</span>
      将被删除，是否确认？
    </>
  );
};

// prettier-ignore
type ICommand = ICommandHandler<NsConfirmModalCmd.IArgs, NsConfirmModalCmd.IResult, NsConfirmModalCmd.ICmdHooks>;

@ManaSyringe.injectable()
/** 部署画布数据 */
export class ConfirmModalCommand implements ICommand {
  /** api */
  @ManaSyringe.inject(ICommandContextProvider) contextProvider!: ICommand['contextProvider'];

  /** 执行Cmd */
  execute = async () => {
    const ctx = this.contextProvider();
    const { args } = ctx.getArgs();
    const hooks = ctx.getHooks();
    await hooks.confirmModal.call(args, async (confirmArgs: NsConfirmModalCmd.IArgs) => {
      const { nodeConfig, confirmModalCallBack } = confirmArgs;
      const { renderKey, label } = nodeConfig;
      if (!nodeConfig.modalProps?.modalContent) {
        let modalContent = <></>;
        if (renderKey === DATASOURCE_NODE_RENDER_ID) {
          modalContent = deleteDataSourceConfirmNode(label!);
        }
        nodeConfig.modalProps = {
          ...(nodeConfig.modalProps || {}),
          modalContent,
        };
      }
      const getAppContext: IGetAppCtx = () => {
        return {
          confirmModalCallBack,
        };
      };

      const x6Graph = await ctx.getX6Graph();
      const cell = x6Graph.getCellById(nodeConfig.id);

      if (!cell || !cell.isNode()) {
        throw new Error(`${nodeConfig.id} is not a valid node`);
      }
      /** 通过modal 获取 new name */
      await showModal(nodeConfig, getAppContext);

      return;
    });

    // ctx.setResult(result);
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

export interface IGetAppCtx {
  (): {
    confirmModalCallBack: NsConfirmModalCmd.IConfirmModalService;
  };
}

export type IModalInstance = ReturnType<typeof Modal.confirm>;

function showModal(node: NsGraph.INodeConfig, getAppContext: IGetAppCtx) {
  /** showModal 返回一个Promise */
  const defer = new Deferred<string | void>();
  const modalTitle = node.modalProps?.title;
  const modalContent = node.modalProps?.modalContent;
  /** modal确认保存逻辑 */
  class ModalCache {
    static modal: IModalInstance;
  }

  /** modal确认保存逻辑 */
  const onOk = async () => {
    const { modal } = ModalCache;
    const appContext = getAppContext();
    const { confirmModalCallBack } = appContext;
    try {
      modal.update({ okButtonProps: { loading: true } });

      /** 执行 confirm回调*/
      if (confirmModalCallBack) {
        await confirmModalCallBack();
      }
      /** 更新成功后，关闭modal */
      onHide();
    } catch (error) {
      console.error(error);
      /** 如果resolve空字符串则不更新 */
      modal.update({ okButtonProps: { loading: false } });
    }
  };

  /** modal销毁逻辑 */
  const onHide = () => {
    modal.destroy();
    ModalCache.modal = null as any;
    container.destroy();
  };

  /** modal内容 */
  const ModalContent = () => {
    return (
      <div>
        <ConfigProvider>{modalContent}</ConfigProvider>
      </div>
    );
  };
  /** 创建modal dom容器 */
  const container = createContainer();
  /** 创建modal */
  const modal = Modal.confirm({
    title: modalTitle,
    content: <ModalContent />,
    getContainer: () => {
      return container.element;
    },
    okButtonProps: {
      onClick: (e) => {
        e.stopPropagation();
        onOk();
      },
    },
    onCancel: () => {
      onHide();
    },
    afterClose: () => {
      onHide();
    },
  });

  /** 缓存modal实例 */
  ModalCache.modal = modal;

  /** showModal 返回一个Promise，用于await */
  return defer.promise;
}

const createContainer = () => {
  const div = document.createElement('div');
  div.classList.add('xflow-modal-container');
  window.document.body.append(div);
  return {
    element: div,
    destroy: () => {
      window.document.body.removeChild(div);
    },
  };
};

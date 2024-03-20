import type { HookHub, ICmdHooks as IHooks, NsGraph, IModelService } from '@antv/xflow';
import { Deferred, ManaSyringe } from '@antv/xflow';
import type { FormInstance } from 'antd';
import { Modal, Form, Input, ConfigProvider } from 'antd';

import type { IArgsBase, ICommandHandler, IGraphCommandService } from '@antv/xflow';
import { ICommandContextProvider } from '@antv/xflow';

import { CustomCommands } from './constants';

// import 'antd/es/modal/style/index.css';

// prettier-ignore
type ICommand = ICommandHandler<NsRenameNodeCmd.IArgs, NsRenameNodeCmd.IResult, NsRenameNodeCmd.ICmdHooks>;

export namespace NsRenameNodeCmd {
  /** Command: 用于注册named factory */
  // eslint-disable-next-line
  export const command = CustomCommands.SHOW_RENAME_MODAL;
  /** hook name */
  // eslint-disable-next-line
  export const hookKey = 'renameNode';
  /** hook 参数类型 */
  export interface IArgs extends IArgsBase {
    nodeConfig: NsGraph.INodeConfig;
    updateNodeNameService: IUpdateNodeNameService;
  }
  export interface IUpdateNodeNameService {
    (newName: string, nodeConfig: NsGraph.INodeConfig, meta: NsGraph.IGraphMeta): Promise<{
      err: string | null;
      nodeName: string;
    }>;
  }
  /** hook handler 返回类型 */
  export interface IResult {
    err: string | null;
    preNodeName?: string;
    currentNodeName?: string;
  }
  /** hooks 类型 */
  export interface ICmdHooks extends IHooks {
    renameNode: HookHub<IArgs, IResult>;
  }
}

@ManaSyringe.injectable()
/** 部署画布数据 */
// prettier-ignore
export class RenameNodeCommand implements ICommand {
  /** api */
  @ManaSyringe.inject(ICommandContextProvider) contextProvider!: ICommand['contextProvider'];

  /** 执行Cmd */
  execute = async () => {
    const ctx = this.contextProvider();
    // const app = useXFlowApp();
    const { args } = ctx.getArgs();
    const hooks = ctx.getHooks();
    const result = await hooks.renameNode.call(args, async (args) => {
      const { nodeConfig, graphMeta, commandService, modelService, updateNodeNameService } = args;
      const preNodeName = nodeConfig.label;

      const getAppContext: IGetAppCtx = () => {
        return {
          graphMeta,
          commandService,
          modelService,
          updateNodeNameService,
        };
      };

      const x6Graph = await ctx.getX6Graph();
      const cell = x6Graph.getCellById(nodeConfig.id);
      const nodes = x6Graph.getNodes();
      const edges = x6Graph.getEdges();
      nodes.forEach((node) => {
        if (node !== cell) {
          x6Graph.removeCell(node);
        }
      });
      edges.forEach((edge) => {
        x6Graph.removeEdge(edge);
      });
      if (!cell || !cell.isNode()) {
        throw new Error(`${nodeConfig.id} is not a valid node`);
      }
      /** 通过modal 获取 new name */
      const newName = await showModal(nodeConfig, getAppContext);
      /** 更新 node name  */
      if (newName) {
        const cellData = cell.getData<NsGraph.INodeConfig>();

        cell.setData({ ...cellData, label: newName } as NsGraph.INodeConfig);
        return { err: null, preNodeName, currentNodeName: newName };
      }
      return { err: null, preNodeName, currentNodeName: '' };
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

export interface IGetAppCtx {
  (): {
    graphMeta: NsGraph.IGraphMeta;
    commandService: IGraphCommandService;
    modelService: IModelService;
    updateNodeNameService: NsRenameNodeCmd.IUpdateNodeNameService;
  };
}

export type IModalInstance = ReturnType<typeof Modal.confirm>;
export interface IFormProps {
  newNodeName: string;
}

const layout = {
  labelCol: { span: 5 },
  wrapperCol: { span: 19 },
};

function showModal(node: NsGraph.INodeConfig, getAppContext: IGetAppCtx) {
  /** showModal 返回一个Promise */
  const defer = new Deferred<string | void>();

  /** modal确认保存逻辑 */
  class ModalCache {
    static modal: IModalInstance;
    static form: FormInstance<IFormProps>;
  }

  /** modal确认保存逻辑 */
  const onOk = async () => {
    const { form, modal } = ModalCache;
    const appContext = getAppContext();
    const { updateNodeNameService, graphMeta } = appContext;
    try {
      modal.update({ okButtonProps: { loading: true } });
      await form.validateFields();
      const values = await form.getFieldsValue();
      const newName: string = values.newNodeName;
      /** 执行 backend service */
      if (updateNodeNameService) {
        const { err, nodeName } = await updateNodeNameService(newName, node, graphMeta);
        if (err) {
          throw new Error(err);
        }
        defer.resolve(nodeName);
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
    ModalCache.form = null as any;
    ModalCache.modal = null as any;
    container.destroy();
  };

  /** modal内容 */
  const ModalContent = () => {
    const [form] = Form.useForm<IFormProps>();
    /** 缓存form实例 */
    ModalCache.form = form;

    return (
      <div>
        <ConfigProvider>
          <Form form={form} {...layout} initialValues={{ newNodeName: node.label }}>
            <Form.Item
              name="newNodeName"
              label="节点名"
              rules={[
                { required: true, message: '请输入新节点名' },
                { min: 3, message: '节点名不能少于3个字符' },
              ]}
            >
              <Input />
            </Form.Item>
          </Form>
        </ConfigProvider>
      </div>
    );
  };
  /** 创建modal dom容器 */
  const container = createContainer();
  /** 创建modal */
  const modal = Modal.confirm({
    title: '重命名',
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

import { DeployDagCommand, NsDeployDagCmd } from './CmdDeploy';
import { RenameNodeCommand, NsRenameNodeCmd } from './CmdRenameNodeModal';
import { ConfirmModalCommand, NsConfirmModalCmd } from './CmdConfirmModal';
import {
  DataSourceRelationCommand,
  NsDataSourceRelationCmd,
} from './CmdUpdateDataSourceRelationList';
import type { ICommandContributionConfig } from '@antv/xflow';
/** 注册成为可以执行的命令 */

export const COMMAND_CONTRIBUTIONS: ICommandContributionConfig[] = [
  {
    ...NsDeployDagCmd,
    CommandHandler: DeployDagCommand,
  },
  {
    ...NsRenameNodeCmd,
    CommandHandler: RenameNodeCommand,
  },
  {
    ...NsConfirmModalCmd,
    CommandHandler: ConfirmModalCommand,
  },
  {
    ...NsDataSourceRelationCmd,
    CommandHandler: DataSourceRelationCommand,
  },
];

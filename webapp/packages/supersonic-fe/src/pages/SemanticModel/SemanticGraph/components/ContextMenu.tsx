import G6 from '@antv/g6';
import '../style.less';
import { Item } from '@antv/g6-core';
import { presetsTagDomString } from '../../components/AntdComponentDom/Tag';
import { SemanticNodeType } from '../../enum';
import { SEMANTIC_NODE_TYPE_CONFIG } from '../../constant';

type InitContextMenuProps = {
  onMenuClick?: (key: string, item: Item) => void;
};

export const getMenuConfig = (props?: InitContextMenuProps) => {
  const { onMenuClick } = props || {};
  return {
    getContent(evt) {
      const nodeData = evt?.item?._cfg?.model;
      const { name, nodeType } = nodeData as any;
      if (nodeData) {
        const nodeTypeConfig = SEMANTIC_NODE_TYPE_CONFIG[nodeType] || {};
        let ulNode = `
        <li title='编辑' key='edit' >编辑</li>
        <li title='删除' key='delete' >删除</li>
     `;
        // if (nodeType === SemanticNodeType.DATASOURCE) {
        //   ulNode = `
        //       <li title='新增维度' key='createDimension' >新增维度</li>
        //       <li title='新增指标' key='createMetric' >新增指标</li>
        //       <li title='编辑' key='editDatasource' >编辑</li>
        //       <li title='删除' key='deleteDatasource' >删除</li>
        //     `;
        // }
        if (nodeType === SemanticNodeType.DATASOURCE) {
          ulNode = `
              <li title='编辑' key='editDatasource' >编辑</li>
              <li title='删除' key='deleteDatasource' >删除</li>
            `;
        }
        const header = `${name}`;
        return `<div class="g6ContextMenuContainer">
          <h3>${presetsTagDomString(nodeTypeConfig.label, nodeTypeConfig.color)}${header}</h3>
          <ul>
        ${ulNode}
        </ul>
        </div>`;
      }
      return `<div>当前节点信息获取失败</div>`;
    },
    handleMenuClick(target, item) {
      const targetKey = target.getAttribute('key') || '';
      onMenuClick?.(targetKey, item);
    },
    // offsetX and offsetY include the padding of the parent container
    // 需要加上父级容器的 padding-left 16 与自身偏移量 10
    offsetX: 16 + 10,
    // 需要加上父级容器的 padding-top 24 、画布兄弟元素高度、与自身偏移量 10
    offsetY: 0,
    // the types of items that allow the menu show up
    // 在哪些类型的元素上响应
    itemTypes: ['node'],
  };
};

const initContextMenu = (props?: InitContextMenuProps) => {
  const config = getMenuConfig(props);
  const contextMenu = new G6.Menu(config);

  return contextMenu;
};
export default initContextMenu;

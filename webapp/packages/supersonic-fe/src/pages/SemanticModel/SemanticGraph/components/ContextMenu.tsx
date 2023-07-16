import G6 from '@antv/g6';
import '../style.less';
// define the CSS with the id of your menu

// insertCss(`
//   #contextMenu {
//     position: absolute;
//     list-style-type: none;
//     padding: 10px 8px;
//     left: -150px;
//     background-color: rgba(255, 255, 255, 0.9);
//     border: 1px solid #e2e2e2;
//     border-radius: 4px;
//     font-size: 12px;
//     color: #545454;
//   }
//   #contextMenu li {
//     cursor: pointer;
// 		list-style-type:none;
//     list-style: none;
//     margin-left: 0px;
//   }
//   #contextMenu li:hover {
//     color: #aaa;
//   }
// `);

const initContextMenu = () => {
  const contextMenu = new G6.Menu({
    getContent(evt) {
      const itemType = evt!.item!.getType();
      console.log(this, evt?.item?._cfg, 333);
      const nodeData = evt?.item?._cfg?.model;
      const { name } = nodeData as any;
      if (nodeData) {
        const header = `${name}`;
        return `<div class="g6ContextMenuContainer">
          <h3>${header}</h3>
          <ul>
            <li title='2'>编辑</li>
            <li title='1'>删除</li>
          </ul>
        </div>`;
      }
      return `<div>当前节点信息获取失败</div>`;
    },
    handleMenuClick(target, item) {
      console.log(contextMenu, target, item);
      const graph = contextMenu._cfgs.graph;
    },
    // offsetX and offsetY include the padding of the parent container
    // 需要加上父级容器的 padding-left 16 与自身偏移量 10
    offsetX: 16 + 10,
    // 需要加上父级容器的 padding-top 24 、画布兄弟元素高度、与自身偏移量 10
    offsetY: 0,
    // the types of items that allow the menu show up
    // 在哪些类型的元素上响应
    itemTypes: ['node'],
  });

  return contextMenu;
};
export default initContextMenu;

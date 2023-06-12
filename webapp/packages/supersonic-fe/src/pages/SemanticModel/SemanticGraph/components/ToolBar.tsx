import G6 from '@antv/g6';
// import { modifyCSS, createDom } from '@antv/dom-util';
import { createDom } from '@antv/dom-util';
const initToolBar = () => {
  // const defaultConfig = G6.ToolBar
  const toolBarInstance = new G6.ToolBar();

  const config = toolBarInstance._cfgs;
  const defaultContentDomString = config.getContent();
  // const regex = /<ul[^>]*>|<\/ul>/g;
  // const innerDom = defaultContentDom.replace(regex, '');
  const defaultContentDom = createDom(defaultContentDomString);

  // @ts-ignore
  const elements = defaultContentDom.querySelectorAll('li[code="redo"], li[code="undo"]');
  elements.forEach((element) => {
    element.remove();
  });

  const searchBtnDom = `<li code="search">
      <svg
        viewBox="64 64 896 896"
        focusable="false"
        data-icon="search"
        width="24"
        height="24"
        fill="currentColor"
        aria-hidden="true"
      >
        <path d="M909.6 854.5L649.9 594.8C690.2 542.7 712 479 712 412c0-80.2-31.3-155.4-87.9-212.1-56.6-56.7-132-87.9-212.1-87.9s-155.5 31.3-212.1 87.9C143.2 256.5 112 331.8 112 412c0 80.1 31.3 155.5 87.9 212.1C256.5 680.8 331.8 712 412 712c67 0 130.6-21.8 182.7-62l259.7 259.6a8.2 8.2 0 0011.6 0l43.6-43.5a8.2 8.2 0 000-11.6zM570.4 570.4C528 612.7 471.8 636 412 636s-116-23.3-158.4-65.6C211.3 528 188 471.8 188 412s23.3-116.1 65.6-158.4C296 211.3 352.2 188 412 188s116.1 23.2 158.4 65.6S636 352.2 636 412s-23.3 116.1-65.6 158.4z" />
      </svg>
    </li>`;
  const toolbar = new G6.ToolBar({
    position: { x: 10, y: 10 },
    getContent: () => {
      return `${searchBtnDom}${defaultContentDom}`;
    },
  });
  // const toolbar = new G6.ToolBar({
  //   getContent: (graph) => {
  //     const searchInput = document.createElement('input');
  //     searchInput.id = 'search-input';
  //     searchInput.placeholder = '搜索节点';

  //     const searchBtn = document.createElement('button');
  //     searchBtn.id = 'search-btn';
  //     searchBtn.innerHTML = '搜索';

  //     const container = document.createElement('div');
  //     container.appendChild(searchInput);
  //     container.appendChild(searchBtn);
  //     return container;
  //   },
  //   handleClick: (name, graph) => {
  //     if (name === 'search-btn') {
  //       const searchText = document.getElementById('search-input').value.trim();
  //       if (!searchText) {
  //         return;
  //       }

  //       const foundNode = graph.getNodes().find((node) => {
  //         const model = node.getModel();
  //         return model.label === searchText;
  //       });

  //       if (foundNode) {
  //         // 如果找到了节点，将其设置为选中状态
  //         graph.setItemState(foundNode, 'active', true);
  //         // 将视图移动到找到的节点位置
  //         graph.focusItem(foundNode, true, {
  //           duration: 300,
  //           easing: 'easeCubic',
  //         });
  //       } else {
  //         alert('未找到节点');
  //       }
  //     }
  //   },
  // });
  return toolbar;
};
export default initToolBar;

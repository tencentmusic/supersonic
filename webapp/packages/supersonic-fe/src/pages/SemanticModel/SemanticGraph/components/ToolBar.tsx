import G6, { Graph } from '@antv/g6';
import { IAbstractGraph as IGraph } from '@antv/g6-core';
import { createDom } from '@antv/dom-util';
import { ToolBarSearchCallBack } from '../../data';
const searchIconSvgPath = `<path d="M909.6 854.5L649.9 594.8C690.2 542.7 712 479 712 412c0-80.2-31.3-155.4-87.9-212.1-56.6-56.7-132-87.9-212.1-87.9s-155.5 31.3-212.1 87.9C143.2 256.5 112 331.8 112 412c0 80.1 31.3 155.5 87.9 212.1C256.5 680.8 331.8 712 412 712c67 0 130.6-21.8 182.7-62l259.7 259.6a8.2 8.2 0 0011.6 0l43.6-43.5a8.2 8.2 0 000-11.6zM570.4 570.4C528 612.7 471.8 636 412 636s-116-23.3-158.4-65.6C211.3 528 188 471.8 188 412s23.3-116.1 65.6-158.4C296 211.3 352.2 188 412 188s116.1 23.2 158.4 65.6S636 352.2 636 412s-23.3 116.1-65.6 158.4z" />`;
const visibleModeIconSvgPath = `<path d="M880 112H144c-17.7 0-32 14.3-32 32v736c0 17.7 14.3 32 32 32h736c17.7 0 32-14.3 32-32V144c0-17.7-14.3-32-32-32zm-40 728H184V184h656v656zM340 683v77c0 4.4 3.6 8 8 8h48c4.4 0 8-3.6 8-8v-77c-10.1 3.3-20.8 5-32 5s-21.9-1.8-32-5zm64-198V264c0-4.4-3.6-8-8-8h-48c-4.4 0-8 3.6-8 8v221c10.1-3.3 20.8-5 32-5s21.9 1.8 32 5zm-64 198c10.1 3.3 20.8 5 32 5s21.9-1.8 32-5c41.8-13.5 72-52.7 72-99s-30.2-85.5-72-99c-10.1-3.3-20.8-5-32-5s-21.9 1.8-32 5c-41.8 13.5-72 52.7-72 99s30.2 85.5 72 99zm.1-115.7c.3-.6.7-1.2 1-1.8v-.1l1.2-1.8c.1-.2.2-.3.3-.5.3-.5.7-.9 1-1.4.1-.1.2-.3.3-.4.5-.6.9-1.1 1.4-1.6l.3-.3 1.2-1.2.4-.4c.5-.5 1-.9 1.6-1.4.6-.5 1.1-.9 1.7-1.3.2-.1.3-.2.5-.3.5-.3.9-.7 1.4-1 .1-.1.3-.2.4-.3.6-.4 1.2-.7 1.9-1.1.1-.1.3-.1.4-.2.5-.3 1-.5 1.6-.8l.6-.3c.7-.3 1.3-.6 2-.8.7-.3 1.4-.5 2.1-.7.2-.1.4-.1.6-.2.6-.2 1.1-.3 1.7-.4.2 0 .3-.1.5-.1.7-.2 1.5-.3 2.2-.4.2 0 .3 0 .5-.1.6-.1 1.2-.1 1.8-.2h.6c.8 0 1.5-.1 2.3-.1s1.5 0 2.3.1h.6c.6 0 1.2.1 1.8.2.2 0 .3 0 .5.1.7.1 1.5.2 2.2.4.2 0 .3.1.5.1.6.1 1.2.3 1.7.4.2.1.4.1.6.2.7.2 1.4.4 2.1.7.7.2 1.3.5 2 .8l.6.3c.5.2 1.1.5 1.6.8.1.1.3.1.4.2.6.3 1.3.7 1.9 1.1.1.1.3.2.4.3.5.3 1 .6 1.4 1 .2.1.3.2.5.3.6.4 1.2.9 1.7 1.3s1.1.9 1.6 1.4l.4.4 1.2 1.2.3.3c.5.5 1 1.1 1.4 1.6.1.1.2.3.3.4.4.4.7.9 1 1.4.1.2.2.3.3.5l1.2 1.8s0 .1.1.1a36.18 36.18 0 015.1 18.5c0 6-1.5 11.7-4.1 16.7-.3.6-.7 1.2-1 1.8 0 0 0 .1-.1.1l-1.2 1.8c-.1.2-.2.3-.3.5-.3.5-.7.9-1 1.4-.1.1-.2.3-.3.4-.5.6-.9 1.1-1.4 1.6l-.3.3-1.2 1.2-.4.4c-.5.5-1 .9-1.6 1.4-.6.5-1.1.9-1.7 1.3-.2.1-.3.2-.5.3-.5.3-.9.7-1.4 1-.1.1-.3.2-.4.3-.6.4-1.2.7-1.9 1.1-.1.1-.3.1-.4.2-.5.3-1 .5-1.6.8l-.6.3c-.7.3-1.3.6-2 .8-.7.3-1.4.5-2.1.7-.2.1-.4.1-.6.2-.6.2-1.1.3-1.7.4-.2 0-.3.1-.5.1-.7.2-1.5.3-2.2.4-.2 0-.3 0-.5.1-.6.1-1.2.1-1.8.2h-.6c-.8 0-1.5.1-2.3.1s-1.5 0-2.3-.1h-.6c-.6 0-1.2-.1-1.8-.2-.2 0-.3 0-.5-.1-.7-.1-1.5-.2-2.2-.4-.2 0-.3-.1-.5-.1-.6-.1-1.2-.3-1.7-.4-.2-.1-.4-.1-.6-.2-.7-.2-1.4-.4-2.1-.7-.7-.2-1.3-.5-2-.8l-.6-.3c-.5-.2-1.1-.5-1.6-.8-.1-.1-.3-.1-.4-.2-.6-.3-1.3-.7-1.9-1.1-.1-.1-.3-.2-.4-.3-.5-.3-1-.6-1.4-1-.2-.1-.3-.2-.5-.3-.6-.4-1.2-.9-1.7-1.3s-1.1-.9-1.6-1.4l-.4-.4-1.2-1.2-.3-.3c-.5-.5-1-1.1-1.4-1.6-.1-.1-.2-.3-.3-.4-.4-.4-.7-.9-1-1.4-.1-.2-.2-.3-.3-.5l-1.2-1.8v-.1c-.4-.6-.7-1.2-1-1.8-2.6-5-4.1-10.7-4.1-16.7s1.5-11.7 4.1-16.7zM620 539v221c0 4.4 3.6 8 8 8h48c4.4 0 8-3.6 8-8V539c-10.1 3.3-20.8 5-32 5s-21.9-1.8-32-5zm64-198v-77c0-4.4-3.6-8-8-8h-48c-4.4 0-8 3.6-8 8v77c10.1-3.3 20.8-5 32-5s21.9 1.8 32 5zm-64 198c10.1 3.3 20.8 5 32 5s21.9-1.8 32-5c41.8-13.5 72-52.7 72-99s-30.2-85.5-72-99c-10.1-3.3-20.8-5-32-5s-21.9 1.8-32 5c-41.8 13.5-72 52.7-72 99s30.2 85.5 72 99zm.1-115.7c.3-.6.7-1.2 1-1.8v-.1l1.2-1.8c.1-.2.2-.3.3-.5.3-.5.7-.9 1-1.4.1-.1.2-.3.3-.4.5-.6.9-1.1 1.4-1.6l.3-.3 1.2-1.2.4-.4c.5-.5 1-.9 1.6-1.4.6-.5 1.1-.9 1.7-1.3.2-.1.3-.2.5-.3.5-.3.9-.7 1.4-1 .1-.1.3-.2.4-.3.6-.4 1.2-.7 1.9-1.1.1-.1.3-.1.4-.2.5-.3 1-.5 1.6-.8l.6-.3c.7-.3 1.3-.6 2-.8.7-.3 1.4-.5 2.1-.7.2-.1.4-.1.6-.2.6-.2 1.1-.3 1.7-.4.2 0 .3-.1.5-.1.7-.2 1.5-.3 2.2-.4.2 0 .3 0 .5-.1.6-.1 1.2-.1 1.8-.2h.6c.8 0 1.5-.1 2.3-.1s1.5 0 2.3.1h.6c.6 0 1.2.1 1.8.2.2 0 .3 0 .5.1.7.1 1.5.2 2.2.4.2 0 .3.1.5.1.6.1 1.2.3 1.7.4.2.1.4.1.6.2.7.2 1.4.4 2.1.7.7.2 1.3.5 2 .8l.6.3c.5.2 1.1.5 1.6.8.1.1.3.1.4.2.6.3 1.3.7 1.9 1.1.1.1.3.2.4.3.5.3 1 .6 1.4 1 .2.1.3.2.5.3.6.4 1.2.9 1.7 1.3s1.1.9 1.6 1.4l.4.4 1.2 1.2.3.3c.5.5 1 1.1 1.4 1.6.1.1.2.3.3.4.4.4.7.9 1 1.4.1.2.2.3.3.5l1.2 1.8v.1a36.18 36.18 0 015.1 18.5c0 6-1.5 11.7-4.1 16.7-.3.6-.7 1.2-1 1.8v.1l-1.2 1.8c-.1.2-.2.3-.3.5-.3.5-.7.9-1 1.4-.1.1-.2.3-.3.4-.5.6-.9 1.1-1.4 1.6l-.3.3-1.2 1.2-.4.4c-.5.5-1 .9-1.6 1.4-.6.5-1.1.9-1.7 1.3-.2.1-.3.2-.5.3-.5.3-.9.7-1.4 1-.1.1-.3.2-.4.3-.6.4-1.2.7-1.9 1.1-.1.1-.3.1-.4.2-.5.3-1 .5-1.6.8l-.6.3c-.7.3-1.3.6-2 .8-.7.3-1.4.5-2.1.7-.2.1-.4.1-.6.2-.6.2-1.1.3-1.7.4-.2 0-.3.1-.5.1-.7.2-1.5.3-2.2.4-.2 0-.3 0-.5.1-.6.1-1.2.1-1.8.2h-.6c-.8 0-1.5.1-2.3.1s-1.5 0-2.3-.1h-.6c-.6 0-1.2-.1-1.8-.2-.2 0-.3 0-.5-.1-.7-.1-1.5-.2-2.2-.4-.2 0-.3-.1-.5-.1-.6-.1-1.2-.3-1.7-.4-.2-.1-.4-.1-.6-.2-.7-.2-1.4-.4-2.1-.7-.7-.2-1.3-.5-2-.8l-.6-.3c-.5-.2-1.1-.5-1.6-.8-.1-.1-.3-.1-.4-.2-.6-.3-1.3-.7-1.9-1.1-.1-.1-.3-.2-.4-.3-.5-.3-1-.6-1.4-1-.2-.1-.3-.2-.5-.3-.6-.4-1.2-.9-1.7-1.3s-1.1-.9-1.6-1.4l-.4-.4-1.2-1.2-.3-.3c-.5-.5-1-1.1-1.4-1.6-.1-.1-.2-.3-.3-.4-.4-.4-.7-.9-1-1.4-.1-.2-.2-.3-.3-.5l-1.2-1.8v-.1c-.4-.6-.7-1.2-1-1.8-2.6-5-4.1-10.7-4.1-16.7s1.5-11.7 4.1-16.7z"></path>`;
// const searchNode = (graph) => {
//   const toolBarSearchInput = document.getElementById('toolBarSearchInput') as HTMLInputElement;
//   const searchText = toolBarSearchInput.value.trim();
//   let lastFoundNode = null;
//   graph.getNodes().forEach((node) => {
//     const model = node.getModel();
//     const isFound = searchText && model.label.includes(searchText);
//     if (isFound) {
//       graph.setItemState(node, 'active', true);
//       lastFoundNode = node;
//     } else {
//       graph.setItemState(node, 'active', false);
//     }
//   });

//   if (lastFoundNode) {
//     // 将数据集移动到找到的节点位置
//     graph.focusItem(lastFoundNode, true, {
//       duration: 300,
//       easing: 'easeCubic',
//     });
//   }
// };

const searchNode = (graph: Graph, onSearch?: ToolBarSearchCallBack) => {
  const toolBarSearchInput = document.getElementById('toolBarSearchInput') as HTMLInputElement;
  const searchText = toolBarSearchInput.value.trim();
  onSearch?.(searchText);
};

const generatorSearchInputDom = (graph: Graph, onSearch: ToolBarSearchCallBack) => {
  const domString =
    '<input placeholder="请输入指标/维度名称" class="ant-input" id="toolBarSearchInput" type="text" value="" />';
  const searchInputDom = createDom(domString);
  searchInputDom.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
      searchNode(graph, onSearch);
    }
  });
  return searchInputDom;
};

const generatorSearchBtnDom = (graph: Graph, onSearch: ToolBarSearchCallBack) => {
  const domString = `<button
  id="toolBarSearchBtn"
  type="button"
  class="ant-btn ant-btn-default ant-btn-icon-only ant-input-search-button"
>
  <span role="img" aria-label="search" class="anticon anticon-search">
    <svg
      viewBox="64 64 896 896"
      focusable="false"
      data-icon="search"
      width="1em"
      height="1em"
      fill="currentColor"
      aria-hidden="true"
    >
      ${searchIconSvgPath}
    </svg>
  </span>
</button>`;
  const searchBtnDom = createDom(domString);
  searchBtnDom.addEventListener('click', () => {
    searchNode(graph, onSearch);
  });
  return searchBtnDom;
};

const searchInputDOM = (graph: Graph, onSearch: ToolBarSearchCallBack) => {
  const searchInputDom = generatorSearchInputDom(graph, onSearch);
  const searchBtnDom = generatorSearchBtnDom(graph, onSearch);
  const searchInput = `
  <div id="searchInputContent" class="g6-component-toolbar-search-input" style="position: absolute;top: 38px;width: 190px;left: 0;">
    <span class="ant-input-group-wrapper ant-input-search" >
      <span class="ant-input-wrapper ant-input-group" id="toolBarSearchWrapper">
        <span class="ant-input-group-addon"></span>
      </span>
    </span>
  </div>`;
  const searchDom = createDom(searchInput);
  const searchWrapperDom = searchDom.querySelector('#toolBarSearchWrapper');
  searchWrapperDom.insertBefore(searchInputDom, searchWrapperDom.firstChild);
  searchWrapperDom.querySelector('.ant-input-group-addon').appendChild(searchBtnDom);
  return searchDom;
};

function zoomGraph(graph, ratio) {
  const width = graph.get('width');
  const height = graph.get('height');
  const centerX = width / 2;
  const centerY = height / 2;
  graph.zoom(ratio, { x: centerX, y: centerY });
}

const initToolBar = ({
  onSearch,
  onClick,
}: {
  onSearch: ToolBarSearchCallBack;
  onClick?: (code: string, graph: IGraph) => void;
}) => {
  const toolBarInstance = new G6.ToolBar();
  const config = toolBarInstance._cfgs;
  const defaultContentDomString = config.getContent();
  const defaultContentDom = createDom(defaultContentDomString);
  // @ts-ignore
  const elements = defaultContentDom.querySelectorAll(
    'li[code="redo"], li[code="undo"], li[code="realZoom"]',
  );
  elements.forEach((element) => {
    element.remove();
  });
  const searchBtnDom = `<li code="search">
  <svg
    viewBox="64 64 896 896"
    class="icon"
    data-icon="search"
    width="24"
    height="24"
    fill="currentColor"
    aria-hidden="true"
  >
    ${searchIconSvgPath}
  </svg>
</li>`;
  const visibleBtnDom = `<li code="visibleMode">
  <svg
    viewBox="64 64 896 896"
    class="icon"
    data-icon="visibleMode"
    width="24"
    height="24"
    fill="currentColor"
    aria-hidden="true"
  >
    ${visibleModeIconSvgPath}
  </svg>
  </li>`;

  defaultContentDom.insertAdjacentHTML('afterbegin', `${searchBtnDom}${visibleBtnDom}`);
  let searchInputContentVisible = true;
  const toolbar = new G6.ToolBar({
    position: { x: 20, y: 20 },
    className: 'semantic-graph-toolbar',
    getContent: (graph) => {
      const searchInput = searchInputDOM(graph as Graph, onSearch);
      const content = `<div class="g6-component-toolbar-content">${defaultContentDom.outerHTML}</div>`;
      const contentDom = createDom(content);
      contentDom.addEventListener('click', (event: PointerEvent) => {
        event.preventDefault();
        event.stopPropagation();
        return false;
      });
      contentDom.addEventListener('dblclick', (event: PointerEvent) => {
        event.preventDefault();
        event.stopPropagation();
        return false;
      });
      contentDom.appendChild(searchInput);
      return contentDom;
    },
    handleClick: (code, graph) => {
      if (code === 'search') {
        const searchText = document.getElementById('searchInputContent');
        if (searchText) {
          const visible = searchInputContentVisible ? 'none' : 'block';
          searchText.style.display = visible;
          searchInputContentVisible = !searchInputContentVisible;
        }
      } else if (code === 'visibleMode') {
        const searchText = document.getElementById('searchInputContent');
        if (searchText) {
          const visible = 'none';
          searchText.style.display = visible;
          searchInputContentVisible = false;
        }
      } else if (code.includes('zoom')) {
        const sensitivity = 0.1; // 设置缩放灵敏度，值越小，缩放越不敏感，默认值为 1
        const zoomInRatio = 1 - sensitivity;
        const zoomOutRatio = 1 + sensitivity;

        if (code === 'zoomIn') {
          zoomGraph(graph, zoomInRatio);
        } else if (code === 'zoomOut') {
          zoomGraph(graph, zoomOutRatio);
        }
        // else if (code === 'realZoom') {
        //   const width = graph.get('width');
        //   const height = graph.get('height');
        //   const centerX = width / 2;
        //   const centerY = height / 2;
        //   graph.moveTo(centerX, centerY);
        //   graph.zoomTo(1, { x: centerX, y: centerY });
        // } else if (code === 'autoZoom') {
        //   const width = graph.get('width');
        //   const height = graph.get('height');
        //   const centerX = width / 2;
        //   const centerY = height / 2;
        //   const centerModel = graph.getPointByCanvas(centerX, centerY);

        //   // 调用 fitView
        //   graph.fitView();

        //   // 在 fitView 之后获取新的画布中心点
        //   const newCenterCanvas = graph.getCanvasByPoint(centerModel.x, centerModel.y);

        //   // 计算并调整画布的偏移量，使得画布中心点保持不变
        //   const dx = centerX - newCenterCanvas.x;
        //   const dy = centerY - newCenterCanvas.y;
        //   graph.translate(dx, dy);
        // }
      } else {
        // handleDefaultOperator public方法缺失graph作为参数传入，将graph挂载在cfgs上，源码通过get会获取到graph,完成默认code的执行逻辑
        toolBarInstance._cfgs.graph = graph;
        toolBarInstance.handleDefaultOperator(code);
      }
      onClick?.(code, graph);
    },
  });

  return toolbar;
};
export default initToolBar;

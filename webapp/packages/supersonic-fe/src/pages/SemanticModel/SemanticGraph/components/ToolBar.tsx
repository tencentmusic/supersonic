import G6, { Graph } from '@antv/g6';
import { createDom } from '@antv/dom-util';
import { RefreshGraphData } from '../../data';
const searchIconSvgPath = `<path d="M909.6 854.5L649.9 594.8C690.2 542.7 712 479 712 412c0-80.2-31.3-155.4-87.9-212.1-56.6-56.7-132-87.9-212.1-87.9s-155.5 31.3-212.1 87.9C143.2 256.5 112 331.8 112 412c0 80.1 31.3 155.5 87.9 212.1C256.5 680.8 331.8 712 412 712c67 0 130.6-21.8 182.7-62l259.7 259.6a8.2 8.2 0 0011.6 0l43.6-43.5a8.2 8.2 0 000-11.6zM570.4 570.4C528 612.7 471.8 636 412 636s-116-23.3-158.4-65.6C211.3 528 188 471.8 188 412s23.3-116.1 65.6-158.4C296 211.3 352.2 188 412 188s116.1 23.2 158.4 65.6S636 352.2 636 412s-23.3 116.1-65.6 158.4z" />`;

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
//     // 将视图移动到找到的节点位置
//     graph.focusItem(lastFoundNode, true, {
//       duration: 300,
//       easing: 'easeCubic',
//     });
//   }
// };

interface Node {
  label: string;
  children?: Node[];
}

function findNodesByLabel(query: string, nodes: Node[]): Node[] {
  const result: Node[] = [];

  for (const node of nodes) {
    let match = false;
    let children: Node[] = [];

    // 如果节点的label包含查询字符串，我们将其标记为匹配
    if (node.label.includes(query)) {
      match = true;
    }

    // 我们还需要在子节点中进行搜索
    if (node.children) {
      children = findNodesByLabel(query, node.children);
      if (children.length > 0) {
        match = true;
      }
    }

    // 如果节点匹配或者其子节点匹配，我们将其添加到结果中
    if (match) {
      result.push({ ...node, children });
    }
  }

  return result;
}

const searchNode = (graph: Graph, refreshGraphData?: RefreshGraphData) => {
  const toolBarSearchInput = document.getElementById('toolBarSearchInput') as HTMLInputElement;
  const searchText = toolBarSearchInput.value.trim();
  const graphData = graph.get('initGraphData');
  const filterChildrenData = findNodesByLabel(searchText, graphData.children);
  refreshGraphData?.({
    ...graphData,
    children: filterChildrenData,
  });
};

const generatorSearchInputDom = (graph: Graph, refreshGraphData: RefreshGraphData) => {
  const domString =
    '<input placeholder="请输入指标/维度名称" class="ant-input" id="toolBarSearchInput" type="text" value="" />';
  const searchInputDom = createDom(domString);
  searchInputDom.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
      searchNode(graph, refreshGraphData);
    }
  });
  return searchInputDom;
};

const generatorSearchBtnDom = (graph: Graph) => {
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
    searchNode(graph);
  });
  return searchBtnDom;
};

const searchInputDOM = (graph: Graph, refreshGraphData: RefreshGraphData) => {
  const searchInputDom = generatorSearchInputDom(graph, refreshGraphData);
  const searchBtnDom = generatorSearchBtnDom(graph);
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

const initToolBar = ({ refreshGraphData }: { refreshGraphData: RefreshGraphData }) => {
  const toolBarInstance = new G6.ToolBar();
  const config = toolBarInstance._cfgs;
  const defaultContentDomString = config.getContent();
  const defaultContentDom = createDom(defaultContentDomString);
  // @ts-ignore
  const elements = defaultContentDom.querySelectorAll('li[code="redo"], li[code="undo"]');
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
  defaultContentDom.insertAdjacentHTML('afterbegin', searchBtnDom);
  let searchInputContentVisible = true;
  const toolbar = new G6.ToolBar({
    position: { x: 10, y: 10 },
    className: 'semantic-graph-toolbar',
    getContent: (graph) => {
      const searchInput = searchInputDOM(graph as Graph, refreshGraphData);
      const content = `<div class="g6-component-toolbar-content">${defaultContentDom.outerHTML}</div>`;
      const contentDom = createDom(content);
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
      } else {
        // handleDefaultOperator public方法缺失graph作为参数传入，将graph挂载在cfgs上，源码通过get会获取到graph,完成默认code的执行逻辑
        toolBarInstance._cfgs.graph = graph;
        toolBarInstance.handleDefaultOperator(code);
      }
    },
  });

  return toolbar;
};
export default initToolBar;

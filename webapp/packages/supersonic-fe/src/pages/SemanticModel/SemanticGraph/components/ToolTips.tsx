import G6 from '@antv/g6';
import moment from 'moment';

const initTooltips = () => {
  const tooltip = new G6.Tooltip({
    offsetX: 10,
    offsetY: 10,
    fixToNode: [1, 0.5],
    // 允许出现 tooltip 的 item 类型
    itemTypes: ['node', 'edge'],
    shouldBegin: (e) => {
      const model = e!.item!.getModel();
      const eleType = e!.item!.getType();
      if (eleType === 'node' || (eleType === 'edge' && model.sourceAnchor)) {
        return true;
      }
      return false;
    },
    // 自定义 tooltip 内容
    getContent: (e) => {
      const eleType = e!.item!.getType();
      const outDiv = document.createElement('div');
      outDiv.style.width = 'fit-content';
      outDiv.style.height = 'fit-content';
      const model = e!.item!.getModel();
      const { name, bizName, createdBy, updatedAt, description, sourceAnchor } = model;
      if (eleType === 'edge' && sourceAnchor) {
        return '点击编辑模型关系';
      }
      const list = [
        {
          label: '名称:',
          value: name,
        },
        {
          label: '字段:',
          value: bizName,
        },
        {
          label: '创建人:',
          value: createdBy,
        },
        {
          label: '更新时间:',
          value: updatedAt ? moment(updatedAt).format('YYYY-MM-DD HH:mm:ss') : '',
        },
        {
          label: '描述:',
          value: description,
        },
      ];
      const listHtml = list.reduce((htmlString, item) => {
        const { label, value } = item;
        if (value) {
          htmlString += `<p style="margin-bottom:0;margin-top:0">
          <span>${label} </span>
          <span>${value}</span>
        </p>`;
        }
        return htmlString;
      }, '');
      const html = `<div>
      ${listHtml}
    </div>`;
      if (e!.item!.getType() === 'node') {
        outDiv.innerHTML = html;
      }
      return outDiv;
    },
  });

  return tooltip;
};
export default initTooltips;

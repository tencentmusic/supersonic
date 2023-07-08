import G6 from '@antv/g6';
import moment from 'moment';

const initTooltips = () => {
  const tooltip = new G6.Tooltip({
    offsetX: 10,
    offsetY: 10,
    fixToNode: [1, 0.5],
    // the types of items that allow the tooltip show up
    // 允许出现 tooltip 的 item 类型
    // itemTypes: ['node', 'edge'],
    itemTypes: ['node'],
    // custom the tooltip's content
    // 自定义 tooltip 内容
    getContent: (e) => {
      const outDiv = document.createElement('div');
      outDiv.style.width = 'fit-content';
      outDiv.style.height = 'fit-content';
      const model = e.item.getModel();

      const { name, bizName, createdBy, updatedAt, description } = model;
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
          htmlString += `<p style="margin-bottom:0">
          <span>${label} </span>
          <span>${value}</span>
        </p>`;
        }
        return htmlString;
      }, '');
      const html = `<div>
      ${listHtml}
    </div>`;
      if (e.item.getType() === 'node') {
        outDiv.innerHTML = html;
      }
      //  else {
      // const source = e.item.getSource();
      // const target = e.item.getTarget();
      // outDiv.innerHTML = `来源：${source.getModel().name}<br/>去向：${
      //   target.getModel().name
      // }`;
      // }
      return outDiv;
    },
  });

  return tooltip;
};
export default initTooltips;

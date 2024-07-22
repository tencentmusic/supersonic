import { ClearOutlined, PlusOutlined } from '@ant-design/icons';
import { Button, Space } from 'antd';
import { useConditionStore } from './Context';

export default function ConditionFooter() {
  const { addCondition, addColumn, addTopN, clearAll } = useConditionStore();

  return (
    <Space className="condition-footer">
      <Button type="link" size="small" icon={<PlusOutlined />} onClick={addCondition}>
        添加过滤
      </Button>
      <Button type="link" size="small" icon={<PlusOutlined />} onClick={addColumn}>
        添加列
      </Button>
      <Button type="link" size="small" icon={<PlusOutlined />} onClick={addTopN}>
        TopN
      </Button>
      <Button type="link" size="small" icon={<ClearOutlined />} onClick={clearAll}>
        一键清空
      </Button>
    </Space>
  );
}

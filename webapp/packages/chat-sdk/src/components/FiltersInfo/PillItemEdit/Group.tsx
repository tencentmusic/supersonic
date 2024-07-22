import { DeleteOutlined } from '@ant-design/icons';
import { Button, Flex, Space, Tooltip } from 'antd';
import Select from '../components/Select';
import { useFieldOptions } from '../hooks/useFieldOptions';
import { IGroupPill } from '../types';

type Props = {
  value: IGroupPill;
  onChange: (value: IGroupPill) => void;
};

export default function Group({ value, onChange }: Props) {
  const options = useFieldOptions();

  const handleFieldSelect = (field: string) => v => {
    onChange({
      ...value,
      fields: value.fields.map(f => {
        if (f.field === field) {
          return { ...f, field: v, fieldName: options.find(o => o.value === v)?.label ?? '' };
        }
        return f;
      }),
    });
  };

  const onClickDelete = (field: string) => {
    onChange({
      ...value,
      fields: value.fields.filter(f => f.field !== field),
    });
  };

  return (
    <Space direction="vertical">
      {value.fields.map(field => {
        return (
          <Flex gap={'small'} justify="space-between" align="center">
            <Select
              value={field.field}
              key={field.field}
              style={{ width: 120 }}
              options={options}
              optionFilterProp="label"
              onChange={handleFieldSelect(field.field)}
            />
            {value.fields.length > 1 && (
              <Tooltip title="删除">
                <Button
                  className="condition-item-remove-btn"
                  type="link"
                  size="small"
                  icon={<DeleteOutlined />}
                  onClick={e => {
                    e.stopPropagation();
                    onClickDelete(field.field);
                  }}
                />
              </Tooltip>
            )}
          </Flex>
        );
      })}
    </Space>
  );
}

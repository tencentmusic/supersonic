import { PlusOutlined } from '@ant-design/icons';
import { Button, Divider, Input, InputRef, Space } from 'antd';
import { ComponentProps, useMemo, useRef, useState } from 'react';
import { uuid } from '../../../utils/utils';
import Select from './Select';

// dropdown带遮罩层的Select， 用于解决Select组件在弹出层中的问题
export default function SelectWithCustomOption(
  props: Omit<ComponentProps<typeof Select>, 'dropdownRender'>
) {
  const { options, ...otherProps } = props;

  const [id] = useState(() => 'select-with-custom-option' + uuid());

  const [customOptions, setCustomOptions] = useState<{ label: string; value: string }[]>([]);

  const [name, setName] = useState('');

  const inputRef = useRef<InputRef>(null);

  const finalOptions = useMemo(() => {
    return [...(options ?? []), ...customOptions];
  }, [options, customOptions]);

  const onNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setName(event.target.value);
  };

  const addItem = (e: React.MouseEvent<HTMLButtonElement | HTMLAnchorElement>) => {
    e.preventDefault();
    setCustomOptions([...customOptions, { label: name, value: name }]);
    setName('');
    setTimeout(() => {
      inputRef.current?.focus();
      // menu滚动到底部
      const menu = document.querySelector(`.${id} .rc-virtual-list-holder`);
      if (menu) {
        menu.scrollTop = menu.scrollHeight;
      }
    }, 10);
  };

  return (
    <Select
      options={finalOptions}
      className={id}
      dropdownRender={menu => (
        <>
          {menu}
          <Divider style={{ margin: '8px 0' }} />
          <Space style={{ padding: '0 8px 4px' }}>
            <Input
              placeholder="请输入"
              ref={inputRef}
              value={name}
              onChange={onNameChange}
              onKeyDown={e => e.stopPropagation()}
            />
            <Button type="text" icon={<PlusOutlined />} onClick={addItem}>
              新增
            </Button>
          </Space>
        </>
      )}
      {...otherProps}
    />
  );
}

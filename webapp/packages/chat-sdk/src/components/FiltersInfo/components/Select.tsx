import { SelectProps, Select as SelectBase } from 'antd';
import { useBoolean } from 'ahooks';
import { useRef } from 'react';
import { useDropDownHandler } from '../components/DropDownContext';
import { getDropdownHtmlElement } from '../utils';

function useDropdown() {
  const [open, { set: setOpen }] = useBoolean(false);

  const containerRef = useRef<HTMLElement>();

  const dropDownHandler = useDropDownHandler();

  const muskRef = useRef<HTMLDivElement>();

  const handleDropdown = async (isOnDropdown: boolean) => {
    setOpen(isOnDropdown);
    dropDownHandler(isOnDropdown);
    // 创建一个遮罩层，防止点击穿透
    if (isOnDropdown) {
      const dropdownElement = await getDropdownHtmlElement(containerRef.current!);
      const zIndex = dropdownElement?.style?.zIndex;

      const mask = document.createElement('div');
      mask.style.position = 'fixed';
      mask.style.top = '0';
      mask.style.left = '0';
      mask.style.right = '0';
      mask.style.bottom = '0';
      mask.style.cursor = 'default';
      mask.style.zIndex = Number(zIndex) - 1 + '';
      containerRef.current!.appendChild(mask);
      muskRef.current = mask;
      mask.addEventListener('click', () => {
        setOpen(false);
        dropDownHandler(false);
        mask.remove();
      });
    } else {
      muskRef.current?.remove();
    }
  };

  const getPopupContainer = triggerNode => {
    const container = triggerNode.parentNode || document.body;
    containerRef.current = container as HTMLDivElement;
    return container;
  };

  return {
    open,
    setOpen: handleDropdown,
    getPopupContainer,
  };
}

// dropdown带遮罩层的Select， 用于解决Select组件在弹出层中的问题
export default function Select(props: Omit<SelectProps, 'open' | 'getPopupContainer'>) {
  const { onDropdownVisibleChange = () => {}, ...otherProps } = props;
  const { open, setOpen, getPopupContainer } = useDropdown();

  const handleDropdownVisibleChange = (visible: boolean) => {
    setOpen(visible);
    onDropdownVisibleChange(visible);
  };

  return (
    <SelectBase
      open={open}
      onDropdownVisibleChange={handleDropdownVisibleChange}
      getPopupContainer={getPopupContainer}
      placeholder="请选择"
      {...otherProps}
    />
  );
}

Select.displayName = SelectBase.displayName ?? 'Select';
Select.Option = SelectBase.Option;
Select.OptGroup = SelectBase.OptGroup;
Select.SECRET_COMBOBOX_MODE_DO_NOT_USE = SelectBase.SECRET_COMBOBOX_MODE_DO_NOT_USE;
Select._InternalPanelDoNotUseOrYouWillBeFired = SelectBase._InternalPanelDoNotUseOrYouWillBeFired;

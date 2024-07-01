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

  let mouseMoveHandlerRef = useRef(e => {});

  const handleDropdown = async (isOnDropdown: boolean) => {
    setOpen(isOnDropdown);
    dropDownHandler(isOnDropdown);

    // 创建一个遮罩层，防止点击穿透
    if (isOnDropdown) {
      const dropdownElement = await getDropdownHtmlElement(containerRef.current!);
      const { left, top, right, bottom } = containerRef.current!.getBoundingClientRect();

      const zIndex = dropdownElement?.style?.zIndex;

      const mask = document.createElement('div');
      mask.style.position = 'fixed';
      mask.style.top = '0';
      mask.style.left = '0';
      mask.style.right = '0';
      mask.style.bottom = '0';
      mask.style.cursor = 'default';
      // mask.style.pointerEvents = 'none';
      mask.style.zIndex = Number(zIndex) - 1 + '';
      containerRef.current!.appendChild(mask);
      muskRef.current = mask;
      mask.addEventListener('click', () => {
        setOpen(false);
        dropDownHandler(false);
        mask.remove();
      });

      mouseMoveHandlerRef.current = e => {
        console.log('🚀 ~ handleDropdown ~ e:', e);
        // 判断鼠标位置是否在dropdownElement内
        if (dropdownElement) {
          if (e.clientX < left || e.clientX > right || e.clientY < top || e.clientY > bottom) {
            mask.style.pointerEvents = 'auto';
          } else {
            mask.style.pointerEvents = 'none';
          }
        }
      };

      document.addEventListener('mousemove', mouseMoveHandlerRef.current);
    } else {
      muskRef.current?.remove();
      document.removeEventListener('mousemove', mouseMoveHandlerRef.current);
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
      showSearch
      getPopupContainer={getPopupContainer}
      placeholder="请选择"
      {...otherProps}
    />
  );
}

Select.displayName = SelectBase.displayName ?? 'Select';
Select.Option = SelectBase.Option;
Select.OptGroup = SelectBase.OptGroup;

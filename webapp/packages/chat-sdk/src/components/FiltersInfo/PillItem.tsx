import classNames from 'classnames';
import { memo, useCallback, useRef } from 'react';
import { useBoolean, useClickAway, useHover } from 'ahooks';
import { useContextSelector } from 'use-context-selector';
import { FilterInfosContext } from '.';
import { Popconfirm, Popover, Tooltip } from 'antd';
import PillItemEdit from './PillItemEdit';
import { getPillTitle, getPillTitleForTooltip } from './utils';
import DropDownHandlerProvider from './components/DropDownContext';

type Props = {
  idx: number;
};

function Pill({ idx }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const isHovering = useHover(ref);

  const [isOpenRemoveConfrim, { set: setIsOpenRemoveConfirm }] = useBoolean(false);
  const isOnDropdownRef = useRef(false);

  const { updatePillItem, removePillItem, data, existItemSelected } = useContextSelector(
    FilterInfosContext,
    context => ({
      updatePillItem: context.updatePillItem,
      removePillItem: context.removePillItem,
      getPillKey: context.getPillKey,
      data: context.getPill(idx),
      existItemSelected: context.existItemSelected,
    })
  );

  const disableDeleteIcon = data.type === 'group' || data.type === 'aggregation';

  const showDeleteIcon =
    !disableDeleteIcon && ((isHovering && !existItemSelected) || isOpenRemoveConfrim);

  const handleSelect = (selected: boolean) => {
    if (isOpenRemoveConfrim || isOnDropdownRef.current) return;
    if (data.selected === selected) return;
    updatePillItem(idx, {
      ...data,
      selected,
    });
  };

  const focus = () => handleSelect(true);

  const blur = () => handleSelect(false);

  useClickAway(blur, ref);

  const handleRemove = (e: React.MouseEvent) => {
    e.stopPropagation();
  };

  const removeConfirm = (e: React.MouseEvent<HTMLElement, MouseEvent> | undefined) => {
    e?.stopPropagation();
    removePillItem(idx);
  };

  const removeCancel = (e: React.MouseEvent<HTMLElement, MouseEvent> | undefined) => {
    e?.stopPropagation();
    setIsOpenRemoveConfirm(false);
  };

  const handleDropdown = useCallback((isOnDropdown: boolean) => {
    if (isOnDropdown) {
      isOnDropdownRef.current = true;
    } else {
      // 触发mouseleave事件，解决下拉框隐藏时，hover状态异常导致删除按钮不消失的问题
      const mouseleaveEvent = new MouseEvent('mouseleave', {
        view: window,
        bubbles: true,
        cancelable: true,
      });
      ref.current?.dispatchEvent(mouseleaveEvent);

      // 延迟设置，卡时间，防止在下拉框隐藏时，同时触发被选中
      setTimeout(() => {
        isOnDropdownRef.current = false;
      }, 300);
    }
  }, []);

  return (
    <div
      ref={ref}
      className={classNames(['pill-item', data.type, data.selected ? 'selected' : ''])}
      onClick={focus}
    >
      <Popover
        placement="bottomLeft"
        content={
          <DropDownHandlerProvider value={handleDropdown}>
            <PillItemEdit idx={idx} />
          </DropDownHandlerProvider>
        }
        open={!!data.selected}
        getPopupContainer={() => ref.current || document.body}
        arrow={false}
        destroyTooltipOnHide
        align={{
          offset: [-10, 10],
        }}
      >
        <Tooltip title={getPillTitleForTooltip(data)}>{getPillTitle(data)}</Tooltip>
      </Popover>
      <Popconfirm
        title="确定删除该查询条件？"
        description={`删除 "${getPillTitle(data)}" 查询条件`}
        onConfirm={removeConfirm}
        onCancel={removeCancel}
        open={isOpenRemoveConfrim}
        onOpenChange={setIsOpenRemoveConfirm}
        okText="确定"
        cancelText="取消"
        getPopupContainer={() => ref.current || document.body}
        destroyTooltipOnHide
      >
        {showDeleteIcon && (
          <div onClick={handleRemove} className="pill-item-close">
            &times;
          </div>
        )}
      </Popconfirm>
    </div>
  );
}

export default memo(Pill);

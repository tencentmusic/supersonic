import { PlusOutlined } from '@ant-design/icons';
import { useBoolean } from 'ahooks';
import { Button, Divider, Popover, Tooltip } from 'antd';
import { cloneDeep } from 'lodash';
import { memo, useRef } from 'react';
import { useContextSelector } from 'use-context-selector';
import { FilterInfosContext } from '.';
import { uuid } from '../../utils/utils';
import ConditionPanel from './ConditionPanel';
import { IConditionListHandle } from './ConditionPanel/ConditionList';
import { useDatasetInfo } from './hooks/useDatasetInfo';
import {
  ColumnCondition,
  FilterCondition,
  IAggregationPill,
  IGroupPill,
  INumberFilterPill,
  IStringColumnCondition,
  ITextFilterPill,
  ITopNPill,
} from './types';

function isValidFilter(condition: FilterCondition) {
  if (!condition.field) return false;

  if (!condition.operator) return false;

  if (condition.fieldType === 'number') {
    return !!condition.value || condition.value === 0;
  }

  if (condition.fieldType === 'string') {
    return !!condition.value;
  }

  return false;
}

function isValidColumn(condition: ColumnCondition) {
  if (!condition.field) return false;

  if (!condition.operator) return false;

  if (condition.operator === 'group') {
    return true;
  }

  if (condition.operator === 'agg') {
    return !!condition.value;
  }

  return false;
}

type Props = {
  onConfirm: () => void;
  onCancel: () => void;
};

function Actions({ onConfirm, onCancel }: Props) {
  const [open, { set: setOpen }] = useBoolean(false);

  const { resetList, pillData } = useContextSelector(FilterInfosContext, context => context);

  const conditionListRef = useRef<IConditionListHandle>(null);

  const { getFieldInfo } = useDatasetInfo();

  const handleAdd = () => {
    setOpen(true);
  };

  const handleOpenChange = (open: boolean) => {
    setOpen(open);
    if (!open) {
      const conditions = conditionListRef.current?.getConditions();
      const cloneData = cloneDeep(pillData);
      conditions?.forEach(condition => {
        if (condition.type === 'filter' && isValidFilter(condition)) {
          if (condition.fieldType === 'string') {
            const fieldInfo = getFieldInfo(condition.field!);
            const textPill: ITextFilterPill = {
              id: condition.field!,
              selected: false,
              type: 'text-filter',
              field: condition.field!,
              fieldId: fieldInfo.id,
              fieldName: fieldInfo.name,
              operator: condition.operator!,
              value: condition.value as string,
            };

            // 判断是否已经存在，存在则替换，不存在则插入
            const index = cloneData.findIndex(
              item => item.type === 'text-filter' && item.field === condition.field
            );

            if (index > -1) {
              cloneData[index] = textPill;
            } else {
              cloneData.push(textPill);
            }
          }

          if (condition.fieldType === 'number') {
            const fieldInfo = getFieldInfo(condition.field!);
            const numberPill: INumberFilterPill = {
              id: condition.field!,
              selected: false,
              type: 'number-filter',
              field: condition.field!,
              fieldName: fieldInfo.name,
              operator: condition.operator!,
              value: condition.value as number,
            };

            // 判断是否已经存在，存在则替换，不存在则插入
            const index = cloneData.findIndex(
              item => item.type === 'number-filter' && item.field === condition.field
            );

            if (index > -1) {
              cloneData[index] = numberPill;
            } else {
              cloneData.push(numberPill);
            }
          }

          // TODO: 日期类型
          if (condition.fieldType === 'date') {
          }
        }

        if (condition.type === 'column' && isValidColumn(condition)) {
          const fieldInfo = getFieldInfo(condition.field!);

          if (condition.operator === 'group') {
            const groupPill: IGroupPill = (cloneData.find(
              item => item.type === 'group'
            ) as IGroupPill) ?? {
              id: 'group',
              selected: false,
              type: 'group',
              fields: [
                {
                  field: condition.field!,
                  fieldName: fieldInfo.name,
                },
              ],
            };

            const index = cloneData.findIndex(item => item.type === 'group');

            if (index > -1) {
              cloneData[index] = groupPill;
            } else {
              cloneData.push(groupPill);
            }
          }

          if (condition.operator === 'agg') {
            const aggPill: IAggregationPill = (cloneData.find(
              item => item.type === 'aggregation'
            ) as IAggregationPill) ?? {
              id: 'aggregation',
              selected: false,
              type: 'aggregation',
              fields: [
                {
                  field: condition.field!,
                  fieldName: fieldInfo.name,
                  operator: condition.value as string,
                },
              ],
            };

            const index = cloneData.findIndex(item => item.type === 'aggregation');

            if (index > -1) {
              cloneData[index] = aggPill;
            } else {
              cloneData.push(aggPill);
            }
          }
        }

        if (condition.type === 'topN' && condition.value) {
          const topNPill: ITopNPill = {
            id: 'topN',
            selected: false,
            type: 'top-n',
            value: condition.value as number,
          };

          const index = cloneData.findIndex(item => item.type === 'top-n');

          if (index > -1) {
            cloneData[index] = topNPill;
          } else {
            cloneData.push(topNPill);
          }
        }
      });

      // 排序
      const order = [
        'text-filter',
        'number-filter',
        'date-filter',
        'group',
        'aggregation',
        'top-n',
      ];
      cloneData.sort((a, b) => {
        return order.indexOf(a.type) - order.indexOf(b.type);
      });

      resetList(cloneData);
    }
  };

  return (
    <div>
      <Popover
        placement="bottom"
        trigger={['click']}
        content={<ConditionPanel listRef={conditionListRef} />}
        open={open}
        getPopupContainer={triggerNode => triggerNode.parentElement || document.body}
        arrow={false}
        destroyTooltipOnHide
        onOpenChange={handleOpenChange}
        align={{
          offset: [0, 10],
        }}
      >
        <Tooltip title="添加查询条件">
          <Button type="dashed" size="small" onClick={handleAdd} icon={<PlusOutlined />}></Button>
        </Tooltip>
      </Popover>
      <Divider type="vertical" />
      <Button type="link" size="small" onClick={onConfirm}>
        确定
      </Button>
      <Button type="text" size="small" onClick={onCancel}>
        取消
      </Button>
    </div>
  );
}

export default memo(Actions);

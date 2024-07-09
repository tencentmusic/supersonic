import { message } from 'antd';
import { memo } from 'react';
import { useContextSelector } from 'use-context-selector';
import { FilterInfosContext } from '.';
import Actions from './Actions';
import { useDatasetInfo } from './hooks/useDatasetInfo';
import PillList from './PillList';
import { IPill } from './types';
import { validatePills } from './utils';

type Props = {
  mode: 'detail' | 'metric';
  onEditFinished: () => void;
  onEditConfirm: (data: {
    pillData: IPill[];
    datasetId: number;
    dimensions: any[];
    metrics: any[];
  }) => void;
};

function FilterConditions({ onEditFinished, onEditConfirm }: Props) {
  const { dimensions, metrics } = useDatasetInfo();
  const { datasetId, pillData } = useContextSelector(FilterInfosContext, context => context);

  const onConfirm = () => {
    // 校验数据
    if (!validatePills(pillData)) {
      return message.warning('请完善筛选条件');
    }
    onEditFinished();

    onEditConfirm({
      pillData,
      datasetId: datasetId!,
      dimensions,
      metrics,
    });
  };

  const onCancel = () => {
    onEditFinished();
  };

  return (
    <div className="filter-conditions-wrap">
      <PillList />
      <Actions onConfirm={onConfirm} onCancel={onCancel} />
    </div>
  );
}

export default memo(FilterConditions);

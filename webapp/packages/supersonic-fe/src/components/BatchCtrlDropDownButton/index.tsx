import { Space, Popconfirm, Dropdown, DatePicker, Popover, Button, Radio } from 'antd';
import { FC, useState, useRef } from 'react';
import {
  PlaySquareOutlined,
  StopOutlined,
  CloudDownloadOutlined,
  DeleteOutlined,
  ExportOutlined,
  RocketOutlined,
  FormOutlined,
} from '@ant-design/icons';

export type BatchCtrlDropDownButtonProps = {
  onMenuClick?: (key: string) => void;
  onDownloadDateRangeChange?: (dateRange: string[], pickerType: string) => void;
  onDeleteConfirm?: () => void;
  downloadLoading?: boolean;
  disabledList?: string[];
  hiddenList?: string[];
  extenderList?: string[];
};
const { RangePicker } = DatePicker;

const BatchCtrlDropDownButton: FC<BatchCtrlDropDownButtonProps> = ({
  onMenuClick,
  onDownloadDateRangeChange,
  onDeleteConfirm,
  downloadLoading,
  disabledList = [],
  hiddenList = [],
  extenderList = [],
}) => {
  const [popoverOpenState, setPopoverOpenState] = useState<boolean>(false);
  const [pickerType, setPickerType] = useState<string>('day');
  const dateRangeRef = useRef<any>([]);

  const extenderConfig: any = {
    exportTagButton: {
      key: 'exportTagButton',
      label: '导出为标签',
      hidden: !!!process.env.SHOW_TAG,
      icon: <ExportOutlined />,
      disabled: disabledList?.includes('exportTagButton'),
    },
    batchPublish: {
      key: 'batchPublish',
      label: '批量发布',
      icon: <RocketOutlined />,
      disabled: disabledList?.includes('batchPublish'),
    },
    batchUnPublish: {
      key: 'batchUnPublish',
      label: '批量下架',
      icon: <RocketOutlined />,
      disabled: disabledList?.includes('batchUnPublish'),
    },
    batchAddClass: {
      key: 'batchAddClass',
      label: '批量分类',
      icon: <FormOutlined />,
      disabled: disabledList?.includes('batchAddClass'),
    },
    batchSensitiveLevel: {
      key: 'batchSensitiveLevel',
      label: '批量修改敏感度',
      icon: <FormOutlined />,
      disabled: disabledList?.includes('batchSensitiveLevel'),
    },
    batchDimensionValueBlackList: {
      key: 'batchDimensionValueBlackList',
      label: '批量导入黑名单',
      icon: <FormOutlined />,
      disabled: disabledList?.includes('batchDimensionValueBlackList'),
    },
    batchDimensionValueWhiteList: {
      key: 'batchDimensionValueWhiteList',
      label: '批量导入白名单',
      icon: <FormOutlined />,
      disabled: disabledList?.includes('batchDimensionValueWhiteList'),
    },
    batchRemoveDimensionValueBlackList: {
      key: 'batchRemoveDimensionValueBlackList',
      label: '批量移除黑名单',
      icon: <DeleteOutlined />,
      disabled: disabledList?.includes('batchRemoveDimensionValueBlackList'),
    },
    batchRemoveDimensionValueWhiteList: {
      key: 'batchRemoveDimensionValueWhiteList',
      label: '批量移除白名单',
      icon: <DeleteOutlined />,
      disabled: disabledList?.includes('batchRemoveDimensionValueWhiteList'),
    },
  };

  const extenderButtonList: any[] = extenderList.reduce((list: any[], key) => {
    const target = extenderConfig[key];
    if (target) {
      list.push(target);
    }
    return list;
  }, []);

  const dropdownButtonItems: any[] = [
    {
      key: 'batchStart',
      label: '批量启用',
      icon: <PlaySquareOutlined />,
      disabled: disabledList?.includes('batchStart'),
    },
    {
      key: 'batchStop',
      label: '批量停用',
      icon: <StopOutlined />,
      disabled: disabledList?.includes('batchStop'),
    },
    {
      key: 'batchDownload',
      label: (
        <a
          onClick={() => {
            setPopoverOpenState(true);
          }}
        >
          批量下载
        </a>
      ),
      icon: <CloudDownloadOutlined />,
      disabled: disabledList?.includes('batchDownload'),
    },
    ...extenderButtonList,
    {
      key: 'batchDelete',
      label: (
        <Popconfirm
          title="确定批量删除吗？"
          onConfirm={() => {
            onDeleteConfirm?.();
          }}
        >
          <a>批量删除</a>
        </Popconfirm>
      ),
      icon: <DeleteOutlined />,
      disabled: disabledList?.includes('batchDelete'),
    },
  ]
    .filter((item) => {
      return !hiddenList.includes(item.key);
    })
    .filter((item) => !!!item.hidden);

  const popoverConfig = {
    title: '选择下载区间',
    content: (
      <Space direction="vertical">
        <Radio.Group
          size="small"
          value={pickerType}
          onChange={(e) => {
            setPickerType(e.target.value);
          }}
        >
          <Radio.Button value="day">按日</Radio.Button>
          <Radio.Button value="week">按周</Radio.Button>
          <Radio.Button value="month">按月</Radio.Button>
        </Radio.Group>
        <RangePicker
          style={{ paddingBottom: 5 }}
          onChange={(date) => {
            dateRangeRef.current = date;
            return;
          }}
          picker={pickerType as any}
          allowClear={true}
        />
        <div style={{ marginTop: 20 }}>
          <Space>
            <Button
              type="primary"
              loading={downloadLoading}
              onClick={() => {
                const [startMoment, endMoment] = dateRangeRef.current;
                let searchDateRange = [
                  startMoment?.format('YYYY-MM-DD'),
                  endMoment?.format('YYYY-MM-DD'),
                ];
                if (pickerType === 'week') {
                  searchDateRange = [
                    startMoment?.startOf('isoWeek').format('YYYY-MM-DD'),
                    endMoment?.startOf('isoWeek').format('YYYY-MM-DD'),
                  ];
                }
                if (pickerType === 'month') {
                  searchDateRange = [
                    startMoment?.startOf('month').format('YYYY-MM-DD'),
                    endMoment?.startOf('month').format('YYYY-MM-DD'),
                  ];
                }
                onDownloadDateRangeChange?.(searchDateRange, pickerType);
              }}
            >
              下载
            </Button>
          </Space>
        </div>
      </Space>
    ),
  };

  return (
    <Popover
      content={popoverConfig?.content}
      title={popoverConfig?.title}
      trigger="click"
      key="ctrlBtnList"
      open={popoverOpenState}
      placement="bottomLeft"
      onOpenChange={(open: boolean) => {
        setPopoverOpenState(open);
      }}
    >
      <Dropdown.Button
        menu={{
          items: dropdownButtonItems,
          onClick: ({ key }: { key: string }) => {
            onMenuClick?.(key);
          },
        }}
      >
        批量操作
      </Dropdown.Button>
    </Popover>
  );
};

export default BatchCtrlDropDownButton;

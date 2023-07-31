import React, { useEffect, useState } from 'react';
import { Button, Modal, message } from 'antd';
import { ISemantic } from '../data';
import CommonEditTable from './CommonEditTable';
import { createDimension, updateDimension } from '../service';
import { connect } from 'umi';
import type { StateType } from '../model';

export type CreateFormProps = {
  dimensionValueSettingList: ISemantic.IDimensionValueSettingItem[];
  onCancel: () => void;
  dimensionItem?: ISemantic.IDimensionItem;
  open: boolean;
  onSubmit: (values?: any) => void;
  domainManger: StateType;
};

type TableDataSource = { techName: string; bizName: string; alias: string };

const DimensionInfoModal: React.FC<CreateFormProps> = ({
  onCancel,
  open,
  dimensionItem,
  dimensionValueSettingList,
  domainManger,
  onSubmit,
}) => {
  const [tableDataSource, setTableDataSource] = useState<TableDataSource[]>([]);
  const { selectDomainId } = domainManger;
  const [dimValueMaps, setDimValueMaps] = useState<ISemantic.IDimensionValueSettingItem[]>([]);

  useEffect(() => {
    const dataSource = dimensionValueSettingList.map((item) => {
      const { alias } = item;
      return {
        ...item,
        alias: Array.isArray(alias) ? alias.join(',') : '',
      };
    });
    setTableDataSource(dataSource);
    setDimValueMaps(dimensionValueSettingList);
  }, [dimensionValueSettingList]);

  const handleSubmit = async () => {
    await saveDimension({ dimValueMaps });
    onSubmit?.(dimValueMaps);
  };

  const saveDimension = async (fieldsValue: any) => {
    if (!dimensionItem?.id) {
      return;
    }
    const queryParams = {
      domainId: selectDomainId,
      id: dimensionItem.id,
      ...fieldsValue,
    };
    const { code, msg } = await updateDimension(queryParams);
    if (code === 200) {
      return;
    }
    message.error(msg);
  };

  const renderFooter = () => {
    return (
      <>
        <Button onClick={onCancel}>取消</Button>
        <Button
          type="primary"
          onClick={() => {
            handleSubmit();
          }}
        >
          完成
        </Button>
      </>
    );
  };

  const columns = [
    {
      title: '技术名称',
      dataIndex: 'techName',
      width: 200,
      formItemProps: {
        fieldProps: {
          placeholder: '请填写技术名称',
        },
        rules: [
          {
            required: true,
            whitespace: true,
            message: '此项是必填项',
          },
        ],
      },
    },
    {
      title: '业务名称',
      dataIndex: 'bizName',
      width: 200,
      fieldProps: {
        placeholder: '请填写业务名称',
      },
      formItemProps: {
        rules: [
          {
            required: true,
            whitespace: true,
            message: '此项是必填项',
          },
        ],
      },
    },
    {
      title: '别名',
      dataIndex: 'alias',
      fieldProps: {
        placeholder: '多个别名用英文逗号隔开',
      },
    },
  ];

  return (
    <Modal
      width={1000}
      destroyOnClose
      title="维度值设置"
      style={{ top: 48 }}
      maskClosable={false}
      open={open}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <CommonEditTable
        tableDataSource={tableDataSource}
        columnList={columns}
        onDataSourceChange={(tableData) => {
          const dimValueMaps = tableData.map((item: TableDataSource) => {
            return {
              ...item,
              alias: item.alias ? `${item.alias}`.split(',') : [],
            };
          });

          setDimValueMaps(dimValueMaps);
        }}
      />
    </Modal>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(DimensionInfoModal);

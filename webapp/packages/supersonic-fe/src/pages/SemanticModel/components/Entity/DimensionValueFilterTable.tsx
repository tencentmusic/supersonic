import { useState, useEffect } from 'react';
import { Select, Popconfirm, Space, Button, Input } from 'antd';
import BatchCtrlDropDownButton from '@/components/BatchCtrlDropDownButton';
import TableHeaderFilter from '@/components/TableHeaderFilter';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import { ProTable } from '@ant-design/pro-components';
import { DimensionValueListType } from '../../enum';

import styles from '../style.less';
import { PlusOutlined } from '@ant-design/icons';

type Props = {
  listType: DimensionValueListType;
  dataSource: string[];
  onSubmit?: () => void;
  onCancel?: () => void;
  onMenuClick?: (key: string, selectedKes: React.Key[]) => void;
};

const DimensionValueFilterTable: React.FC<Props> = ({
  listType,
  dataSource,
  onCancel,
  onMenuClick,
}) => {
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [tableData, setTableData] = useState<any[]>([]);

  const [importValues, setImportValues] = useState<string[]>([]);

  const typeMap: any = {
    [DimensionValueListType.BLACK_LIST]: {
      label: '黑名单',
      tableSubTitle: '设置进入黑名单后，在搜索结果中维度值将会被过滤',
      dropDownExtenderList: ['batchRemoveDimensionValueBlackList'],
      addKey: 'batchDimensionValueBlackList',
      removeKey: 'batchRemoveDimensionValueBlackList',
    },
    [DimensionValueListType.WHITE_LIST]: {
      label: '白名单',
      tableSubTitle: '设置进入白名单后，将会在搜索结果展示出维度值',
      dropDownExtenderList: ['batchRemoveDimensionValueWhiteList'],
      addKey: 'batchDimensionValueWhiteList',
      removeKey: 'batchRemoveDimensionValueWhiteList',
    },
  };
  useEffect(() => {
    if (Array.isArray(dataSource)) {
      const data = dataSource.map((item) => {
        return {
          value: item,
        };
      });
      setTableData(data);
    } else {
      setTableData([]);
    }
  }, [dataSource]);

  const columns = [
    {
      title: '维度值',
      dataIndex: 'value',
      tooltip: '数据库中存储的维度值数据。 比如数据库中维度平台的维度值有kw、qy等',
      width: 200,
    },
    {
      title: '操作',
      dataIndex: 'x',
      valueType: 'option',
      width: 150,
      render: (_, record) => {
        return (
          <Space className={styles.ctrlBtnContainer}>
            <Popconfirm
              title="确认移除？"
              okText="是"
              cancelText="否"
              onConfirm={() => {
                onMenuClick?.(typeMap[listType].removeKey, [record.value]);
              }}
            >
              <Button type="link" key="metricDeleteBtn">
                移除
              </Button>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  return (
    <>
      <div>
        <div style={{ padding: '20px 0px', width: '100%' }}>
          <FormItemTitle
            title={`导入自定义维度值`}
            subTitle={`将尚不存在的维度值以自定义的方式导入${typeMap[listType].label}中`}
          />
          <Space.Compact style={{ marginTop: 10 }}>
            <Select
              style={{ width: '500px' }}
              mode="tags"
              placeholder={`输入维度值后回车确认，多维度值输入、复制粘贴支持英文逗号自动分隔`}
              tokenSeparators={[',']}
              maxTagCount={9}
              value={importValues}
              onChange={(values) => {
                setImportValues(values);
              }}
            />
            <Button
              key="importBtn"
              type="primary"
              onClick={() => {
                setImportValues([]);
                onMenuClick?.(typeMap[listType].addKey, importValues);
              }}
            >
              <Space>
                <PlusOutlined /> 导入
              </Space>
            </Button>
          </Space.Compact>
        </div>

        <FormItemTitle
          title={`${typeMap[listType].label}维度值列表`}
          subTitle={`${typeMap[listType].tableSubTitle}`}
        />
        <ProTable
          className={`${styles.dimensionValueFilterTable}`}
          rowKey="value"
          size="small"
          // loading={loading}
          pagination={{ defaultPageSize: 10, showSizeChanger: true }}
          search={false}
          columns={columns}
          dataSource={tableData}
          tableAlertRender={() => {
            return false;
          }}
          scroll={{ y: 500 }}
          toolBarRender={() => {
            return [
              <BatchCtrlDropDownButton
                key="ctrlBtnList"
                hiddenList={['batchStart', 'batchStop', 'batchDownload', 'batchDelete']}
                extenderList={typeMap[listType].dropDownExtenderList}
                onMenuClick={(key) => {
                  onMenuClick?.(key, selectedRowKeys);
                }}
              />,
            ];
          }}
          headerTitle={
            <TableHeaderFilter
              components={[
                {
                  component: (
                    <Input.Search
                      style={{ width: 280 }}
                      placeholder="请输入维度值名称"
                      allowClear
                      onSearch={(value) => {
                        const data = dataSource
                          .filter((item) => {
                            return item.includes(value);
                          })
                          .map((item) => {
                            return {
                              value: item,
                            };
                          });
                        setTableData(data);
                      }}
                    />
                  ),
                },
              ]}
            />
          }
          rowSelection={{
            type: 'checkbox',
            onChange: (selectedRowKeys: React.Key[]) => {
              setSelectedRowKeys(selectedRowKeys);
            },
          }}
          sticky={{ offsetHeader: 0 }}
          options={false}
        />
      </div>
    </>
  );
};

export default DimensionValueFilterTable;

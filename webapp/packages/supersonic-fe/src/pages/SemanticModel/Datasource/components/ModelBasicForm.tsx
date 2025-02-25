import React, { useState, useEffect } from 'react';
import { Form, Input, Spin, Select, message } from 'antd';
import type { FormInstance } from 'antd/lib/form';
import {getDbNames, getTables, getDimensionList, getCatalogs} from '../../service';
import { ISemantic } from '../../data';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';

const FormItem = Form.Item;
const { TextArea } = Input;

type Props = {
  isEdit?: boolean;
  databaseConfigList: ISemantic.IDatabaseItemList;
  modelItem: ISemantic.IModelItem;
  form: FormInstance<any>;
  mode?: 'normal' | 'fast';
};

const ModelBasicForm: React.FC<Props> = ({
  isEdit,
  modelItem,
  databaseConfigList,
  form,
  mode = 'normal',
}) => {
  const [currentDbLinkConfigId, setCurrentDbLinkConfigId] = useState<number>();
  const [currentCatalog, setCurrentCatalog] = useState<string>("");
  const [catalogList, setCatalogList] = useState<string[]>([]);
  const [dbNameList, setDbNameList] = useState<string[]>([]);
  const [tableNameList, setTableNameList] = useState<any[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [dimensionOptions, setDimensionOptions] = useState<{ label: string; value: number }[]>([]);
  const [catalogSelectOpen, setCatalogSelectOpen] = useState<boolean>(false);

  useEffect(() => {
    if (modelItem?.id) {
      queryDimensionList();
    }
  }, [modelItem]);

  const queryDimensionList = async () => {
    const { code, data, msg } = await getDimensionList({ modelId: modelItem?.id });
    if (code === 200 && Array.isArray(data?.list)) {
      setDimensionOptions(
        data.list.map((item: ISemantic.IDimensionItem) => {
          return {
            label: item.name,
            value: item.id,
          };
        }),
      );
    } else {
      message.error(msg);
    }
  };

  const onDatabaseSelect = (databaseId: number, type: string) => {
    setLoading(true);
    if (type === 'STARROCKS' || type === 'KYUUBI' || type === 'PRESTO' || type === 'TRINO') {
      queryCatalogList(databaseId);
      setCatalogSelectOpen(true);
      setDbNameList([]);
    } else {
      queryDbNameList(databaseId, "");
      setCatalogSelectOpen(false);
      setCatalogList([]);
    }
    form.setFieldsValue({
      catalog: undefined,
      dbName: undefined,
      tableName: undefined,
    })
  };

  const queryCatalogList = async (databaseId: number) => {
    setLoading(true);
    const { code, data, msg } = await getCatalogs(databaseId);
    setLoading(false)
    if (code === 200) {
      const list = data || [];
      setCatalogList(list);
    } else {
      message.error(msg);
    }
  }

  const onCatalogSelect = (catalog: string) => {
    if (currentDbLinkConfigId) {
      queryDbNameList(currentDbLinkConfigId, catalog);
    }
    form.setFieldsValue({
      catalog: catalog,
      dbName: undefined,
      tableName: undefined,
    })
    setCurrentCatalog(catalog);
  }

  const queryDbNameList = async (databaseId: number, catalog: string) => {
    setLoading(true);
    const { code, data, msg } = await getDbNames(databaseId, catalog);
    setLoading(false);
    if (code === 200) {
      const list = data || [];
      setDbNameList(list);
    } else {
      message.error(msg);
    }
  };

  const queryTableNameList = async (databaseName: string) => {
    if (!currentDbLinkConfigId) {
      return;
    }
    setLoading(true);
    const { code, data, msg } = await getTables(currentDbLinkConfigId, currentCatalog, databaseName);
    setLoading(false);
    if (code === 200) {
      const list = data || [];
      setTableNameList(list);
    } else {
      message.error(msg);
    }
  };

  return (
    <Spin spinning={loading}>
      {mode === 'fast' && (
        <>
          <FormItem
            name="databaseId"
            label="数据库连接"
            rules={[{ required: true, message: '请选择数据库连接' }]}
          >
            <Select
              showSearch
              placeholder="请选择数据库连接"
              disabled={isEdit}
              onSelect={(dbLinkConfigId: number, option) => {
                onDatabaseSelect(dbLinkConfigId, option.type);
                setCurrentDbLinkConfigId(dbLinkConfigId);
                setCurrentCatalog("");
              }}
            >
              {databaseConfigList.map((item) => (
                <Select.Option key={item.id} value={item.id} disabled={!item.hasUsePermission} type={item.type}>
                  {item.name}
                </Select.Option>
              ))}
            </Select>
          </FormItem>

           <FormItem
              name="catalog"
              label="Catalog"
              rules={[{ required: catalogSelectOpen, message: '请选择Catalog' }]}
              hidden={!catalogSelectOpen}
            >
              <Select
                showSearch
                placeholder="请选择Catalog"
                disabled={isEdit}
                onSelect={onCatalogSelect}
              >
                {catalogList.map((item) => (
                  <Select.Option key={item} value={item}>
                    {item}
                  </Select.Option>
                ))}
              </Select>
            </FormItem>

          <FormItem
            name="dbName"
            label="数据库名"
            rules={[{ required: true, message: '请选择数据库/表' }]}
          >
            <Select
              showSearch
              placeholder="请先选择一个数据库连接"
              disabled={isEdit}
              onSelect={(dbName: string) => {
                queryTableNameList(dbName);
                form.setFieldsValue({
                  tableName: undefined,
                })
              }}
            >
              {dbNameList.map((item) => (
                <Select.Option key={item} value={item}>
                  {item}
                </Select.Option>
              ))}
            </Select>
          </FormItem>
          <FormItem
            name="tableName"
            label="数据表名"
            rules={[{ required: true, message: '请选择数据库/表' }]}
          >
            <Select placeholder="请选择数据库/表" disabled={isEdit} showSearch>
              {tableNameList.map((item) => (
                <Select.Option key={item} value={item}>
                  {item}
                </Select.Option>
              ))}
            </Select>
          </FormItem>
        </>
      )}

      <FormItem
        name="name"
        label="模型中文名"
        rules={[{ required: true, message: '请输入模型中文名' }]}
      >
        <Input placeholder="名称不可重复" />
      </FormItem>
      <FormItem
        name="bizName"
        label="模型英文名"
        rules={[{ required: true, message: '请输入模型英文名' }]}
      >
        <Input placeholder="名称不可重复" disabled={isEdit} />
      </FormItem>
      {/* <FormItem
        name="alias"
        label="别名"
        getValueFromEvent={(value) => {
          return value.join(',');
        }}
        getValueProps={(value) => {
          return {
            value: value && isString(value) ? value.split(',') : [],
          };
        }}
      >
        <Select
          mode="tags"
          placeholder="输入别名后回车确认，多别名输入、复制粘贴支持英文逗号自动分隔"
          tokenSeparators={[',']}
          maxTagCount={9}
        />
      </FormItem> */}
      <FormItem
        name="drillDownDimensions"
        label={
          <FormItemTitle
            title={'默认下钻维度'}
            subTitle={'配置之后,可在指标主页和问答指标卡处选择用来对指标进行下钻和过滤'}
          />
        }
        hidden={!modelItem?.id}
      >
        <Select
          mode="multiple"
          options={dimensionOptions}
          placeholder="请选择默认下钻维度"
          maxTagCount={9}
        />
      </FormItem>
      <FormItem name="description" label="模型描述">
        <TextArea placeholder="请输入模型描述" />
      </FormItem>
      {/* <FormItem name={['ext', 'usId']} label="调度任务ID">
        <Select
          mode="tags"
          placeholder="输入ID后回车确认，多ID输入、复制粘贴支持英文逗号自动分隔"
          tokenSeparators={[',']}
          maxTagCount={9}
        />
      </FormItem> */}
    </Spin>
  );
};

export default ModelBasicForm;

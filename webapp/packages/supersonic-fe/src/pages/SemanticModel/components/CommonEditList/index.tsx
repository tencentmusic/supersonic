import React, { useState, useEffect, ReactNode } from 'react';
import { List, Collapse, Button } from 'antd';
import { uuid } from '@/utils/utils';
import SqlEditor from '@/components/SqlEditor';
import styles from './style.less';

const { Panel } = Collapse;

type Props = {
  title?: string | ReactNode;
  defaultCollapse?: boolean;
  value?: string[];
  createBtnType?: 'link' | 'primary';
  onChange?: (list: string[]) => void;
};

type ListItem = {
  id: string;
  sql: string;
};

type List = ListItem[];

const CommonEditList: React.FC<Props> = ({
  title,
  defaultCollapse = false,
  value,
  createBtnType = 'link',
  onChange,
}) => {
  const [listDataSource, setListDataSource] = useState<List>([]);
  const [currentSql, setCurrentSql] = useState<string>('');
  const [activeKey, setActiveKey] = useState<string>();
  const [currentRecord, setCurrentRecord] = useState<ListItem>();

  useEffect(() => {
    if (Array.isArray(value)) {
      const list = value.map((sql: string) => {
        return {
          id: uuid(),
          sql,
        };
      });
      setListDataSource(list);
    }
  }, [value]);

  const handleListChange = (listDataSource: List) => {
    const sqlList = listDataSource.map((item) => {
      return item.sql;
    });
    onChange?.(sqlList);
  };

  return (
    <div className={styles.commonEditList}>
      <Collapse
        activeKey={activeKey}
        defaultActiveKey={defaultCollapse ? ['editor'] : undefined}
        onChange={() => {}}
        ghost
      >
        <Panel
          header={title}
          key="editor"
          extra={
            activeKey ? (
              <Button
                key="saveBtn"
                type={createBtnType}
                onClick={() => {
                  if (!currentRecord && !currentSql) {
                    setActiveKey(undefined);
                    return;
                  }
                  if (currentRecord) {
                    const list = [...listDataSource].map((item) => {
                      if (item.id === currentRecord.id) {
                        return {
                          ...item,
                          sql: currentSql,
                        };
                      }
                      return item;
                    });
                    setListDataSource(list);
                    handleListChange(list);
                  } else {
                    const list = [
                      ...listDataSource,
                      {
                        id: uuid(),
                        sql: currentSql,
                      },
                    ];
                    setListDataSource(list);
                    handleListChange(list);
                  }

                  setActiveKey(undefined);
                }}
              >
                确认
              </Button>
            ) : (
              <Button
                type={createBtnType}
                key="createBtn"
                onClick={() => {
                  setCurrentRecord(undefined);
                  setCurrentSql('');
                  setActiveKey('editor');
                }}
              >
                新增
              </Button>
            )
          }
          showArrow={false}
        >
          <div>
            <SqlEditor
              value={currentSql}
              height={'150px'}
              onChange={(sql) => {
                setCurrentSql(sql);
              }}
            />
          </div>
        </Panel>
      </Collapse>
      <List
        itemLayout="horizontal"
        dataSource={listDataSource || []}
        renderItem={(item) => (
          <List.Item
            actions={[
              <a
                key="list-loadmore-edit"
                onClick={() => {
                  setCurrentSql(item.sql);
                  setCurrentRecord(item);
                  setActiveKey('editor');
                }}
              >
                编辑
              </a>,
              <a
                key="list-loadmore-more"
                onClick={() => {
                  const list = [...listDataSource].filter(({ id }) => {
                    return item.id !== id;
                  });
                  handleListChange(list);
                  setListDataSource(list);
                }}
              >
                删除
              </a>,
            ]}
          >
            <List.Item.Meta title={item.sql} />
          </List.Item>
        )}
      />
    </div>
  );
};

export default CommonEditList;

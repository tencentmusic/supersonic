import React, { useState, useRef } from 'react';
import type { FC } from 'react';
import { Input, Tag, Tooltip } from 'antd';
import { PlusOutlined } from '@ant-design/icons';

import styles from '../style.less';

type ParamsItemProps = {
  value?: string[];
  onChange?: (e: string[]) => void;
};
const ParamsSqlEditor: FC<ParamsItemProps> = ({ value, onChange }) => {
  const [editInputValue, setEditInputValue] = useState<string>();
  const [inputValue, setInputValue] = useState<string>();
  const [editInputIndex, setEditInputIndex] = useState<number>(-1);
  const [inputVisible, setInputVisible] = useState<boolean>(false);
  const editInput = useRef<typeof Input>(null);
  const inputRef = useRef<typeof Input>(null);

  const handleEditInputChange: React.ChangeEventHandler<HTMLInputElement> = (e) => {
    setEditInputValue(e.target.value);
  };
  const handleEditInputConfirm = () => {
    const newValues = value ? [...value] : [];
    newValues[editInputIndex] = editInputValue || '';
    if (onChange) {
      onChange(newValues);
    }
    setEditInputIndex(-1);
    setEditInputValue('');
  };
  const handleClose = (removedTag: string) => {
    const newValues = value ? value.filter((tag) => tag !== removedTag) : [];
    if (onChange) {
      onChange(newValues);
    }
  };
  const handleInputChange: React.ChangeEventHandler<HTMLInputElement> = (e) => {
    setInputValue(e.target.value);
  };

  const handleInputConfirm = () => {
    const newValues = value ? [...value] : [];
    if (inputValue && !newValues.includes(inputValue)) {
      newValues.push(inputValue);
    }
    if (onChange) {
      onChange(newValues);
    }
    setInputVisible(false);
    setInputValue('');
  };

  const showInput = () => {
    setInputVisible(true);
    setTimeout(() => {
      inputRef.current?.focus();
    }, 0);
  };

  return (
    <>
      {value &&
        value.map((tag: any, index: number) => {
          if (editInputIndex === index) {
            return (
              <Input
                ref={editInput}
                key={tag}
                size="small"
                className={styles.tagInput}
                value={editInputValue}
                onChange={handleEditInputChange}
                onBlur={handleEditInputConfirm}
                onPressEnter={handleEditInputConfirm}
              />
            );
          }

          const isLongTag = tag.length > 20;

          const tagElem = (
            <Tag
              className={styles.editTag}
              key={tag}
              closable={true}
              onClose={() => handleClose(tag)}
            >
              <span
                onDoubleClick={(e) => {
                  setEditInputIndex(index);
                  setEditInputValue(tag);
                  e.preventDefault();
                  setTimeout(() => {
                    editInput.current?.focus();
                  }, 0);
                }}
              >
                {isLongTag ? `${tag.slice(0, 20)}...` : tag}
              </span>
            </Tag>
          );
          return isLongTag ? (
            <Tooltip title={tag} key={tag}>
              {tagElem}
            </Tooltip>
          ) : (
            tagElem
          );
        })}
      {inputVisible && (
        <Input
          ref={inputRef}
          type="text"
          size="small"
          className={styles.tagInput}
          value={inputValue}
          onChange={handleInputChange}
          onBlur={handleInputConfirm}
          onPressEnter={handleInputConfirm}
        />
      )}
      {!inputVisible && (
        <Tag className={styles.siteTagPlus} onClick={showInput}>
          <PlusOutlined /> 增加默认值
        </Tag>
      )}
    </>
  );
};

export default ParamsSqlEditor;

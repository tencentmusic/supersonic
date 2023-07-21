import { PlusOutlined } from '@ant-design/icons';
import type { InputRef } from 'antd';
import { Input as AntdInput, Tag, Tooltip } from 'antd';
import React, { useEffect, useRef, useState } from 'react';
import styles from './style.less';

type Props = {
  createBtnString?: string;
  value?: string[];
  onChange?: (valueList: string[]) => void;
};

const InfoTagList: React.FC<Props> = ({ value, createBtnString = '新增', onChange }) => {
  const [tags, setTags] = useState<string[]>([]);
  const [inputVisible, setInputVisible] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [editInputIndex, setEditInputIndex] = useState(-1);
  const [editInputValue, setEditInputValue] = useState('');
  const inputRef = useRef<InputRef>(null);
  const editInputRef = useRef<InputRef>(null);

  useEffect(() => {
    if (Array.isArray(value)) {
      setTags([...value]);
    }
  }, [value]);

  const handleTagChange = (tagList: string[]) => {
    onChange?.(tagList);
  };

  useEffect(() => {
    if (inputVisible) {
      inputRef.current?.focus();
    }
  }, [inputVisible]);

  useEffect(() => {
    editInputRef.current?.focus();
  }, [inputValue]);

  const handleClose = (removedTag: string) => {
    const newTags = tags.filter((tag) => tag !== removedTag);
    handleTagChange?.(newTags);
    setTags(newTags);
  };

  const showInput = () => {
    setInputVisible(true);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
  };

  const handleInputConfirm = () => {
    if (inputValue && tags.indexOf(inputValue) === -1) {
      const newTags = [...tags, inputValue];
      handleTagChange?.(newTags);
      setTags(newTags);
    }
    setInputVisible(false);
    setInputValue('');
  };

  const handleEditInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setEditInputValue(e.target.value);
  };

  const handleEditInputConfirm = () => {
    const newTags = [...tags];
    newTags[editInputIndex] = editInputValue;
    setTags(newTags);
    handleTagChange?.(newTags);
    setEditInputIndex(-1);
    setInputValue('');
  };

  return (
    <div className={styles.infoTagList}>
      {tags.map((tag, index) => {
        if (editInputIndex === index) {
          return (
            <AntdInput
              ref={editInputRef}
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
        <AntdInput
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
          <PlusOutlined /> {createBtnString}
        </Tag>
      )}
    </div>
  );
};

export default InfoTagList;

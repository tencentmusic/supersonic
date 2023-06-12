import IconFont from '@/components/IconFont';
import { getTextWidth, groupByColumn, isMobile } from '@/utils/utils';
import { AutoComplete, Select, Tag } from 'antd';
import classNames from 'classnames';
import { debounce } from 'lodash';
import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { searchRecommend } from 'supersonic-chat-sdk';
import { SemanticTypeEnum, SEMANTIC_TYPE_MAP } from '../constants';
import styles from './style.less';
import { PLACE_HOLDER } from '@/common/constants';

type Props = {
  inputMsg: string;
  chatId?: number;
  onInputMsgChange: (value: string) => void;
  onSendMsg: (msg: string, domainId?: number) => void;
};

const { OptGroup, Option } = Select;
let isPinyin = false;
let isSelect = false;

const compositionStartEvent = () => {
  isPinyin = true;
};

const compositionEndEvent = () => {
  isPinyin = false;
};

const ChatFooter: ForwardRefRenderFunction<any, Props> = (
  { inputMsg, chatId, onInputMsgChange, onSendMsg },
  ref,
) => {
  const [stepOptions, setStepOptions] = useState<Record<string, any[]>>({});
  const [open, setOpen] = useState(false);
  const [focused, setFocused] = useState(false);
  const inputRef = useRef<any>();
  const fetchRef = useRef(0);

  const inputFocus = () => {
    inputRef.current?.focus();
  };

  const inputBlur = () => {
    inputRef.current?.blur();
  };

  useImperativeHandle(ref, () => ({
    inputFocus,
    inputBlur,
  }));

  const initEvents = () => {
    const autoCompleteEl = document.getElementById('chatInput');
    autoCompleteEl!.addEventListener('compositionstart', compositionStartEvent);
    autoCompleteEl!.addEventListener('compositionend', compositionEndEvent);
  };

  const removeEvents = () => {
    const autoCompleteEl = document.getElementById('chatInput');
    if (autoCompleteEl) {
      autoCompleteEl.removeEventListener('compositionstart', compositionStartEvent);
      autoCompleteEl.removeEventListener('compositionend', compositionEndEvent);
    }
  };

  useEffect(() => {
    initEvents();
    return () => {
      removeEvents();
    };
  }, []);

  const debounceGetWordsFunc = useCallback(() => {
    const getAssociateWords = async (msg: string, chatId?: number) => {
      if (isPinyin) {
        return;
      }
      fetchRef.current += 1;
      const fetchId = fetchRef.current;
      const res = await searchRecommend(msg, chatId);
      if (fetchId !== fetchRef.current) {
        return;
      }
      const recommends = msg ? res.data.data || [] : [];
      const stepOptionList = recommends.map((item: any) => item.subRecommend);

      if (stepOptionList.length > 0 && stepOptionList.every((item: any) => item !== null)) {
        const data = groupByColumn(recommends, 'domainName');
        const optionsData =
          isMobile && recommends.length > 6
            ? Object.keys(data)
              .slice(0, 4)
              .reduce((result, key) => {
                result[key] = data[key].slice(
                  0,
                  Object.keys(data).length > 2 ? 2 : Object.keys(data).length > 1 ? 3 : 6,
                );
                return result;
              }, {})
            : data;
        setStepOptions(optionsData);
      } else {
        setStepOptions({});
      }

      setOpen(recommends.length > 0);
    };
    return debounce(getAssociateWords, 20);
  }, []);

  const [debounceGetWords] = useState<any>(debounceGetWordsFunc);

  useEffect(() => {
    if (!isSelect) {
      debounceGetWords(inputMsg, chatId);
    } else {
      isSelect = false;
    }
    if (!inputMsg) {
      setStepOptions({});
    }
  }, [inputMsg]);

  useEffect(() => {
    if (!focused) {
      setOpen(false);
    }
  }, [focused]);

  useEffect(() => {
    const autoCompleteDropdown = document.querySelector(
      `.${styles.autoCompleteDropdown}`,
    ) as HTMLElement;
    if (!autoCompleteDropdown) {
      return;
    }
    const textWidth = getTextWidth(inputMsg);
    if (Object.keys(stepOptions).length > 0) {
      autoCompleteDropdown.style.marginLeft = `${textWidth}px`;
    }
  }, [stepOptions]);

  const sendMsg = (value: string) => {
    const option = Object.keys(stepOptions)
      .reduce((result: any[], item) => {
        result = result.concat(stepOptions[item]);
        return result;
      }, [])
      .find((item) =>
        Object.keys(stepOptions).length === 1
          ? item.recommend === value
          : `${item.domainName || ''}${item.recommend}` === value,
      );
    if (option && isSelect) {
      onSendMsg(option.recommend, option.domainId);
    } else {
      onSendMsg(value);
    }
  };

  const autoCompleteDropdownClass = classNames(styles.autoCompleteDropdown, {
    [styles.external]: true,
    [styles.mobile]: isMobile,
  });

  const onSelect = (value: string) => {
    isSelect = true;
    sendMsg(value);
    setOpen(false);
    setTimeout(() => {
      isSelect = false;
    }, 200);
  };

  const chatFooterClass = classNames(styles.chatFooter, {
    [styles.mobile]: isMobile,
  });

  return (
    <div className={chatFooterClass}>
      <div className={styles.composer}>
        <div className={styles.composerInputWrapper}>
          <AutoComplete
            className={styles.composerInput}
            placeholder={PLACE_HOLDER}
            value={inputMsg}
            onChange={onInputMsgChange}
            onSelect={onSelect}
            autoFocus={!isMobile}
            backfill
            ref={inputRef}
            id="chatInput"
            onKeyDown={(e) => {
              if ((e.code === 'Enter' || e.code === 'NumpadEnter') && !isSelect) {
                const chatInputEl: any = document.getElementById('chatInput');
                sendMsg(chatInputEl.value);
                setOpen(false);
              }
            }}
            onFocus={() => {
              setFocused(true);
            }}
            onBlur={() => {
              setFocused(false);
            }}
            dropdownClassName={autoCompleteDropdownClass}
            listHeight={500}
            allowClear
            open={open}
            getPopupContainer={isMobile ? (triggerNode) => triggerNode.parentNode : undefined}
          >
            {Object.keys(stepOptions).map((key) => {
              return (
                <OptGroup key={key} label={key}>
                  {stepOptions[key].map((option) => (
                    <Option
                      key={`${option.recommend}${option.domainName ? `_${option.domainName}` : ''}`}
                      value={
                        Object.keys(stepOptions).length === 1
                          ? option.recommend
                          : `${option.domainName || ''}${option.recommend}`
                      }
                      className={styles.searchOption}
                    >
                      <div className={styles.optionContent}>
                        {option.schemaElementType && (
                          <Tag
                            className={styles.semanticType}
                            color={
                              option.schemaElementType === SemanticTypeEnum.DIMENSION ||
                              option.schemaElementType === SemanticTypeEnum.DOMAIN
                                ? 'blue'
                                : option.schemaElementType === SemanticTypeEnum.VALUE
                                  ? 'geekblue'
                                  : 'orange'
                            }
                          >
                            {SEMANTIC_TYPE_MAP[option.schemaElementType] ||
                              option.schemaElementType ||
                              '维度'}
                          </Tag>
                        )}
                        {option.subRecommend}
                      </div>
                    </Option>
                  ))}
                </OptGroup>
              );
            })}
          </AutoComplete>
          <div
            className={classNames(styles.sendBtn, {
              [styles.sendBtnActive]: inputMsg?.length > 0,
            })}
            onClick={() => {
              sendMsg(inputMsg);
            }}
          >
            <IconFont type="icon-ios-send" />
          </div>
        </div>
      </div>
    </div>
  );
};

export default forwardRef(ChatFooter);

import IconFont from '@/components/IconFont';
import { getTextWidth, groupByColumn, isMobile } from '@/utils/utils';
import { AutoComplete, Select, Tag, Tooltip } from 'antd';
import classNames from 'classnames';
import { debounce } from 'lodash';
import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { searchRecommend } from 'supersonic-chat-sdk';
import { SemanticTypeEnum, SEMANTIC_TYPE_MAP } from '../constants';
import styles from './style.less';
import { PLACE_HOLDER } from '../constants';
import { DefaultEntityType, ModelType } from '../type';
import { MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons';

type Props = {
  inputMsg: string;
  chatId?: number;
  currentModel?: ModelType;
  defaultEntity?: DefaultEntityType;
  isCopilotMode?: boolean;
  copilotFullscreen?: boolean;
  models: ModelType[];
  collapsed: boolean;
  onToggleCollapseBtn: () => void;
  onInputMsgChange: (value: string) => void;
  onSendMsg: (msg: string, modelId?: number) => void;
  onAddConversation: () => void;
  onCancelDefaultFilter: () => void;
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
  {
    inputMsg,
    chatId,
    currentModel,
    defaultEntity,
    models,
    collapsed,
    isCopilotMode,
    copilotFullscreen,
    onToggleCollapseBtn,
    onInputMsgChange,
    onSendMsg,
    onAddConversation,
    onCancelDefaultFilter,
  },
  ref,
) => {
  const [modelOptions, setModelOptions] = useState<ModelType[]>([]);
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

  const getStepOptions = (recommends: any[]) => {
    const data = groupByColumn(recommends, 'modelName');
    return isMobile && recommends.length > 6
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
  };

  const processMsg = (msg: string, models: ModelType[]) => {
    let msgValue = msg;
    let modelId: number | undefined;
    if (msg?.[0] === '@') {
      const model = models.find((item) => msg.includes(`@${item.name}`));
      msgValue = model ? msg.replace(`@${model.name}`, '') : msg;
      modelId = model?.id;
    }
    return { msgValue, modelId };
  };

  const debounceGetWordsFunc = useCallback(() => {
    const getAssociateWords = async (
      msg: string,
      models: ModelType[],
      chatId?: number,
      model?: ModelType,
    ) => {
      if (isPinyin) {
        return;
      }
      if (msg === '' || (msg.length === 1 && msg[0] === '@')) {
        return;
      }
      fetchRef.current += 1;
      const fetchId = fetchRef.current;
      const { msgValue, modelId } = processMsg(msg, models);
      const modelIdValue = modelId || model?.id;
      const res = await searchRecommend(msgValue.trim(), chatId, modelIdValue);
      if (fetchId !== fetchRef.current) {
        return;
      }

      const recommends = msgValue ? res.data.data || [] : [];
      const stepOptionList = recommends.map((item: any) => item.subRecommend);

      if (stepOptionList.length > 0 && stepOptionList.every((item: any) => item !== null)) {
        setStepOptions(getStepOptions(recommends));
      } else {
        setStepOptions({});
      }
      setOpen(recommends.length > 0);
    };
    return debounce(getAssociateWords, 200);
  }, []);

  const [debounceGetWords] = useState<any>(debounceGetWordsFunc);

  useEffect(() => {
    if (inputMsg.length === 1 && inputMsg[0] === '@') {
      setOpen(true);
      setModelOptions(models);
      setStepOptions({});
      return;
    } else {
      setOpen(false);
      if (modelOptions.length > 0) {
        setTimeout(() => {
          setModelOptions([]);
        }, 500);
      }
    }
    if (!isSelect) {
      debounceGetWords(inputMsg, models, chatId, currentModel);
    } else {
      isSelect = false;
    }
    if (!inputMsg) {
      setStepOptions({});
      fetchRef.current = 0;
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
    } else {
      setTimeout(() => {
        autoCompleteDropdown.style.marginLeft = `0px`;
      }, 200);
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
          : `${item.modelName || ''}${item.recommend}` === value,
      );
    if (option && isSelect) {
      onSendMsg(option.recommend, option.modelId);
    } else {
      onSendMsg(value.trim());
    }
  };

  const autoCompleteDropdownClass = classNames(styles.autoCompleteDropdown, {
    [styles.mobile]: isMobile,
    [styles.modelOptions]: modelOptions.length > 0,
  });

  const onSelect = (value: string) => {
    isSelect = true;
    if (modelOptions.length === 0) {
      sendMsg(value);
    }
    setOpen(false);
    setTimeout(() => {
      isSelect = false;
    }, 200);
  };

  const chatFooterClass = classNames(styles.chatFooter, {
    [styles.mobile]: isMobile,
    [styles.defaultCopilotMode]: isCopilotMode && !copilotFullscreen,
  });

  return (
    <div className={chatFooterClass}>
      <div className={styles.composer}>
        <div className={styles.collapseBtn} onClick={onToggleCollapseBtn}>
          {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        </div>
        <Tooltip title="新建对话">
          <IconFont
            type="icon-icon-add-conversation-line"
            className={styles.addConversation}
            onClick={onAddConversation}
          />
        </Tooltip>
        <div className={styles.composerInputWrapper}>
          {currentModel && (
            <div className={styles.currentModel}>
              <div className={styles.currentModelName}>
                输入联想与问题回复将限定于：“
                <span className={styles.quoteText}>
                  主题域【{currentModel.name}】
                  {defaultEntity && (
                    <>
                      <span>，</span>
                      <span>{`${currentModel.name.slice(0, currentModel.name.length - 1)}【`}</span>
                      <span className={styles.entityName} title={defaultEntity.entityName}>
                        {defaultEntity.entityName}
                      </span>
                      <span>】</span>
                    </>
                  )}
                </span>
                ”
              </div>
              <div className={styles.cancelModel} onClick={onCancelDefaultFilter}>
                取消限定
              </div>
            </div>
          )}
          <AutoComplete
            className={styles.composerInput}
            placeholder={
              currentModel
                ? `请输入【${currentModel.name}】主题的问题，可使用@切换到其他主题`
                : PLACE_HOLDER
            }
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
            getPopupContainer={(triggerNode) => triggerNode.parentNode}
          >
            {modelOptions.length > 0
              ? modelOptions.map((model) => {
                  return (
                    <Option
                      key={model.id}
                      value={`@${model.name} `}
                      className={styles.searchOption}
                    >
                      {model.name}
                    </Option>
                  );
                })
              : Object.keys(stepOptions).map((key) => {
                  return (
                    <OptGroup key={key} label={key}>
                      {stepOptions[key].map((option) => {
                        let optionValue =
                          Object.keys(stepOptions).length === 1
                            ? option.recommend
                            : `${option.modelName || ''}${option.recommend}`;
                        if (inputMsg[0] === '@') {
                          const model = models.find((item) => inputMsg.includes(item.name));
                          optionValue = model ? `@${model.name} ${option.recommend}` : optionValue;
                        }
                        return (
                          <Option
                            key={`${option.recommend}${
                              option.modelName ? `_${option.modelName}` : ''
                            }`}
                            value={optionValue}
                            className={styles.searchOption}
                          >
                            <div className={styles.optionContent}>
                              {option.schemaElementType && (
                                <Tag
                                  className={styles.semanticType}
                                  color={
                                    option.schemaElementType === SemanticTypeEnum.DIMENSION ||
                                    option.schemaElementType === SemanticTypeEnum.MODEL
                                      ? 'blue'
                                      : option.schemaElementType === SemanticTypeEnum.VALUE
                                      ? 'geekblue'
                                      : 'cyan'
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
                        );
                      })}
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

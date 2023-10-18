import IconFont from '../../components/IconFont';
import { getTextWidth, groupByColumn, isMobile } from '../../utils/utils';
import { AutoComplete, Select, Tag } from 'antd';
import classNames from 'classnames';
import { debounce } from 'lodash';
import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { SemanticTypeEnum, SEMANTIC_TYPE_MAP, HOLDER_TAG } from '../constants';
import { AgentType, ModelType } from '../type';
import { searchRecommend } from '../../service';
import styles from './style.module.less';

type Props = {
  inputMsg: string;
  chatId?: number;
  currentAgent?: AgentType;
  agentList: AgentType[];
  onToggleHistoryVisible: () => void;
  onOpenAgents: () => void;
  onInputMsgChange: (value: string) => void;
  onSendMsg: (msg: string, modelId?: number) => void;
  onAddConversation: (agent?: AgentType) => void;
  onSelectAgent: (agent: AgentType) => void;
  onOpenShowcase: () => void;
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
    currentAgent,
    agentList,
    onToggleHistoryVisible,
    onOpenAgents,
    onInputMsgChange,
    onSendMsg,
    onAddConversation,
    onSelectAgent,
    onOpenShowcase,
  },
  ref
) => {
  const [modelOptions, setModelOptions] = useState<(ModelType | AgentType)[]>([]);
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
              Object.keys(data).length > 2 ? 2 : Object.keys(data).length > 1 ? 3 : 6
            );
            return result;
          }, {})
      : data;
  };

  const processMsg = (msg: string) => {
    let msgValue = msg;
    let modelId: number | undefined;
    if (msg?.[0] === '/') {
      const agent = agentList.find(item => msg.includes(`/${item.name}`));
      msgValue = agent ? msg.replace(`/${agent.name}`, '') : msg;
    }
    return { msgValue, modelId };
  };

  const debounceGetWordsFunc = useCallback(() => {
    const getAssociateWords = async (msg: string, chatId?: number, currentAgent?: AgentType) => {
      if (isPinyin) {
        return;
      }
      if (msg === '' || (msg.length === 1 && msg[0] === '@')) {
        return;
      }
      fetchRef.current += 1;
      const fetchId = fetchRef.current;
      const { msgValue, modelId } = processMsg(msg);
      const res = await searchRecommend(msgValue.trim(), chatId, modelId, currentAgent?.id);
      if (fetchId !== fetchRef.current) {
        return;
      }
      const recommends = msgValue ? res.data || [] : [];
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
    if (inputMsg.length === 1 && inputMsg[0] === '/') {
      setOpen(true);
      setModelOptions(agentList);
      setStepOptions({});
      return;
    }
    if (modelOptions.length > 0) {
      setTimeout(() => {
        setModelOptions([]);
      }, 50);
    }
    if (!isSelect) {
      debounceGetWords(inputMsg, chatId, currentAgent);
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
      `.${styles.autoCompleteDropdown}`
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
      .find(item =>
        Object.keys(stepOptions).length === 1
          ? item.recommend === value
          : `${item.modelName || ''}${item.recommend}` === value
      );
    if (option && isSelect) {
      onSendMsg(option.recommend, Object.keys(stepOptions).length > 1 ? option.modelId : undefined);
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
    if (modelOptions.length > 0) {
      const agent = agentList.find(item => value.includes(item.name));
      if (agent) {
        if (agent.id !== currentAgent?.id) {
          onSelectAgent(agent);
        }
        onInputMsgChange('');
      }
    } else {
      onInputMsgChange(value.replace(HOLDER_TAG, ''));
    }
    setOpen(false);
    setTimeout(() => {
      isSelect = false;
    }, 200);
  };

  const chatFooterClass = classNames(styles.chatFooter, {
    [styles.mobile]: isMobile,
  });

  const modelOptionNodes = modelOptions.map(model => {
    return (
      <Option key={model.id} value={`/${model.name} `} className={styles.searchOption}>
        {model.name}
      </Option>
    );
  });

  const associateOptionNodes = Object.keys(stepOptions).map(key => {
    return (
      <OptGroup key={key} label={key}>
        {stepOptions[key].map(option => {
          let optionValue =
            Object.keys(stepOptions).length === 1
              ? option.recommend
              : `${option.modelName || ''}${option.recommend}`;
          if (inputMsg[0] === '/') {
            const agent = agentList.find(item => inputMsg.includes(item.name));
            optionValue = agent ? `/${agent.name} ${option.recommend}` : optionValue;
          }
          return (
            <Option
              key={`${option.recommend}${option.modelName ? `_${option.modelName}` : ''}`}
              value={`${optionValue}${HOLDER_TAG}`}
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
  });

  return (
    <div className={chatFooterClass}>
      <div className={styles.tools}>
        <div
          className={styles.toolItem}
          onClick={() => {
            onAddConversation();
          }}
        >
          <IconFont type="icon-c003xiaoxiduihua" className={styles.toolIcon} />
          <div>新对话</div>
        </div>
        {!isMobile && (
          <div className={styles.toolItem} onClick={onToggleHistoryVisible}>
            <IconFont type="icon-lishi" className={styles.toolIcon} />
            <div>历史对话</div>
          </div>
        )}
        <div className={styles.toolItem} onClick={onOpenAgents}>
          <IconFont type="icon-zhinengzhuli" className={styles.toolIcon} />
          <div>智能助理</div>
        </div>
        {!isMobile && (
          <div className={styles.toolItem} onClick={onOpenShowcase}>
            <IconFont type="icon-showcase" className={styles.toolIcon} />
            <div>showcase</div>
          </div>
        )}
      </div>
      <div className={styles.composer}>
        <div className={styles.composerInputWrapper}>
          <AutoComplete
            className={styles.composerInput}
            placeholder={
              currentAgent
                ? `智能助理${
                    isMobile ? `[${currentAgent?.name}]` : `【${currentAgent?.name}】`
                  }将与您对话，输入“/”可切换助理`
                : '请输入您的问题'
            }
            value={inputMsg}
            onChange={(value: string) => {
              onInputMsgChange(value);
            }}
            onSelect={onSelect}
            autoFocus={!isMobile}
            ref={inputRef}
            id="chatInput"
            onKeyDown={e => {
              if (e.code === 'Enter' || e.code === 'NumpadEnter') {
                const chatInputEl: any = document.getElementById('chatInput');
                const agent = agentList.find(
                  item => chatInputEl.value[0] === '/' && chatInputEl.value.includes(item.name)
                );
                if (agent) {
                  if (agent.id !== currentAgent?.id) {
                    onSelectAgent(agent);
                  }
                  onInputMsgChange('');
                  return;
                }
                if (!isSelect) {
                  sendMsg(chatInputEl.value);
                  setOpen(false);
                }
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
            defaultActiveFirstOption={false}
            getPopupContainer={triggerNode => triggerNode.parentNode}
          >
            {modelOptions.length > 0 ? modelOptionNodes : associateOptionNodes}
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

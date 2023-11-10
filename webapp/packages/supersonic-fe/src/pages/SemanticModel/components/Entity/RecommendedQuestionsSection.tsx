import { message } from 'antd';
import React, { useState, useEffect } from 'react';
import { connect } from 'umi';
import type { StateType } from '../../model';
import { getDomainExtendConfig, addDomainExtend, editDomainExtend } from '../../service';
import ProCard from '@ant-design/pro-card';

import TextAreaCommonEditList from '../../components/CommonEditList/TextArea';

type Props = {
  domainManger: StateType;
};

const RecommendedQuestionsSection: React.FC<Props> = ({ domainManger }) => {
  const { selectModelId: modelId } = domainManger;

  const [questionData, setQuestionData] = useState<string[]>([]);
  const [currentRecordId, setCurrentRecordId] = useState<number>(0);

  const queryThemeListData: any = async () => {
    const { code, data } = await getDomainExtendConfig({
      modelId,
    });

    if (code === 200) {
      const target = data?.[0] || {};
      if (Array.isArray(target.recommendedQuestions)) {
        setQuestionData(
          target.recommendedQuestions.map((item: { question: string }) => {
            return item.question;
          }),
        );
        setCurrentRecordId(target.id || 0);
      } else {
        setQuestionData([]);
        setCurrentRecordId(0);
      }
      return;
    }

    message.error('获取问答设置信息失败');
  };

  const saveEntity = async (list: string[]) => {
    let saveDomainExtendQuery = addDomainExtend;
    if (currentRecordId) {
      saveDomainExtendQuery = editDomainExtend;
    }
    const { code, msg } = await saveDomainExtendQuery({
      recommendedQuestions: list.map((question: string) => {
        return { question };
      }),
      id: currentRecordId,
      modelId,
    });

    if (code === 200) {
      return;
    }
    message.error(msg);
  };

  const initPage = async () => {
    queryThemeListData();
  };

  useEffect(() => {
    if (!modelId) {
      return;
    }
    initPage();
  }, [modelId]);

  return (
    <div style={{ width: 800, margin: '20px auto' }}>
      <ProCard bordered title="问题推荐列表">
        <TextAreaCommonEditList
          value={questionData}
          onChange={(list) => {
            saveEntity(list);
            setQuestionData(list);
          }}
        />
      </ProCard>
    </div>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(RecommendedQuestionsSection);

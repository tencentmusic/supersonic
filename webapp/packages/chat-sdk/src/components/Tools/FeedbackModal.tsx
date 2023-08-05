import { Modal, Input, message } from 'antd';
import { CLS_PREFIX } from '../../common/constants';
import { useState } from 'react';

const { TextArea } = Input;

type Props = {
  visible: boolean;
  feedbackValue: string;
  onSubmit: (feedback: string) => void;
  onClose: () => void;
};

const FeedbackModal: React.FC<Props> = ({ visible, feedbackValue, onSubmit, onClose }) => {
  const [feedback, setFeedback] = useState(feedbackValue);
  const prefixCls = `${CLS_PREFIX}-tools`;

  const onOk = () => {
    if (feedback.trim() === '') {
      message.warning('请输入点评内容');
      return;
    }
    onSubmit(feedback);
  };

  const onFeedbackChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setFeedback(e.target.value);
  };

  return (
    <Modal
      open={visible}
      title="点评一下~"
      onOk={onOk}
      onCancel={onClose}
      okText="提交"
      cancelText="取消"
    >
      <div className={`${prefixCls}-feedback-item`}>
        <div className={`${prefixCls}-feedback-item-title`}>评价</div>
        <TextArea
          placeholder="请输入评价"
          rows={3}
          value={feedback}
          onChange={onFeedbackChange}
          onClick={e => {
            e.stopPropagation();
          }}
        />
      </div>
    </Modal>
  );
};

export default FeedbackModal;

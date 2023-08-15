import { PREFIX_CLS } from '../../../common/constants';

type Props = {
  model: string;
  onApplyAuth?: (model: string) => void;
};

const ApplyAuth: React.FC<Props> = ({ model, onApplyAuth }) => {
  const prefixCls = `${PREFIX_CLS}-apply-auth`;

  return (
    <div className={prefixCls}>
      暂无权限，
      {onApplyAuth ? (
        <span
          className={`${prefixCls}-apply`}
          onClick={() => {
            onApplyAuth(model);
          }}
        >
          点击申请
        </span>
      ) : (
        '请联系管理员申请权限'
      )}
    </div>
  );
};

export default ApplyAuth;

import { getToken } from '@/utils/utils';
import { UploadOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';
import { Button, message, Upload } from 'antd';

type Props = {
  buttonType?: string;
  domainId?: number;
  datasetId?: string;
  onFileUploaded?: () => void;
};

const UploadFile = ({ buttonType, domainId, datasetId, onFileUploaded }: Props) => {
  const props: UploadProps = {
    name: 'multipartFile',
    action: `/aibi/api/data/file/uploadFileNew?type=DATASET&domainId=${domainId}${
      datasetId ? `&dataSetId=${datasetId}` : ''
    }`,
    showUploadList: false,
    onChange(info) {
      if (info.file.status !== 'uploading') {
        console.log(info.file, info.fileList);
      }
      if (info.file.status === 'done') {
        message.success('导入成功');
        onFileUploaded?.();
      } else if (info.file.status === 'error') {
        message.error('导入失败');
      }
    },
  };

  return (
    <Upload {...props}>
      {buttonType === 'link' ? (
        <a>导入文件</a>
      ) : (
        <Button icon={<UploadOutlined />}>导入文件</Button>
      )}
    </Upload>
  );
};

export default UploadFile;

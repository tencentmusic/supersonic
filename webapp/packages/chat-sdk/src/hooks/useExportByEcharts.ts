import { message } from 'antd';
import { ECharts } from 'echarts';

export interface ExportByEchartsProps {
  instanceRef: React.MutableRefObject<ECharts | undefined>;
  question: string;
  options?: Parameters<ECharts['getConnectedDataURL']>[0];
}

export const useExportByEcharts = ({ instanceRef, question, options }: ExportByEchartsProps) => {
  const handleSaveAsImage = () => {
    if (instanceRef.current) {
      return instanceRef.current.getConnectedDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: '#fff',
        excludeComponents: ['toolbox'],
        ...options,
      });
    }
  };

  const downloadImage = (url: string) => {
    const a = document.createElement('a');
    a.href = url;
    a.download = `${question}.png`;
    a.click();
  };

  const downloadChartAsImage = () => {
    const url = handleSaveAsImage();
    if (url) {
      downloadImage(url);
      message.success('导出图片成功');
    } else {
      message.error('该条消息暂不支持导出图片');
    }
  };

  return { downloadChartAsImage };
};

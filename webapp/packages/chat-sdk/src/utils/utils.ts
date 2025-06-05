import moment, { Moment } from 'moment';
import { NumericUnit } from '../common/constants';
import { isString } from 'lodash';
import { ColumnType } from '../common/type';

export function formatByDataFormatType(value: number | string, type: ColumnType['dataFormatType'], dataFormat: Partial<ColumnType['dataFormat']> = {}) {
  return `${formatByDecimalPlaces(dataFormat?.needMultiply100 ? +value * 100 : value, dataFormat?.decimalPlaces || 2)}${type === 'percent' ? '%' : ''}`;
}

export function formatByDecimalPlaces(value: number | string, decimalPlaces: number) {
  if (value === null || value === undefined || value === '') {
    return 0;
  }
  if (isNaN(+value) || decimalPlaces < 0 || decimalPlaces > 100) {
    return value;
  }
  let strValue = (+value).toFixed(decimalPlaces);
  if (!/^-?[0-9.]+$/g.test(strValue)) {
    return '0';
  }
  while (strValue.includes('.') && (strValue.endsWith('.') || strValue.endsWith('0'))) {
    strValue = strValue.slice(0, -1);
  }
  return strValue;
}

export function formatByThousandSeperator(value: number | string) {
  if (value === null || value === undefined || value === '') {
    return 0;
  }
  if (isNaN(+value)) {
    return value;
  }
  const partValues = value.toString().split('.');
  partValues[0] = partValues[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return partValues.join('.');
}

export function formatMetric(value: number | string) {
  return formatByThousandSeperator(formatByDecimalPlaces(value, 4));
}

export function formatByUnit(value: number | string, unit: NumericUnit) {
  const numericValue = +value;
  if (isNaN(numericValue) || unit === NumericUnit.None) {
    return value;
  }
  let exponentValue = 0;
  switch (unit) {
    case NumericUnit.TenThousand:
    case NumericUnit.EnTenThousand:
      exponentValue = 4;
      break;
    case NumericUnit.OneHundredMillion:
      exponentValue = 8;
      break;
    case NumericUnit.Thousand:
      exponentValue = 3;
      break;
    case NumericUnit.Million:
      exponentValue = 6;
      break;
    case NumericUnit.Giga:
      exponentValue = 9;
      break;
  }
  return numericValue / Math.pow(10, exponentValue);
}

export const getFormattedValue = (value: number | string, remainZero?: boolean) => {
  if (remainZero && (value === undefined || +value === 0)) {
    return 0;
  }
  if (value === undefined) {
    return '-';
  }
  if (!isFinite(+value)) {
    return value;
  }

  const absNumericValue = Math.abs(+value);

  const unit =
    absNumericValue >= 100000000
      ? NumericUnit.OneHundredMillion
      : absNumericValue >= 10000
      ? NumericUnit.TenThousand
      : NumericUnit.None;

  let formattedValue = formatByUnit(value, unit);
  formattedValue = formatByDecimalPlaces(
    formattedValue,
    unit === NumericUnit.OneHundredMillion ? 2 : absNumericValue < 1 ? 3 : 1
  );
  formattedValue = formatByThousandSeperator(formattedValue);
  if ((typeof formattedValue === 'number' && isNaN(formattedValue)) || +formattedValue === 0) {
    return '-';
  }
  return `${formattedValue}${unit === NumericUnit.None ? '' : unit}`;
};

export const formatNumberWithCN = (num: number) => {
  if (isNaN(num)) return '-';
  if (Math.abs(+num) >= 10000) {
    return (num / 10000).toFixed(1) + '万';
  } else {
    return formatByDecimalPlaces(num, 2);
  }
};

export const groupByColumn = (data: any[], column: string) => {
  return data.reduce((result, item) => {
    const resultData = { ...result };
    const key = item[column];
    if (!resultData[key]) {
      resultData[key] = [];
    }
    resultData[key].push(item);
    return resultData;
  }, {});
};

// 获取任意两个日期中的所有日期
export function enumerateDaysBetweenDates(startDate: Moment, endDate: Moment, dateType?: any) {
  let daysList: any[] = [];
  const day = endDate.diff(startDate, dateType || 'days');
  const format = dateType === 'months' ? 'YYYY-MM' : 'YYYY-MM-DD';
  daysList.push(startDate.format(format));
  for (let i = 1; i <= day; i++) {
    daysList.push(startDate.add(1, dateType || 'days').format(format));
  }
  return daysList;
}

export const normalizeTrendData = (
  resultList: any[],
  dateColumnName: string,
  valueColumnName: string,
  startDate: string,
  endDate: string,
  dateType?: string
) => {
  const dateList = enumerateDaysBetweenDates(moment(startDate), moment(endDate), dateType);
  const result = dateList.map(date => {
    const item = resultList.find(
      result =>
        moment(result[dateColumnName]).format(dateType === 'months' ? 'YYYY-MM' : 'YYYY-MM-DD') ===
        date
    );
    return {
      ...(item || {}),
      [dateColumnName]: date,
      [valueColumnName]: item ? item[valueColumnName] : 0,
    };
  });
  return result;
};

export const getMinMaxDate = (resultList: any[], dateColumnName: string) => {
  const dateList = resultList.map(item => moment(item[dateColumnName]));
  return [moment.min(dateList).format('YYYY-MM-DD'), moment.max(dateList).format('YYYY-MM-DD')];
};

export function hexToRgbObj(hex) {
  var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result
    ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16),
      }
    : null;
}

export function getLightenDarkenColor(col, amt) {
  let result;
  if (col?.includes('rgb')) {
    const [r, g, b, a] = col.match(/\d+/g).map(Number);
    result = { r, g, b, a };
  } else {
    result = hexToRgbObj(col) || {};
  }
  return `rgba(${result.r + amt},${result.g + amt},${result.b + amt}${
    result.a ? `,${result.a}` : ''
  })`;
}

export function getChartLightenColor(col) {
  return getLightenDarkenColor(col, 80);
}

export const isMobile = window.navigator.userAgent.match(/(iPhone|iPod|Android|ios)/i);

export const isIOS = window.navigator.userAgent.match(/(iPhone|iPod|ios)/i);

export const isAndroid = window.navigator.userAgent.match(/(Android)/i);

export function isProd() {
  return process.env.NODE_ENV === 'production';
}

export function setToken(token: string) {
  localStorage.setItem('SUPERSONIC_TOKEN', token);
}

export function getToken() {
  return localStorage.getItem('SUPERSONIC_TOKEN');
}

export const updateMessageContainerScroll = (nodeId?: string) => {
  setTimeout(() => {
    const ele: any = document.getElementById('messageContainer');
    if (ele && ele.scrollHeight > ele.clientHeight) {
      if (nodeId) {
        const node = document.getElementById(nodeId);
        if (node) {
          ele.scrollTop = ele.scrollHeight - node.clientHeight - 130;
        }
      } else {
        ele.scrollTop = ele.scrollHeight;
      }
    }
  }, 100);
};

/**
 * UUID生成器
 * @param len 长度 number
 * @param radix 随机数基数 number
 * @returns {string}
 */
export const uuid = (len: number = 8, radix: number = 62) => {
  const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'.split('');
  const uuid: string[] = [];
  let i;

  if (len) {
    // Compact form
    for (i = 0; i < len; i++) {
      uuid[i] = chars[Math.floor(Math.random() * radix)];
    }
  } else {
    // rfc4122, version 4 form
    let r;

    // rfc4122 requires these characters
    uuid[8] = uuid[13] = uuid[18] = uuid[23] = '-';
    uuid[14] = '4';

    // Fill in random data.  At i==19 set the high bits of clock sequence as
    // per rfc4122, sec. 4.1.5
    for (i = 0; i < 36; i++) {
      if (!uuid[i]) {
        r = Math.floor(Math.random() * 16);
        uuid[i] = chars[i === 19 ? ((r % 4) % 8) + 8 : r];
      }
    }
  }
  return uuid.join('');
};

let utilCanvas: any = null;

export const getTextWidth = (
  text: string,
  fontSize: string = '16px',
  fontWeight: string = 'normal',
  fontFamily: string = 'DINPro Medium'
): number => {
  const canvas = utilCanvas || (utilCanvas = document.createElement('canvas'));
  const context = canvas.getContext('2d');
  context.font = `${fontWeight} ${fontSize} ${fontFamily}`;
  const metrics = context.measureText(text);
  return Math.ceil(metrics.width);
};

export function jsonParse(config: any, defaultReturn?: any) {
  if (!isString(config)) {
    return config;
  }
  if (!config) {
    return defaultReturn;
  }
  try {
    return JSON.parse(config);
  } catch (error) {
    console.log(error);
    return defaultReturn;
  }
}

/**
 * 导出文本文件的函数
 * @param content - 要导出的文本内容
 * @param fileName - 导出的文件名
 * @param mimeType - 文件的 MIME 类型，默认为 'text/plain'
 */
export function exportTextFile(content: string, fileName: string, mimeType: string = 'text/plain') {
  // 创建一个 Blob 对象
  const blob = new Blob([content], { type: mimeType });

  // 创建一个 URL 对象
  const url = URL.createObjectURL(blob);

  // 创建一个 <a> 元素
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;

  // 触发下载
  document.body.appendChild(a);
  a.click();

  // 移除 <a> 元素
  document.body.removeChild(a);

  // 释放 URL 对象
  URL.revokeObjectURL(url);
}

function replacer(key: string, value: any) {
  return value === null ? '' : value; // 将null值转换为空字符串
}

export function exportCsvFile(data: any[]) {
  // 生成CSV内容
  const csvRows: any[] = [];
  const headers = Object.keys(data[0]);
  csvRows.push(headers.join(',')); // 添加表头

  for (const row of data) {
    csvRows.push(headers.map(header => JSON.stringify(row[header], replacer)).join(','));
  }

  // 创建Blob并下载文件
  const csvString = '\ufeff' + csvRows.join('\n');
  const blob = new Blob([csvString], { type: 'text/csv;charset=utf-8;' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'data.csv'; // 指定下载文件名
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url); // 释放Blob URL
}

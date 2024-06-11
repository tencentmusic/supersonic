import { AUTH_TOKEN_KEY, NumericUnit } from '@/common/constants';
import { message } from 'antd';
import numeral from 'numeral';
import copy from 'copy-to-clipboard';
import { isString } from 'lodash';
import CryptoJS from 'crypto-js';

/* eslint no-useless-escape:0  */
const reg =
  /(((^https?:(?:\/\/)?)(?:[-;:&=\+\$,\w]+@)?[A-Za-z0-9.-]+(?::\d+)?|(?:www.|[-;:&=\+\$,\w]+@)[A-Za-z0-9.-]+)((?:\/[\+~%\/.\w-_]*)?\??(?:[-\+=&;%@.\w_]*)#?(?:[\w]*))?)$/;

export const isUrl = (path: string): boolean => reg.test(path);

export function copyText(str: string) {
  copy(str);
  return message.success('复制成功');
}

export function mapToOptions(map: Map<string | number, string>) {
  return [...map].map((item) => ({
    value: item[0],
    label: item[1],
  }));
}

export function objToList(obj: any) {
  return Object.keys(obj).map((key: string) => {
    return {
      value: key,
      label: obj[key],
    };
  });
}

// list 转成树形json
export function listToTree(list: any[], parentId: number) {
  const ret: any[] = [];
  list.forEach((item) => {
    if (item.parentId === parentId) {
      const data = { ...item };
      const leftList = list.filter((l) => l.id !== data.id);
      data.children = listToTree(leftList, data.id);
      ret.push(data);
    }
  });
  return ret;
}

/**
 * 返回格式化后的url params
 * @param {string} originUrl 地址，例如http://www.domain.com/?user=anonymous&id=123&id=456&city=%E5%8C%97%E4%BA%AC&enabled，不传则默认是window.location.href
 * @return {*} 针对以上URL返回以下对象
 * {
  user: 'anonymous',
  id: [123, 456],     // 重复出现的 key 要组装成数组，能被转成数字的就转成数字类型
  city: '北京',        // 中文
  enabled: true,      // 未指定值的 key 约定值为 true
}
 * */
export function getUrlParams(originUrl: string = '') {
  let url: string;
  if (!originUrl) {
    url = decodeURIComponent(window.location.href);
  } else {
    url = decodeURIComponent(originUrl);
  }
  const index = url.indexOf('?');
  if (index === -1) {
    return {};
  }
  const paramString = url.substr(index + 1);
  const paramArr = paramString.split('&');
  const paramObj = {};

  paramArr.forEach((item) => {
    const itemArr = item.split('=');
    const key = itemArr[0];
    const value = itemArr[1];

    if (Array.isArray(paramObj[key])) {
      paramObj[key].push(value);
    } else if (paramObj[key]) {
      paramObj[key] = [paramObj[key], value];
    } else {
      paramObj[key] = value || true;
    }
  });
  return paramObj;
}

/**
 * 删除url中的某个参数
 * @param {String} URL 地址，例如http://www.domain.com/?user=anonymous&id=123&id=456&city=%E5%8C%97%E4%BA%AC&enabled
 * @param {String} key 指定的key
 * @return {String} URL 删除指定参数后的URL
 */
export function deleteUrlQuery(url = '', key = '') {
  const regExp = new RegExp(`[\\&\\?]${key}=([^&#]+)`, 'g');
  return url.replace(regExp, '');
}

/**
 * 获取权限判断后的树数据(项目树选择等组件会用到)
 * @param {Array} treeData
 * @param {Function} authFn 权限过滤函数，如果不传则默认根据数结构的authCodes字段进行权限识别
 * @return {Array} 处理后的树结构数据
 */
type AuthCodes = ('VIEW' | 'EDIT')[];

type AuthFn = {
  (authArr: AuthCodes): boolean;
};

type TreeNode = {
  disabled?: boolean;
  children?: TreeNode[];
  authCodes?: AuthCodes;
};

export function getAuthTreeData(treeData: TreeNode[] = [], authFn?: AuthFn) {
  const EDIT_KEY = 'EDIT';
  return treeData.map((treeNode: any) => {
    const item = { ...treeNode };
    if (typeof authFn === 'function') {
      item.disabled = authFn(item.authCodes);
    } else if (!(item.authCodes ?? []).includes(EDIT_KEY)) {
      item.disabled = true;
    }
    if (item.children) {
      item.children = getAuthTreeData(item.children, authFn);
    }
    return item;
  });
}

export function changeTreeDataTolongId(treeData: TreeNode[] = []) {
  return treeData.map((treeNode: any) => {
    const item = { ...treeNode };
    item.value = item.projectIncreId;
    if (item.children) {
      item.children = changeTreeDataTolongId(item.children);
    }
    return item;
  });
}

export type RegisterBdPostMessageData = { from: string; type: string; payload: any };

export function formatNumber(number: number, formatter = '0,0') {
  return numeral(number).format(formatter);
}

export function getToken() {
  return localStorage.getItem(AUTH_TOKEN_KEY);
}

export function findFirstLeaf(tree: any): any {
  if (tree.children.length === 0) {
    return tree;
  }
  for (const child of tree.children) {
    if (child.children.length === 0) {
      return child;
    } else {
      const data = findFirstLeaf(child);
      if (data) {
        return data;
      }
    }
  }
  return null;
}

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
 * UUID generator
 * @param len length number
 * @param radix random base number
 * @returns {string}
 */
export const uuid = (len: number = 8, radix: number = 62) => {
  const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'.split('');
  const uuid = [];
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

export const isMobile = window.navigator.userAgent.match(/(iPhone|iPod|Android|ios)/i);

export const updateMessageContainerScroll = (step?: boolean, gap?: number) => {
  setTimeout(() => {
    const ele: any = document.getElementById('messageContainer');
    if (ele && ele.scrollHeight > ele.clientHeight) {
      ele.scrollTop = step ? ele.scrollTop + ele.clientHeight - (gap || 130) : ele.scrollHeight;
    }
  }, 100);
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

let utilCanvas: any = null;

export const getTextWidth = (
  text: string,
  fontSize: string = '16px',
  fontWeight: string = 'normal',
  fontFamily: string = 'DINPro Medium',
): number => {
  const canvas = utilCanvas || (utilCanvas = document.createElement('canvas'));
  const context = canvas.getContext('2d');
  context.font = `${fontWeight} ${fontSize} ${fontFamily}`;
  const metrics = context.measureText(text);
  return Math.ceil(metrics.width);
};

export function formatByDecimalPlaces(value: number | string, decimalPlaces: number) {
  if (isNaN(+value)) {
    return value;
  }
  if (decimalPlaces < 0 || decimalPlaces > 100) {
    return value;
  }
  let str = (+value).toFixed(decimalPlaces);
  if (!/^[0-9.]+$/g.test(str)) {
    return '0';
  }
  while (str.includes('.') && (str.endsWith('.') || str.endsWith('0'))) {
    str = str.slice(0, -1);
  }
  return str;
}

export function formatByPercentageData(value: number | string, decimalPlaces: number) {
  const formattedValue: any = Number(value) * 100;
  if (!isFinite(formattedValue)) {
    return value;
  }
  if (formattedValue < 0) {
    return `-${formatByDecimalPlaces(Math.abs(formattedValue), decimalPlaces)}%`;
  }
  return `${formatByDecimalPlaces(formattedValue, decimalPlaces)}%`;
}

export function formatByThousandSeperator(value: number | string) {
  if (isNaN(+value)) {
    return value;
  }

  const parts = value.toString().split('.');
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const formatted = parts.join('.');
  return formatted;
}

export function formatByUnit(value: number | string, unit: NumericUnit) {
  const numericValue = +value;
  if (isNaN(numericValue) || unit === NumericUnit.None) {
    return value;
  }

  let exponent = 0;
  switch (unit) {
    case NumericUnit.TenThousand:
    case NumericUnit.EnTenThousand:
      exponent = 4;
      break;
    case NumericUnit.OneHundredMillion:
      exponent = 8;
      break;
    case NumericUnit.Thousand:
      exponent = 3;
      break;
    case NumericUnit.Million:
      exponent = 6;
      break;
    case NumericUnit.Giga:
      exponent = 9;
      break;
  }
  return numericValue / Math.pow(10, exponent);
}

export const getFormattedValueData = (value: number | string, remainZero?: boolean) => {
  if (remainZero && (value === undefined || +value === 0)) {
    return 0;
  }
  if (value === undefined) {
    return '-';
  }
  if (!isFinite(+value)) {
    return value;
  }
  const unit =
    value >= 100000000
      ? NumericUnit.OneHundredMillion
      : value >= 10000
      ? NumericUnit.EnTenThousand
      : NumericUnit.None;

  let formattedValue = formatByUnit(value, unit);
  formattedValue = formatByDecimalPlaces(
    formattedValue,
    unit === NumericUnit.OneHundredMillion ? 2 : value < 1 ? 3 : 1,
  );
  formattedValue = formatByThousandSeperator(formattedValue);
  if ((typeof formattedValue === 'number' && isNaN(formattedValue)) || +formattedValue === 0) {
    return '-';
  }
  return `${formattedValue}${unit === NumericUnit.None ? '' : unit}`;
};

export function getLeafNodes(treeNodes: any[]): any[] {
  const leafNodes: any[] = [];

  function traverse(node: any) {
    if (!node.children || node.children.length === 0) {
      leafNodes.push(node);
    } else {
      node.children.forEach((child: any) => traverse(child));
    }
  }

  treeNodes.forEach((node) => traverse(node));

  return leafNodes;
}

export function buildTree(nodes: any[]): any[] {
  const map: Record<number, any> = {};
  const roots: any[] = [];

  nodes.forEach((node) => {
    map[node.id] = node;
    node.children = [];
  });

  nodes.forEach((node) => {
    if (node.parentId) {
      const parent = map[node.parentId];
      if (parent) {
        parent.children.push(node);
      }
    } else {
      roots.push(node);
    }
  });

  return roots;
}

export function getLeafList(flatNodes: any[]): any[] {
  const treeNodes = buildTree(flatNodes);
  const leafNodes = getLeafNodes(treeNodes);
  return leafNodes;
}

export function traverseTree(treeData: any[], callback: (node: any) => void) {
  treeData.forEach((node) => {
    callback(node);
    if (node.children?.length > 0) {
      traverseTree(node.children, callback);
    }
  });
  return treeData;
}

export function traverseRoutes(routes, env: string, result: any[] = []) {
  if (!Array.isArray(routes)) {
    return result;
  }

  for (let i = 0; i < routes.length; i++) {
    const route = routes[i];

    if (route.envRedirect) {
      route.redirect = route.envRedirect[env];
    }
    if (route.routes) {
      const filteredRoutes = traverseRoutes(route.routes, env);

      if (Array.isArray(filteredRoutes) && filteredRoutes.length > 0) {
        result.push({
          ...route,
          routes: filteredRoutes,
        });
      }
    } else if (
      (route.envEnableList &&
        (route.envEnableList.includes(env) || route.envEnableList.length === 0)) ||
      !route.envEnableList
    ) {
      result.push(route);
    }
  }
  return result;
}

export function isProd() {
  return process.env.NODE_ENV === 'production';
}

export function isArrayOfValues(array: any) {
  if (array && Array.isArray(array) && array.length > 0) {
    return true;
  }
  return false;
}

type ObjToArrayParams = Record<string, string>;

const keyTypeTran = {
  string: String,
  number: Number,
};
/**
 * obj转成value，label的数组
 * @param _obj
 */
export const objToArray = (_obj: ObjToArrayParams, keyType: string = 'string') => {
  return Object.keys(_obj).map((key) => {
    return {
      value: keyTypeTran[keyType](key),
      label: _obj[key],
    };
  });
};

export function encryptPassword(password: string, username: string) {
  if (!password) {
    return password;
  }
  // TODO This key should be stored in a secure place
  const key = CryptoJS.enc.Utf8.parse('supersonic@2024');
  const srcs = CryptoJS.enc.Utf8.parse(password);
  const encrypted = CryptoJS.AES.encrypt(srcs, key, {mode: CryptoJS.mode.ECB, padding: CryptoJS.pad.Pkcs7});
  return encrypted.toString();

};

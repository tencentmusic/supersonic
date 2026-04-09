import { createContext } from 'react';

export const ChartItemContext = createContext({
  register: (...args: any[]) => {},
  call: (...args: any[]) => {},
});

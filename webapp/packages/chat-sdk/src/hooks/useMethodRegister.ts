import { useCallback, useRef } from 'react';

export const useMethodRegister = (fallback?: (...args: any[]) => any) => {
  const methodStore = useRef<Map<string, (...args: any[]) => any>>(new Map());

  const register = useCallback<(key: string, method: (...args: any[]) => any) => any>(
    (key, method) => {
      methodStore.current.set(key, method);
    },
    [methodStore]
  );

  const call = useCallback<(key: string, ...args: any[]) => any>(
    (key, ...args) => {
      const method = methodStore.current.get(key);
      if (method) {
        return method(...args);
      }
      return fallback?.(...args);
    },
    [methodStore]
  );

  return { register, call };
};

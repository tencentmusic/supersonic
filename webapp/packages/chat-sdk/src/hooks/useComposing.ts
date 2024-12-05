import { type MutableRefObject, useEffect, useState } from 'react';

const isRefObject = <T>(value: any): value is MutableRefObject<T> => {
  return value !== null && typeof value === 'object' && 'current' in value;
};

export const useComposing = (element?: HTMLElement | null | MutableRefObject<HTMLElement>) => {
  const [isComposing, setIsComposing] = useState(false);

  useEffect(() => {
    const handleCompositionStart = (): void => {
      setIsComposing(true);
    };
    const handleCompositionEnd = (): void => {
      setIsComposing(false);
    };

    const dom = isRefObject(element) ? element.current : element;
    const target = dom || window;

    target.addEventListener('compositionstart', handleCompositionStart);
    target.addEventListener('compositionend', handleCompositionEnd);

    return () => {
      target.removeEventListener('compositionstart', handleCompositionStart);
      target.removeEventListener('compositionend', handleCompositionEnd);
    };
  }, [element]);

  return { isComposing, setIsComposing };
};

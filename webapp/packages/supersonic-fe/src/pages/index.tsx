import { useEffect, useTransition } from 'react';
import { useLocation } from '@umijs/max';
import NProgress from 'nprogress';
import 'nprogress/nprogress.css';

NProgress.configure({ showSpinner: false });

const startProgress = () => {
  NProgress.start();
};

const stopProgress = () => {
  NProgress.done();
};

const Page = ({ dom }) => {
  const [isPending, startTransition] = useTransition();
  const location = useLocation();

  useEffect(() => {
    startTransition(() => {
      startProgress();
    });

    return () => {
      stopProgress();
    };
  }, [location]);

  useEffect(() => {
    if (!isPending) {
      stopProgress();
    }
  }, [isPending]);

  return <>{dom}</>;
};

export default Page;

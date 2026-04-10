import React, { useEffect } from 'react';
import { Outlet, history, useLocation, useModel } from '@umijs/max';
import { GROUP_REDIRECTS, resolveGroupRedirect } from './resolveRedirect';

const RouteGroupRedirect: React.FC = () => {
  const location = useLocation();
  const { initialState } = useModel('@@initialState');

  useEffect(() => {
    const target = resolveGroupRedirect(location.pathname, initialState?.authCodes || []);
    if (!target) {
      return;
    }

    if (target !== location.pathname) {
      history.replace(target);
      return;
    }
  }, [initialState?.authCodes, location.pathname]);

  if (GROUP_REDIRECTS[location.pathname]) {
    return null;
  }

  return <Outlet />;
};

export default RouteGroupRedirect;

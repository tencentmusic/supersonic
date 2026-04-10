const { resolveGroupRedirect } = require('../../.tmp-unit/pages/RouteGroupRedirect/resolveRedirect.js');

describe('resolveGroupRedirect', () => {
  it('returns null for non-group paths', () => {
    expect(resolveGroupRedirect('/reports', [])).toBeNull();
  });

  it('redirects analysis center to operations cockpit', () => {
    expect(resolveGroupRedirect('/analysis-center', [])).toBe('/operations-cockpit');
  });

  it('redirects ai query to first accessible page', () => {
    expect(resolveGroupRedirect('/ai-query', ['MENU_AGENT'])).toBe('/agent');
  });

  it('redirects data modeling to tag when show tag is enabled and no other access exists', () => {
    expect(resolveGroupRedirect('/data-modeling', [])).toBe('/tag');
  });

  it('falls back to 401 when no accessible system admin page exists', () => {
    expect(resolveGroupRedirect('/system-admin', [])).toBe('/401');
  });
});

const { filterEmptyGroups, buildGroupedMenu } = require('../../.tmp-unit/utils/menuFilter.js');

describe('filterEmptyGroups', () => {
  it('removes a leaf item with hideInMenu: true', () => {
    const input = [
      { name: 'a', path: '/a', hideInMenu: false },
      { name: 'b', path: '/b', hideInMenu: true },
    ];
    const result = filterEmptyGroups(input);
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe('a');
    expect(result[0].path).toBe('/a');
  });

  it('keeps a parent whose children are all visible', () => {
    const input = [
      {
        name: 'group',
        hideInMenu: false,
        children: [{ name: 'child', path: '/c', hideInMenu: false }],
      },
    ];
    const result = filterEmptyGroups(input);
    expect(result).toHaveLength(1);
    expect(result[0].children).toHaveLength(1);
  });

  it('removes a parent when all children are hidden', () => {
    const input = [
      {
        name: 'group',
        hideInMenu: false,
        children: [
          { name: 'hidden', path: '/h', hideInMenu: true },
          { name: 'also-hidden', path: '/h2', hideInMenu: true },
        ],
      },
    ];
    expect(filterEmptyGroups(input)).toHaveLength(0);
  });

  it('keeps a parent when at least one child is visible', () => {
    const input = [
      {
        name: 'group',
        hideInMenu: false,
        children: [
          { name: 'hidden', path: '/h', hideInMenu: true },
          { name: 'visible', path: '/v', hideInMenu: false },
        ],
      },
    ];
    const result = filterEmptyGroups(input);
    expect(result).toHaveLength(1);
    expect(result[0].children).toHaveLength(1);
    expect(result[0].children[0].name).toBe('visible');
  });

  it('handles leaf nodes without children', () => {
    expect(filterEmptyGroups([{ name: 'leaf', path: '/l', hideInMenu: false }])).toHaveLength(1);
  });

  it('handles deep nesting', () => {
    const input = [
      {
        name: 'top',
        hideInMenu: false,
        children: [
          {
            name: 'mid',
            hideInMenu: false,
            children: [{ name: 'leaf', path: '/leaf', hideInMenu: true }],
          },
        ],
      },
    ];
    expect(filterEmptyGroups(input)).toHaveLength(0);
  });

  it('returns empty array for empty input', () => {
    expect(filterEmptyGroups([])).toEqual([]);
  });
});

describe('buildGroupedMenu', () => {
  it('groups top-level routes under the expected virtual parents', () => {
    const input = [
      { name: 'operationsCockpit', path: '/operations-cockpit', hideInMenu: false },
      { name: 'chat', path: '/chat', hideInMenu: false },
      { name: 'semanticModel', path: '/model/', hideInMenu: false },
      { name: 'platform', path: '/platform', hideInMenu: false },
    ];

    const result = buildGroupedMenu(input);

    expect(result.map((item) => item.path)).toEqual([
      '/analysis-center',
      '/ai-query',
      '/data-modeling',
      '/system-admin',
    ]);
    expect(result[0].children.map((item) => item.path)).toEqual(['/operations-cockpit']);
    expect(result[1].children.map((item) => item.path)).toEqual(['/chat']);
    expect(result[2].children.map((item) => item.path)).toEqual(['/model/']);
    expect(result[3].children.map((item) => item.path)).toEqual(['/platform']);
  });

  it('drops empty virtual groups after child filtering', () => {
    const input = [
      { name: 'chat', path: '/chat', hideInMenu: true },
      { name: 'agent', path: '/agent', hideInMenu: true },
    ];

    expect(buildGroupedMenu(input)).toEqual([]);
  });

  it('keeps ungrouped items after grouped parents', () => {
    const input = [
      { name: 'operationsCockpit', path: '/operations-cockpit', hideInMenu: false },
      { name: 'customPage', path: '/custom-page', hideInMenu: false },
    ];

    const result = buildGroupedMenu(input);

    expect(result.map((item) => item.path)).toEqual(['/analysis-center', '/custom-page']);
  });
});

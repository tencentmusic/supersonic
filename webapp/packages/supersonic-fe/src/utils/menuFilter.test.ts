import { filterEmptyGroups } from './menuFilter';

describe('filterEmptyGroups', () => {
  it('removes a leaf item with hideInMenu: true', () => {
    const input = [
      { name: 'a', path: '/a', hideInMenu: false },
      { name: 'b', path: '/b', hideInMenu: true },
    ];
    expect(filterEmptyGroups(input)).toEqual([
      { name: 'a', path: '/a', hideInMenu: false },
    ]);
  });

  it('keeps a parent whose children are all visible', () => {
    const input = [
      {
        name: 'group',
        hideInMenu: false,
        children: [
          { name: 'child', path: '/c', hideInMenu: false },
        ],
      },
    ];
    const result = filterEmptyGroups(input);
    expect(result).toHaveLength(1);
    expect(result[0].children).toHaveLength(1);
  });

  it('removes a parent when ALL children are hideInMenu: true', () => {
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
    expect((result[0].children as any)[0].name).toBe('visible');
  });

  it('handles a parent with no children field (leaf with no children)', () => {
    const input = [
      { name: 'leaf', path: '/l', hideInMenu: false },
    ];
    expect(filterEmptyGroups(input)).toHaveLength(1);
  });

  it('handles 3-level nesting: removes top-level group when all grandchildren are hidden', () => {
    const input = [
      {
        name: 'top',
        hideInMenu: false,
        children: [
          {
            name: 'mid',
            hideInMenu: false,
            children: [
              { name: 'leaf', path: '/leaf', hideInMenu: true },
            ],
          },
        ],
      },
    ];
    // mid becomes empty → top becomes empty → both filtered
    expect(filterEmptyGroups(input)).toHaveLength(0);
  });

  it('returns empty array for empty input', () => {
    expect(filterEmptyGroups([])).toEqual([]);
  });
});

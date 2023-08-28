import { DownOutlined, UpOutlined } from '@ant-design/icons';
import { useBoolean, useControllableValue } from 'ahooks';
import { Tag } from 'antd';
import classNames from 'classnames';
import { FC, useEffect } from 'react';
import React from 'react';
import styles from './index.less';

const { CheckableTag } = Tag;

export interface TagSelectOptionProps {
  value: string | number | undefined;
  style?: React.CSSProperties;
  checked?: boolean;
  onChange?: (value: string | number, state: boolean) => void;
}

const TagSelectOption: React.FC<TagSelectOptionProps> & {
  isTagSelectOption: boolean;
} = ({ children, checked, onChange, value }) => (
  <CheckableTag
    checked={!!checked}
    key={value}
    onChange={(state) => onChange && onChange(value, state)}
  >
    {children}
  </CheckableTag>
);

TagSelectOption.isTagSelectOption = true;

type TagSelectOptionElement = React.ReactElement<TagSelectOptionProps, typeof TagSelectOption>;
export interface TagSelectProps {
  onChange?: (value: (string | number)[]) => void;
  expandable?: boolean;
  value?: (string | number)[];
  defaultValue?: (string | number)[];
  style?: React.CSSProperties;
  hideCheckAll?: boolean;
  actionsText?: {
    expandText?: React.ReactNode;
    collapseText?: React.ReactNode;
    selectAllText?: React.ReactNode;
  };
  className?: string;
  Option?: TagSelectOptionProps;
  children?: TagSelectOptionElement | TagSelectOptionElement[];
  single?: boolean;
  disableUnCheck?: boolean;
  empty?: boolean;
  isSelectAll?: boolean;
  reverseCheckAll?: boolean;
}

const TagSelect: FC<TagSelectProps> & { Option: typeof TagSelectOption } = (props) => {
  const {
    children,
    hideCheckAll = false,
    className,
    style,
    expandable,
    actionsText = {},
    single = false,
    disableUnCheck = false,
    empty = false,
    isSelectAll = false,
    reverseCheckAll = false,
  } = props;

  const [expand, { toggle }] = useBoolean();

  const [value, setValue] = useControllableValue<(string | number)[] | undefined>(props);

  useEffect(() => {
    if (empty) {
      setValue([]);
    }
  }, [empty]);

  const isTagSelectOption = (node: TagSelectOptionElement) =>
    node &&
    node.type &&
    (node.type.isTagSelectOption || node.type.displayName === 'TagSelectOption');

  const getAllTags = () => {
    const childrenArray = React.Children.toArray(children) as TagSelectOptionElement[];
    const checkedTags = childrenArray
      .filter((child) => isTagSelectOption(child))
      .map((child) => child.props.value);
    return checkedTags || [];
  };

  const onSelectAll = (checked: boolean) => {
    let checkedTags: (string | number)[] = [];
    if (reverseCheckAll) {
      setValue(undefined);
      return;
    }
    if (checked) {
      checkedTags = getAllTags();
    }
    setValue(checkedTags);
  };

  useEffect(() => {
    if (isSelectAll) {
      onSelectAll(true);
    }
  }, []);

  const handleTagChange = (tag: string | number, checked: boolean) => {
    let checkedTags: (string | number)[] = [...(value || [])];
    if (single && checkedTags.length > 0) {
      checkedTags = [checkedTags.join('')];
    }
    const index = checkedTags.indexOf(tag);
    if (checked && index === -1) {
      if (single) {
        checkedTags = [tag];
      } else {
        checkedTags.push(tag);
      }
    } else if (!checked && index > -1 && !disableUnCheck) {
      checkedTags.splice(index, 1);
    }
    setValue(checkedTags.length === 0 ? undefined : checkedTags);
  };

  const checkedAll = getAllTags().length === value?.length;
  const hasChecked = value === undefined ? false : value?.length > 0;

  const {
    expandText = '展开',
    collapseText = '收起',
    selectAllText = reverseCheckAll ? '不限' : '全部',
  } = actionsText;

  const cls = classNames(styles.tagSelect, className, {
    [styles.hasExpandTag]: expandable,
    [styles.expanded]: expand,
  });

  return (
    <div className={cls} style={style}>
      {hideCheckAll ? null : (
        <CheckableTag
          checked={reverseCheckAll ? !hasChecked : checkedAll}
          key="tag-select-__all__"
          onChange={onSelectAll}
        >
          {selectAllText}
        </CheckableTag>
      )}
      {children &&
        React.Children.map(children, (child: TagSelectOptionElement) => {
          if (isTagSelectOption(child)) {
            return React.cloneElement(child, {
              key: `tag-select-${child.props.value}`,
              value: child.props.value,
              checked: value && value.indexOf(child.props.value) > -1,
              onChange: handleTagChange,
            });
          }
          return child;
        })}
      {expandable && (
        <a
          className={styles.trigger}
          onClick={() => {
            toggle();
          }}
        >
          {expand ? (
            <>
              {collapseText} <UpOutlined />
            </>
          ) : (
            <>
              {expandText}
              <DownOutlined />
            </>
          )}
        </a>
      )}
    </div>
  );
};

TagSelect.Option = TagSelectOption;

export default TagSelect;

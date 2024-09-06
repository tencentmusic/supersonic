package com.tencent.supersonic.common.util;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;

public class PageUtils {

    /** convert PageInfo Po to Vo */
    public static <P, V> PageInfo<V> pageInfo2PageInfoVo(PageInfo<P> pageInfoPo) {
        Page<V> page = new Page<>(pageInfoPo.getPageNum(), pageInfoPo.getPageSize());
        page.setTotal(pageInfoPo.getTotal());
        return new PageInfo<>(page);
    }
}

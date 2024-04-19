package com.tencent.supersonic.headless.server.utils;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.testng.Assert;

class NameCheckUtilsTest {

    @Test
    void findForbiddenCharacters() {
        Assert.assertTrue(StringUtils.isBlank(NameCheckUtils.findForbiddenCharacters("访问时长")));
        Assert.assertTrue(StringUtils.isNotBlank(NameCheckUtils.findForbiddenCharacters("访问时长(秒)")));
        Assert.assertTrue(StringUtils.isNotBlank(NameCheckUtils.findForbiddenCharacters("访问时长#")));
        Assert.assertTrue(StringUtils.isNotBlank(NameCheckUtils.findForbiddenCharacters("访问时长%")));
        Assert.assertTrue(StringUtils.isNotBlank(NameCheckUtils.findForbiddenCharacters("访问时长(")));
        Assert.assertTrue(StringUtils.isNotBlank(NameCheckUtils.findForbiddenCharacters("访问时长)")));
        Assert.assertTrue(StringUtils.isNotBlank(NameCheckUtils.findForbiddenCharacters("访问时长（")));
        Assert.assertTrue(StringUtils.isNotBlank(NameCheckUtils.findForbiddenCharacters("访问时长）")));
    }
}
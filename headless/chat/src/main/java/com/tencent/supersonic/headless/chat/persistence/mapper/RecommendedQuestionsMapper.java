package com.tencent.supersonic.headless.chat.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.headless.chat.persistence.dataobject.RecommendedQuestions;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecommendedQuestionsMapper extends BaseMapper<RecommendedQuestions> {

}

package com.tencent.supersonic.headless.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.headless.chat.persistence.dataobject.RecommendedQuestions;
import com.tencent.supersonic.headless.chat.service.RecommendedQuestionsService;
import com.tencent.supersonic.headless.chat.persistence.mapper.RecommendedQuestionsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RecommendedQuestionsServiceImpl extends ServiceImpl<RecommendedQuestionsMapper, RecommendedQuestions>
    implements RecommendedQuestionsService{

    @Override
    public String findQuerySqlByQuestion(Integer agentId, String question) {
        LambdaQueryWrapper<RecommendedQuestions> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecommendedQuestions::getAgentId, agentId)
                .eq(RecommendedQuestions::getQuestion, question);


        // 查第一条数据，若无记录，返回 null
        RecommendedQuestions result = this.getOne(wrapper, false);

        // 如果查到了对象，返回其 querySql，否则返回 null
        return result != null ? result.getQuerySql() : null;
    }
}





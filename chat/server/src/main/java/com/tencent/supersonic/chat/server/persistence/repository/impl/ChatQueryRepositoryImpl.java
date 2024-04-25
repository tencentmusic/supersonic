package com.tencent.supersonic.chat.server.persistence.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.SimilarQueryRecallResp;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDOExample;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDOExample.Criteria;
import com.tencent.supersonic.chat.server.persistence.mapper.ChatParseMapper;
import com.tencent.supersonic.chat.server.persistence.mapper.ChatQueryDOMapper;
import com.tencent.supersonic.chat.server.persistence.mapper.custom.ShowCaseCustomMapper;
import com.tencent.supersonic.chat.server.persistence.repository.ChatQueryRepository;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.PageUtils;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
@Primary
@Slf4j
public class ChatQueryRepositoryImpl implements ChatQueryRepository {

    private final ChatQueryDOMapper chatQueryDOMapper;

    private final ChatParseMapper chatParseMapper;

    private final ShowCaseCustomMapper showCaseCustomMapper;

    public ChatQueryRepositoryImpl(ChatQueryDOMapper chatQueryDOMapper,
                                   ChatParseMapper chatParseMapper,
                                   ShowCaseCustomMapper showCaseCustomMapper) {
        this.chatQueryDOMapper = chatQueryDOMapper;
        this.chatParseMapper = chatParseMapper;
        this.showCaseCustomMapper = showCaseCustomMapper;
    }

    @Override
    public PageInfo<QueryResp> getChatQuery(PageQueryInfoReq pageQueryInfoReq, Long chatId) {
        ChatQueryDOExample example = new ChatQueryDOExample();
        example.setOrderByClause("question_id desc");
        Criteria criteria = example.createCriteria();
        if (chatId != null) {
            criteria.andChatIdEqualTo(chatId);
        }
        if (StringUtils.isNotBlank(pageQueryInfoReq.getUserName())) {
            criteria.andUserNameEqualTo(pageQueryInfoReq.getUserName());
        }
        if (!CollectionUtils.isEmpty(pageQueryInfoReq.getIds())) {
            criteria.andQuestionIdIn(pageQueryInfoReq.getIds());
        }
        PageInfo<ChatQueryDO> pageInfo = PageHelper.startPage(pageQueryInfoReq.getCurrent(),
                        pageQueryInfoReq.getPageSize())
                .doSelectPageInfo(() -> chatQueryDOMapper.selectByExampleWithBLOBs(example));

        PageInfo<QueryResp> chatQueryVOPageInfo = PageUtils.pageInfo2PageInfoVo(pageInfo);
        chatQueryVOPageInfo.setList(
                pageInfo.getList().stream().filter(o -> !StringUtils.isEmpty(o.getQueryResult())).map(this::convertTo)
                        .sorted(Comparator.comparingInt(o -> o.getQuestionId().intValue()))
                        .collect(Collectors.toList()));
        return chatQueryVOPageInfo;
    }

    @Override
    public QueryResp getChatQuery(Long queryId) {
        ChatQueryDO chatQueryDO = getChatQueryDO(queryId);
        if (Objects.isNull(chatQueryDO)) {
            return new QueryResp();
        }
        return convertTo(chatQueryDO);
    }

    @Override
    public ChatQueryDO getChatQueryDO(Long queryId) {
        return chatQueryDOMapper.selectByPrimaryKey(queryId);
    }

    @Override
    public List<QueryResp> queryShowCase(PageQueryInfoReq pageQueryInfoReq, int agentId) {
        return showCaseCustomMapper.queryShowCase(pageQueryInfoReq.getLimitStart(),
                        pageQueryInfoReq.getPageSize(), agentId, pageQueryInfoReq.getUserName())
                .stream().map(this::convertTo)
                .collect(Collectors.toList());
    }

    private QueryResp convertTo(ChatQueryDO chatQueryDO) {
        QueryResp queryResp = new QueryResp();
        BeanUtils.copyProperties(chatQueryDO, queryResp);
        QueryResult queryResult = JsonUtil.toObject(chatQueryDO.getQueryResult(), QueryResult.class);
        if (queryResult != null) {
            queryResult.setQueryId(chatQueryDO.getQuestionId());
            queryResp.setQueryResult(queryResult);
        }
        queryResp.setSimilarQueries(JSONObject.parseArray(chatQueryDO.getSimilarQueries(),
                SimilarQueryRecallResp.class));
        return queryResp;
    }

    @Override
    public Long createChatQuery(ChatParseReq chatParseReq) {
        ChatQueryDO chatQueryDO = new ChatQueryDO();
        chatQueryDO.setChatId(Long.valueOf(chatParseReq.getChatId()));
        chatQueryDO.setCreateTime(new java.util.Date());
        chatQueryDO.setUserName(chatParseReq.getUser().getName());
        chatQueryDO.setQueryText(chatParseReq.getQueryText());
        chatQueryDO.setAgentId(chatParseReq.getAgentId());
        chatQueryDO.setQueryResult("");
        try {
            chatQueryDOMapper.insert(chatQueryDO);
        } catch (Exception e) {
            log.info("database insert has an exception:{}", e.toString());
        }
        return chatQueryDO.getQuestionId();
    }

    @Override
    public List<ChatParseDO> batchSaveParseInfo(ChatParseReq chatParseReq,
                                                ParseResp parseResult, List<SemanticParseInfo> candidateParses) {
        List<ChatParseDO> chatParseDOList = new ArrayList<>();
        getChatParseDO(chatParseReq, parseResult.getQueryId(), candidateParses, chatParseDOList);
        if (!CollectionUtils.isEmpty(candidateParses)) {
            chatParseMapper.batchSaveParseInfo(chatParseDOList);
        }
        return chatParseDOList;
    }

    public void getChatParseDO(ChatParseReq chatParseReq, Long queryId,
                               List<SemanticParseInfo> parses, List<ChatParseDO> chatParseDOList) {
        for (int i = 0; i < parses.size(); i++) {
            ChatParseDO chatParseDO = new ChatParseDO();
            chatParseDO.setChatId(Long.valueOf(chatParseReq.getChatId()));
            chatParseDO.setQuestionId(queryId);
            chatParseDO.setQueryText(chatParseReq.getQueryText());
            chatParseDO.setParseInfo(JsonUtil.toString(parses.get(i)));
            chatParseDO.setIsCandidate(1);
            if (i == 0) {
                chatParseDO.setIsCandidate(0);
            }
            chatParseDO.setParseId(parses.get(i).getId());
            chatParseDO.setCreateTime(new java.util.Date());
            chatParseDO.setUserName(chatParseReq.getUser().getName());
            chatParseDOList.add(chatParseDO);
        }
    }

    @Override
    public ChatQueryDO getLastChatQuery(long chatId) {
        ChatQueryDOExample example = new ChatQueryDOExample();
        example.setOrderByClause("question_id desc");
        example.setLimitEnd(1);
        example.setLimitStart(0);
        Criteria criteria = example.createCriteria();
        criteria.andChatIdEqualTo(chatId);
        List<ChatQueryDO> chatQueryDOS = chatQueryDOMapper.selectByExampleWithBLOBs(example);
        if (!CollectionUtils.isEmpty(chatQueryDOS)) {
            return chatQueryDOS.get(0);
        }
        return null;
    }

    @Override
    public int updateChatQuery(ChatQueryDO chatQueryDO) {
        return chatQueryDOMapper.updateByPrimaryKeyWithBLOBs(chatQueryDO);
    }

    public ChatParseDO getParseInfo(Long questionId, int parseId) {
        return chatParseMapper.getParseInfo(questionId, parseId);
    }

    @Override
    public List<ChatParseDO> getParseInfoList(List<Long> questionIds) {
        return chatParseMapper.getParseInfoList(questionIds);
    }

    @Override
    public Boolean deleteChatQuery(Long questionId) {
        return chatQueryDOMapper.deleteByPrimaryKey(questionId);
    }

    @Override
    public List<ParseResp> getContextualParseInfo(Integer chatId) {
        List<ChatParseDO> chatParseDOList = chatParseMapper.getContextualParseInfo(chatId);
        List<ParseResp> semanticParseInfoList = chatParseDOList.stream().map(parseInfo -> {
            ParseResp parseResp = new ParseResp(chatId, parseInfo.getQueryText());
            List<SemanticParseInfo> selectedParses = new ArrayList<>();
            selectedParses.add(JSONObject.parseObject(parseInfo.getParseInfo(), SemanticParseInfo.class));
            parseResp.setSelectedParses(selectedParses);
            return parseResp;
        }).collect(Collectors.toList());
        return semanticParseInfoList;
    }

}

package com.tencent.supersonic.chat.persistence.repository.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDOExample;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDOExample.Criteria;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.persistence.mapper.ChatParseMapper;
import com.tencent.supersonic.chat.persistence.mapper.ChatQueryDOMapper;
import com.tencent.supersonic.chat.persistence.mapper.custom.ShowCaseCustomMapper;
import com.tencent.supersonic.chat.persistence.repository.ChatQueryRepository;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.PageUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

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
    public List<QueryResp> queryShowCase(PageQueryInfoReq pageQueryInfoReq, int agentId) {
        return showCaseCustomMapper.queryShowCase(pageQueryInfoReq.getLimitStart(),
                        pageQueryInfoReq.getPageSize(), agentId, pageQueryInfoReq.getUserName())
                .stream().map(this::convertTo)
                .collect(Collectors.toList());
    }

    private QueryResp convertTo(ChatQueryDO chatQueryDO) {
        QueryResp queryResponse = new QueryResp();
        BeanUtils.copyProperties(chatQueryDO, queryResponse);
        QueryResult queryResult = JsonUtil.toObject(chatQueryDO.getQueryResult(), QueryResult.class);
        if (queryResult != null) {
            queryResult.setQueryId(chatQueryDO.getQuestionId());
            queryResponse.setQueryResult(queryResult);
        }
        return queryResponse;
    }

    @Override
    public void createChatQuery(QueryResult queryResult, ChatContext chatCtx) {
        ChatQueryDO chatQueryDO = new ChatQueryDO();
        chatQueryDO.setChatId(Long.valueOf(chatCtx.getChatId()));
        chatQueryDO.setCreateTime(new java.util.Date());
        chatQueryDO.setUserName(chatCtx.getUser());
        chatQueryDO.setQueryState(queryResult.getQueryState().ordinal());
        chatQueryDO.setQueryText(chatCtx.getQueryText());
        chatQueryDO.setQueryResult(JsonUtil.toString(queryResult));
        chatQueryDO.setAgentId(chatCtx.getAgentId());
        chatQueryDOMapper.insert(chatQueryDO);
        ChatQueryDO lastChatQuery = getLastChatQuery(chatCtx.getChatId());
        Long queryId = lastChatQuery.getQuestionId();
        queryResult.setQueryId(queryId);
    }

    public Long createChatParse(ParseResp parseResult, ChatContext chatCtx, QueryReq queryReq) {
        ChatQueryDO chatQueryDO = new ChatQueryDO();
        chatQueryDO.setChatId(Long.valueOf(chatCtx.getChatId()));
        chatQueryDO.setCreateTime(new java.util.Date());
        chatQueryDO.setUserName(queryReq.getUser().getName());
        chatQueryDO.setQueryText(queryReq.getQueryText());
        chatQueryDO.setAgentId(queryReq.getAgentId());
        chatQueryDO.setQueryResult("");
        try {
            chatQueryDOMapper.insert(chatQueryDO);
        } catch (Exception e) {
            log.info("database insert has an exception:{}", e.toString());
        }
        Long queryId = chatQueryDO.getQuestionId();
        parseResult.setQueryId(queryId);
        return queryId;
    }

    @Override
    public List<ChatParseDO> batchSaveParseInfo(ChatContext chatCtx, QueryReq queryReq,
                                      ParseResp parseResult,
                                      List<SemanticParseInfo> candidateParses,
                                      List<SemanticParseInfo> selectedParses) {
        Long queryId = createChatParse(parseResult, chatCtx, queryReq);
        List<ChatParseDO> chatParseDOList = new ArrayList<>();
        log.info("candidateParses size:{},selectedParses size:{}", candidateParses.size(), selectedParses.size());
        getChatParseDO(chatCtx, queryReq, queryId, 0, 1, candidateParses, chatParseDOList);
        getChatParseDO(chatCtx, queryReq, queryId, candidateParses.size(), 0, selectedParses, chatParseDOList);
        chatParseMapper.batchSaveParseInfo(chatParseDOList);
        return chatParseDOList;
    }

    @Override
    public void updateChatParseInfo(List<ChatParseDO> chatParseDOS) {
        for (ChatParseDO chatParseDO : chatParseDOS) {
            chatParseMapper.updateParseInfo(chatParseDO);
        }
    }

    public void getChatParseDO(ChatContext chatCtx, QueryReq queryReq, Long queryId, int base, int isCandidate,
                               List<SemanticParseInfo> parses, List<ChatParseDO> chatParseDOList) {
        for (int i = 0; i < parses.size(); i++) {
            ChatParseDO chatParseDO = new ChatParseDO();
            parses.get(i).setId(base + i + 1);
            chatParseDO.setChatId(Long.valueOf(chatCtx.getChatId()));
            chatParseDO.setQuestionId(queryId);
            chatParseDO.setQueryText(queryReq.getQueryText());
            chatParseDO.setParseInfo(JsonUtil.toString(parses.get(i)));
            chatParseDO.setIsCandidate(isCandidate);
            chatParseDO.setParseId(base + i + 1);
            chatParseDO.setCreateTime(new java.util.Date());
            chatParseDO.setUserName(queryReq.getUser().getName());
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
}

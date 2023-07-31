package com.tencent.supersonic.chat.persistence.repository.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.request.QueryRequest;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDOExample;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDOExample.Criteria;
import com.tencent.supersonic.chat.api.pojo.response.QueryResponse;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.persistence.mapper.ChatQueryDOMapper;
import com.tencent.supersonic.chat.persistence.repository.ChatQueryRepository;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.PageUtils;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Repository
@Primary
@Slf4j
public class ChatQueryRepositoryImpl implements ChatQueryRepository {

    private final ChatQueryDOMapper chatQueryDOMapper;

    public ChatQueryRepositoryImpl(ChatQueryDOMapper chatQueryDOMapper) {
        this.chatQueryDOMapper = chatQueryDOMapper;
    }

    @Override
    public PageInfo<QueryResponse> getChatQuery(PageQueryInfoReq pageQueryInfoCommend, long chatId) {
        ChatQueryDOExample example = new ChatQueryDOExample();
        example.setOrderByClause("question_id desc");
        Criteria criteria = example.createCriteria();
        criteria.andChatIdEqualTo(chatId);
        criteria.andUserNameEqualTo(pageQueryInfoCommend.getUserName());

        PageInfo<ChatQueryDO> pageInfo = PageHelper.startPage(pageQueryInfoCommend.getCurrent(),
                        pageQueryInfoCommend.getPageSize())
                .doSelectPageInfo(() -> chatQueryDOMapper.selectByExampleWithBLOBs(example));

        PageInfo<QueryResponse> chatQueryVOPageInfo = PageUtils.pageInfo2PageInfoVo(pageInfo);
        chatQueryVOPageInfo.setList(
                pageInfo.getList().stream().map(this::convertTo)
                        .sorted(Comparator.comparingInt(o -> o.getQuestionId().intValue()))
                        .collect(Collectors.toList()));
        return chatQueryVOPageInfo;
    }

    private QueryResponse convertTo(ChatQueryDO chatQueryDO) {
        QueryResponse queryResponse = new QueryResponse();
        BeanUtils.copyProperties(chatQueryDO, queryResponse);
        QueryResult queryResult = JsonUtil.toObject(chatQueryDO.getQueryResult(), QueryResult.class);
        queryResult.setQueryId(chatQueryDO.getQuestionId());
        queryResponse.setQueryResult(queryResult);
        return queryResponse;
    }

    @Override
    public void createChatQuery(QueryResult queryResult, QueryRequest queryRequest, ChatContext chatCtx) {
        ChatQueryDO chatQueryDO = new ChatQueryDO();
        chatQueryDO.setChatId(Long.valueOf(queryRequest.getChatId()));
        chatQueryDO.setCreateTime(new java.util.Date());
        chatQueryDO.setUserName(queryRequest.getUser().getName());
        chatQueryDO.setQueryState(queryResult.getQueryState().ordinal());
        chatQueryDO.setQueryText(queryRequest.getQueryText());
        chatQueryDO.setQueryResult(JsonUtil.toString(queryResult));
        chatQueryDOMapper.insert(chatQueryDO);
        ChatQueryDO lastChatQuery = getLastChatQuery(queryRequest.getChatId());
        Long queryId = lastChatQuery.getQuestionId();
        queryResult.setQueryId(queryId);
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
}

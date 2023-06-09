package com.tencent.supersonic.chat.infrastructure.repository;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.response.QueryResultResp;
import com.tencent.supersonic.chat.domain.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.domain.dataobject.ChatQueryDOExample;
import com.tencent.supersonic.chat.domain.dataobject.ChatQueryDOExample.Criteria;
import com.tencent.supersonic.chat.domain.pojo.chat.ChatQueryVO;
import com.tencent.supersonic.chat.domain.pojo.chat.PageQueryInfoReq;
import com.tencent.supersonic.chat.domain.repository.ChatQueryRepository;
import com.tencent.supersonic.chat.infrastructure.mapper.ChatQueryDOMapper;
import com.tencent.supersonic.common.util.json.JsonUtil;
import com.tencent.supersonic.common.util.mybatis.PageUtils;
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
    public PageInfo<ChatQueryVO> getChatQuery(PageQueryInfoReq pageQueryInfoCommend, long chatId) {
        ChatQueryDOExample example = new ChatQueryDOExample();
        example.setOrderByClause("question_id desc");
        Criteria criteria = example.createCriteria();
        criteria.andChatIdEqualTo(chatId);
        criteria.andUserNameEqualTo(pageQueryInfoCommend.getUserName());

        PageInfo<ChatQueryDO> pageInfo = PageHelper.startPage(pageQueryInfoCommend.getCurrent(),
                        pageQueryInfoCommend.getPageSize())
                .doSelectPageInfo(() -> chatQueryDOMapper.selectByExampleWithBLOBs(example));

        PageInfo<ChatQueryVO> chatQueryVOPageInfo = PageUtils.pageInfo2PageInfoVo(pageInfo);
        chatQueryVOPageInfo.setList(
                pageInfo.getList().stream().map(this::convertTo)
                        .sorted(Comparator.comparingInt(o -> o.getQuestionId().intValue()))
                        .collect(Collectors.toList()));
        return chatQueryVOPageInfo;
    }

    private ChatQueryVO convertTo(ChatQueryDO chatQueryDO) {
        ChatQueryVO chatQueryVO = new ChatQueryVO();
        BeanUtils.copyProperties(chatQueryDO, chatQueryVO);
        QueryResultResp queryResponse = JsonUtil.toObject(chatQueryDO.getQueryResponse(), QueryResultResp.class);
        queryResponse.setQueryId(chatQueryDO.getQuestionId());
        chatQueryVO.setQueryResponse(queryResponse);
        return chatQueryVO;
    }

    @Override
    public void createChatQuery(QueryResultResp queryResponse, QueryContextReq queryContext, ChatContext chatCtx) {
        ChatQueryDO chatQueryDO = new ChatQueryDO();
        chatQueryDO.setChatId(Long.valueOf(queryContext.getChatId()));
        chatQueryDO.setCreateTime(new java.util.Date());
        chatQueryDO.setUserName(queryContext.getUser().getName());
        chatQueryDO.setQueryState(queryResponse.getQueryState());
        chatQueryDO.setQueryText(queryContext.getQueryText());
        chatQueryDO.setQueryResponse(JsonUtil.toString(queryResponse));
        chatQueryDOMapper.insert(chatQueryDO);
        ChatQueryDO lastChatQuery = getLastChatQuery(queryContext.getChatId());
        Long queryId = lastChatQuery.getQuestionId();
        queryResponse.setQueryId(queryId);
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

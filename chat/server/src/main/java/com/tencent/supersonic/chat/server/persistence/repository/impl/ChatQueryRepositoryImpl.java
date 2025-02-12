package com.tencent.supersonic.chat.server.persistence.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.SimilarQueryRecallResp;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.server.persistence.mapper.ChatParseMapper;
import com.tencent.supersonic.chat.server.persistence.mapper.ChatQueryDOMapper;
import com.tencent.supersonic.chat.server.persistence.mapper.custom.ShowCaseCustomMapper;
import com.tencent.supersonic.chat.server.persistence.repository.ChatQueryRepository;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.PageUtils;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseTimeCostResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ChatQueryDOMapper chatQueryDOMapper;

    @Autowired
    private ChatParseMapper chatParseMapper;

    @Autowired
    private ShowCaseCustomMapper showCaseCustomMapper;

    @Override
    public PageInfo<QueryResp> getChatQuery(PageQueryInfoReq pageQueryInfoReq, Long chatId) {
        QueryWrapper<ChatQueryDO> queryWrapper = new QueryWrapper<>();
        if (chatId != null) {
            queryWrapper.lambda().eq(ChatQueryDO::getChatId, chatId);
        }
        if (StringUtils.isNotBlank(pageQueryInfoReq.getUserName())) {
            queryWrapper.lambda().eq(ChatQueryDO::getUserName, pageQueryInfoReq.getUserName());
        }
        if (!CollectionUtils.isEmpty(pageQueryInfoReq.getIds())) {
            queryWrapper.lambda().in(ChatQueryDO::getQuestionId, pageQueryInfoReq.getIds());
        }
        queryWrapper.lambda().isNotNull(ChatQueryDO::getQueryResult);
        queryWrapper.lambda().ne(ChatQueryDO::getQueryResult, "");
        queryWrapper.lambda().orderByDesc(ChatQueryDO::getQuestionId);
        queryWrapper.lambda().eq(ChatQueryDO::getQueryState, 1);

        PageInfo<ChatQueryDO> pageInfo =
                PageHelper.startPage(pageQueryInfoReq.getCurrent(), pageQueryInfoReq.getPageSize())
                        .doSelectPageInfo(() -> chatQueryDOMapper.selectList(queryWrapper));

        PageInfo<QueryResp> chatQueryVOPageInfo = PageUtils.pageInfo2PageInfoVo(pageInfo);
        chatQueryVOPageInfo.setList(pageInfo.getList().stream()
                .sorted(Comparator.comparingInt(o -> o.getQuestionId().intValue()))
                .map(this::convertTo).collect(Collectors.toList()));
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
        return chatQueryDOMapper.selectById(queryId);
    }

    @Override
    public List<QueryResp> getChatQueries(Integer chatId) {
        QueryWrapper<ChatQueryDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ChatQueryDO::getChatId, chatId);
        queryWrapper.lambda().orderByDesc(ChatQueryDO::getQuestionId);
        queryWrapper.lambda().eq(ChatQueryDO::getQueryState, 1);
        return chatQueryDOMapper.selectList(queryWrapper).stream().map(q -> convertTo(q))
                .collect(Collectors.toList());
    }

    @Override
    public List<QueryResp> queryShowCase(PageQueryInfoReq pageQueryInfoReq, int agentId) {
        return showCaseCustomMapper
                .queryShowCase(pageQueryInfoReq.getLimitStart(), pageQueryInfoReq.getPageSize(),
                        agentId, pageQueryInfoReq.getUserName())
                .stream().map(this::convertTo).collect(Collectors.toList());
    }

    private QueryResp convertTo(ChatQueryDO chatQueryDO) {
        QueryResp queryResp = new QueryResp();
        BeanUtils.copyProperties(chatQueryDO, queryResp);
        QueryResult queryResult =
                JsonUtil.toObject(chatQueryDO.getQueryResult(), QueryResult.class);
        if (queryResult != null) {
            queryResult.setQueryId(chatQueryDO.getQuestionId());
            // fix bugs, compatible with bugs caused by history field changes
            if (!CollectionUtils.isEmpty(queryResult.getQueryColumns())) {
                List<QueryColumn> queryColumns = queryResult.getQueryColumns().stream().peek(x -> {
                    if (StringUtils.isEmpty(x.getBizName())
                            && StringUtils.isNotEmpty(x.getNameEn())) {
                        x.setBizName(x.getNameEn());
                    }
                    if (StringUtils.isNotEmpty(x.getBizName())
                            && StringUtils.isEmpty(x.getNameEn())) {
                        x.setNameEn(x.getBizName());
                    }
                }).collect(Collectors.toList());
                queryResult.setQueryColumns(queryColumns);
            }
            queryResp.setQueryResult(queryResult);
        }
        queryResp.setSimilarQueries(JSONObject.parseArray(chatQueryDO.getSimilarQueries(),
                SimilarQueryRecallResp.class));
        queryResp.setParseTimeCost(
                JsonUtil.toObject(chatQueryDO.getParseTimeCost(), ParseTimeCostResp.class));
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
        chatQueryDO.setQueryState(1);
        try {
            chatQueryDOMapper.insert(chatQueryDO);
        } catch (Exception e) {
            log.info("database insert has an exception:{}", e.toString());
        }
        return chatQueryDO.getQuestionId();
    }

    @Override
    public List<ChatParseDO> batchSaveParseInfo(ChatParseReq chatParseReq,
            ChatParseResp chatParseResp, List<SemanticParseInfo> candidateParses) {
        List<ChatParseDO> chatParseDOList = new ArrayList<>();
        getChatParseDO(chatParseReq, chatParseResp.getQueryId(), candidateParses, chatParseDOList);
        if (!CollectionUtils.isEmpty(candidateParses)) {
            chatParseMapper.batchSaveParseInfo(chatParseDOList);
        }
        return chatParseDOList;
    }

    public void getChatParseDO(ChatParseReq chatParseReq, Long queryId,
            List<SemanticParseInfo> parses, List<ChatParseDO> chatParseDOList) {
        for (int i = 0; i < parses.size(); i++) {
            ChatParseDO chatParseDO = new ChatParseDO();
            chatParseDO.setChatId(chatParseReq.getChatId());
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
    public int updateChatQuery(ChatQueryDO chatQueryDO) {
        return chatQueryDOMapper.updateById(chatQueryDO);
    }

    @Override
    public void updateChatQuery(ChatQueryDO chatQueryDO, UpdateWrapper<ChatQueryDO> updateWrapper) {
        chatQueryDOMapper.update(chatQueryDO, updateWrapper);
    }

    public ChatParseDO getParseInfo(Long questionId, int parseId) {
        return chatParseMapper.getParseInfo(questionId, parseId);
    }

    @Override
    public List<ChatParseDO> getParseInfoList(List<Long> questionIds) {
        return chatParseMapper.getParseInfoList(questionIds);
    }
}

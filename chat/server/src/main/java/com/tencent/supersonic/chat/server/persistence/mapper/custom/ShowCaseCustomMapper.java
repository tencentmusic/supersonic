package com.tencent.supersonic.chat.server.persistence.mapper.custom;

import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShowCaseCustomMapper {

    @Select("<script>" + "SELECT q1.* FROM s2_chat_query q1 " + "JOIN ("
            + "SELECT DISTINCT chat_id FROM ("
            + "SELECT * FROM s2_chat_query WHERE query_state = 1 AND agent_id = ${agentId} AND score = 5 "
            + "<if test='userName != null and userName != \"\"'>AND user_name = #{userName}</if> "
            + "ORDER BY chat_id DESC" + ") a LIMIT #{start}, #{limit}"
            + ") q2 ON q1.chat_id = q2.chat_id " + "WHERE agent_id = ${agentId} AND score = 5"
            + "</script>")
    List<ChatQueryDO> queryShowCase(@Param("start") int start, @Param("limit") int limit,
            @Param("agentId") int agentId, @Param("userName") String userName);

    @Select("<script>" + "SELECT q1.* FROM s2_chat_query q1 " + "JOIN ("
            + "SELECT DISTINCT chat_id FROM ("
            + "SELECT * FROM s2_chat_query WHERE query_state = 1 AND agent_id = ${agentId} "
            + "AND (score IS NULL OR score != 1) "
            + "<if test='userName != null and userName != \"\"'>AND user_name = #{userName}</if> "
            + "ORDER BY chat_id DESC" + ") a LIMIT #{start}, #{limit}"
            + ") q2 ON q1.chat_id = q2.chat_id "
            + "WHERE agent_id = ${agentId} AND query_state = 1 AND (score IS NULL OR score != 1)"
            + "</script>")
    List<ChatQueryDO> queryShowCaseFallback(@Param("start") int start, @Param("limit") int limit,
            @Param("agentId") int agentId, @Param("userName") String userName);
}

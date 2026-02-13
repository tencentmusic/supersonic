package com.tencent.supersonic.chat.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.QueryDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ChatMapper extends BaseMapper<ChatDO> {

    @Insert("INSERT INTO s2_chat (agent_id, chat_name, create_time, last_time, creator, last_question, is_delete, is_top, tenant_id) "
            + "VALUES (#{agentId}, #{chatName}, #{createTime}, #{lastTime}, #{creator}, #{lastQuestion}, #{isDelete}, #{isTop}, #{tenantId})")
    @Options(useGeneratedKeys = true, keyProperty = "chatId")
    boolean createChat(ChatDO chatDO);

    @Select("<script>"
            + "SELECT * FROM s2_chat WHERE creator = #{creator} AND tenant_id = #{tenantId} AND is_delete = 0 "
            + "<if test='agentId != null'>AND agent_id = #{agentId}</if> "
            + "<if test='chatName != null'>AND chat_name = #{chatName}</if> "
            + "ORDER BY is_top DESC, last_time DESC" + "</script>")
    List<ChatDO> getAll(@Param("creator") String creator, @Param("agentId") Integer agentId,
            @Param("tenantId") Long tenantId, @Param("chatName") String chatName);

    @Update("UPDATE s2_chat SET chat_name = #{chatName}, last_time = #{lastTime} WHERE chat_id = #{chatId} AND creator = #{creator} AND tenant_id = #{tenantId}")
    Boolean updateChatName(@Param("chatId") Long chatId, @Param("chatName") String chatName,
            @Param("lastTime") String lastTime, @Param("creator") String creator,
            @Param("tenantId") Long tenantId);

    @Update("UPDATE s2_chat SET last_question = #{lastQuestion}, last_time = #{lastTime} WHERE chat_id = #{chatId}")
    Boolean updateLastQuestion(@Param("chatId") Long chatId,
            @Param("lastQuestion") String lastQuestion, @Param("lastTime") String lastTime);

    @Update("UPDATE s2_chat SET is_top = #{isTop} WHERE chat_id = #{chatId}")
    Boolean updateConversionIsTop(@Param("chatId") Long chatId, @Param("isTop") int isTop);

    @Update("UPDATE s2_chat_query SET score = #{score}, feedback = #{feedback} WHERE question_id = #{id}")
    boolean updateFeedback(QueryDO queryDO);

    @Update("UPDATE s2_chat SET is_delete = 1 WHERE chat_id = #{chatId} AND creator = #{userName} AND tenant_id = #{tenantId}")
    Boolean deleteChat(@Param("chatId") Long chatId, @Param("userName") String userName,
            @Param("tenantId") Long tenantId);
}

package com.ican.mapper;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.Chat;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author：yep
 * @Project：blog-springboot
 * @name：ChatRecordMapper
 * @Date：2024/11/25 16:24
 * @Filename：ChatRecordMapper
 */
@Repository
public interface ChatMapper extends BaseMapper<Chat> {

    List<Chat> selectByCouple(@Param("send") Integer s, @Param("receive") Integer s1);

    List<Chat> selectPageByCouple(
            @Param("send") Integer send,
            @Param("receive") Integer receive,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    Long countByCouple(@Param("send") Integer send, @Param("receive") Integer receive);

    Long countUnreadByCouple(@Param("senderId") Integer senderId, @Param("receiverId") Integer receiverId);

    void markReadByCouple(@Param("senderId") Integer senderId, @Param("receiverId") Integer receiverId);

    void deleteChatRecord(@Param("endTime") DateTime endTime);
}

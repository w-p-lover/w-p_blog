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

    void deleteChatRecord(@Param("endTime") DateTime endTime);
}

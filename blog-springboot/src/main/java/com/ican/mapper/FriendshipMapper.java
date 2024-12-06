package com.ican.mapper;

import com.ican.model.vo.FriendshipVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author：yep
 * @Project：blog-springboot
 * @name：FriendshipsMapper
 * @Date：2024/12/4 10:10
 * @Filename：FriendshipsMapper
 */
@Repository
public interface FriendshipMapper {
    List<Integer> selectFriendshipVOList(@Param("userId") Integer userId);
}

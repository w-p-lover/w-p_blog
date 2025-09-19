package com.ican.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.UserFavorite;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户收藏Mapper
 *
 * @author xcs
 * @date 2025/9/19 12:13
 */
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /**
     * 根据用户ID查询收藏ID列表
     *
     * @param userId 用户ID
     * @return 收藏ID列表
     */
    List<String> selectFavoriteIdsByUserId(@Param("userId") Integer userId);

    /**
     * 根据用户ID和收藏ID查询
     *
     * @param userId 用户ID
     * @param favoriteId 收藏ID
     * @return 用户收藏记录
     */
    UserFavorite selectByUserIdAndFavoriteId(@Param("userId") Integer userId, @Param("favoriteId") String favoriteId);

    /**
     * 根据收藏ID查询用户ID列表
     *
     * @param favoriteId 收藏ID
     * @return 用户ID列表
     */
    List<Integer> selectUserIdsByFavoriteId(@Param("favoriteId") String favoriteId);

    /**
     * 统计收藏数量
     *
     * @param favoriteId 收藏ID
     * @return 收藏数量
     */
    Integer countByFavoriteId(@Param("favoriteId") String favoriteId);
}

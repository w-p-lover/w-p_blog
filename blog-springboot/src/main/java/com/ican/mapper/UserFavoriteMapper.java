package com.ican.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.UserFavorite;
import com.ican.model.vo.ArticleFavoriteCountVO;

import java.util.List;

/**
 * 用户收藏Mapper
 *
 * @author xcs
 * @date 2025/9/19 12:13
 */
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /**
     * 统计文章收藏数
     *
     * @return 文章收藏统计
     */
    List<ArticleFavoriteCountVO> selectArticleFavoriteCount();
}

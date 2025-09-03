package com.ican.strategy;

/**
 * 点赞策略
 *
 * @author xcs
 */
public interface LikeStrategy {

    /**
     * 点赞
     *
     * @param typeId 类型id
     */
    void like(Integer typeId);
}

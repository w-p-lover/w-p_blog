package com.ican.service;

import com.ican.model.vo.HotArticleWarmupResultVO;

import java.util.Collection;

public interface HotArticleWarmupService {

    HotArticleWarmupResultVO warmupTopHotArticles(int limit);

    HotArticleWarmupResultVO warmupArticleIds(Collection<Integer> articleIds);
}

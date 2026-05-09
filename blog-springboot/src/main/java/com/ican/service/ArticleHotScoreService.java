package com.ican.service;

import com.ican.model.vo.ArticleHotScoreRefreshResultVO;
import com.ican.model.vo.ArticleHotScoreVO;

import java.util.List;

public interface ArticleHotScoreService {

    ArticleHotScoreRefreshResultVO refreshHotScores();

    List<ArticleHotScoreVO> listTopHotScores(int limit);
}

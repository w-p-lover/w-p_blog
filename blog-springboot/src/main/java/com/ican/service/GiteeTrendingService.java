package com.ican.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.ican.entity.GiteeTrending;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.vo.GiteeTrendingVO;
import com.ican.model.vo.PageResult;

import java.util.List;


public interface GiteeTrendingService extends IService<GiteeTrending> {
    /**
     * 查看Gitee趋势列表
     *
     * @param condition 条件
     * @return Gitee趋势列表
     */
    PageResult<GiteeTrendingVO> listGiteeTrending(ConditionDTO condition);

    void runPythonSpider(  String language, String category);

    List<String> listGiteeTrendingType();

    List<String> listGiteeTrendingLang();
}

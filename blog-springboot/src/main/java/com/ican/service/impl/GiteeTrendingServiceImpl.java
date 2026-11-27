package com.ican.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.GiteeTrending;
import com.ican.mapper.GiteeTrendingMapper;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.vo.GiteeTrendingVO;
import com.ican.model.vo.PageResult;
import com.ican.service.GiteeTrendingService;
import com.ican.utils.PythonScriptRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GiteeTrendingServiceImpl implements GiteeTrendingService {

    private final GiteeTrendingMapper giteeTrendingMapper;
    private final PythonScriptRunner pythonScriptRunner;

    @Override
    public PageResult<GiteeTrendingVO> listGiteeTrending(ConditionDTO condition) {
        Long count = giteeTrendingMapper.countGiteeTrending(condition);
        if (count == 0) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        List<GiteeTrendingVO> list = giteeTrendingMapper.selectGiteeTrendingVOList(
                (condition.getCurrent() - 1) * condition.getSize(),
                condition.getSize(),
                condition
        );
        List<GiteeTrendingVO> voList = list.stream().map(item -> {
            GiteeTrendingVO vo = new GiteeTrendingVO();
            vo.setId(item.getId());
            vo.setRepoName(item.getRepoName());
            vo.setAuthor(item.getAuthor());
            vo.setStars(item.getStars());
            vo.setForks(item.getForks());
            vo.setLanguage(item.getLanguage());
            vo.setDescription(item.getDescription());
            vo.setUrl(item.getUrl());
            vo.setProjectImage(item.getProjectImage());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, count);
    }

    @Override
    public void runPythonSpider(String language, String category) {
        PythonScriptRunner.PythonExecutionResult result = pythonScriptRunner.run(
                "static/gitee.py",
                List.of(
                        "--language", language == null ? "" : language,
                        "--category", category == null ? "" : category
                ),
                line -> log.info("[Gitee 爬虫日志] {}", line)
        );
        if (!result.isSuccess()) {
            log.error("Gitee 爬虫执行失败，exitCode={}, timedOut={}", result.exitCode(), result.timedOut());
        }
    }

    @Override
    public List<String> listGiteeTrendingType() {
        List<String> trendingTypes = giteeTrendingMapper.countGiteeTrendingTypes();
        if (trendingTypes.isEmpty()) {
            return Collections.emptyList();
        }
        return trendingTypes;
    }

    @Override
    public List<String> listGiteeTrendingLang() {
        List<String> trendingTypes = giteeTrendingMapper.countGiteeTrendingLang();
        if (trendingTypes.isEmpty()) {
            return Collections.emptyList();
        }
        return trendingTypes;
    }

    @Override
    public boolean saveBatch(Collection<GiteeTrending> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<GiteeTrending> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean updateBatchById(Collection<GiteeTrending> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdate(GiteeTrending entity) {
        return false;
    }

    @Override
    public GiteeTrending getOne(Wrapper<GiteeTrending> queryWrapper, boolean throwEx) {
        return null;
    }

    @Override
    public Optional<GiteeTrending> getOneOpt(Wrapper<GiteeTrending> queryWrapper, boolean throwEx) {
        return Optional.empty();
    }

    @Override
    public Map<String, Object> getMap(Wrapper<GiteeTrending> queryWrapper) {
        return Map.of();
    }

    @Override
    public <V> V getObj(Wrapper<GiteeTrending> queryWrapper, Function<? super Object, V> mapper) {
        return null;
    }

    @Override
    public BaseMapper<GiteeTrending> getBaseMapper() {
        return null;
    }

    @Override
    public Class<GiteeTrending> getEntityClass() {
        return null;
    }
}

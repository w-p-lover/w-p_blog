package com.ican.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.GiteeTrending;
import com.ican.mapper.GiteeTrendingMapper;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.vo.GiteeTrendingVO;
import com.ican.model.vo.PageResult;
import com.ican.service.GiteeTrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GiteeTrendingServiceImpl implements GiteeTrendingService {

    @Autowired
    private final GiteeTrendingMapper giteeTrendingMapper;

    /**
     * 查看Gitee趋势列表
     *
     * @param condition 条件
     * @return {@link PageResult<GiteeTrendingVO>} Gitee趋势列表
     */
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

    /**
     * 调用 Python 爬虫脚本
     */
    public void runPythonSpider(String language, String category) {
        try {
            ClassPathResource resource = new ClassPathResource("static/gitee.py");
            File tempFile = File.createTempFile("gitee", ".py");
            Files.copy(resource.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            String pythonCmd = "python"; // 最好改成配置项
            ProcessBuilder pb = new ProcessBuilder(
                    pythonCmd,
                    tempFile.getAbsolutePath(),
                    "--language", language != null ? language : "",
                    "--category", category != null ? category : "");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            System.out.println("--------------------------爬虫执行--------------------------");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[爬虫日志] " + line);
                }
            }
            int exitCode = process.waitFor();
            System.out.println("-------------------爬虫执行完成，退出码：" + exitCode + "-------------------");
        } catch (Exception e) {
            log.error("爬虫任务发生异常：{}", e.getMessage());
        }
    }

    /**
     * 获取项目类型
     *
     * @return {@link List<String>} 项目类型列表
     */
    @Override
    public List<String> listGiteeTrendingType() {
        List<String> trendingTypes = giteeTrendingMapper.countGiteeTrendingTypes();
        if (trendingTypes.isEmpty()) {
            return Collections.emptyList();
        }
        return trendingTypes;
    }

    /**
     * 获取项目语言
     *
     * @return {@link List<String>} 项目语言列表
     */
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

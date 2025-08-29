package com.ican.service.impl;

import com.ican.mapper.GiteeTrendingMapper;
import com.ican.model.dto.ConditionDTO;
import com.ican.entity.GiteeTrending;
import com.ican.model.vo.GiteeTrendingVO;
import com.ican.model.vo.PageResult;
import com.ican.service.GiteeTrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GiteeTrendingServiceImpl implements GiteeTrendingService {

    @Autowired
    private final GiteeTrendingMapper giteeTrendingMapper;

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
    public void runPythonSpider() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "path/to/your/gitee_spider.py");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);  // 输出爬虫日志
            }

            int exitCode = process.waitFor();
            System.out.println("爬虫执行完成，退出码：" + exitCode);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}

package com.ican.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.entity.Category;
import com.ican.mapper.CategoryMapper;
import com.ican.mapper.UserFavoriteMapper;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.DocDTO;
import com.ican.entity.Doc;
import com.ican.mapper.DocMapper;
import com.ican.model.vo.CollabTagVO;
import com.ican.model.vo.DocManagerVO;
import com.ican.model.vo.DocVersionVO;
import com.ican.service.DocService;
import com.ican.model.vo.DocVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocServiceImpl extends ServiceImpl<DocMapper, Doc> implements DocService {

    private final DocMapper docMapper;

    private final UserFavoriteMapper userFavoriteMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CategoryMapper categoryMapper;

    @Override
    public DocVO getDocById(Long id) {
        try {
            Doc entity = docMapper.selectById(id);
            if (entity == null) {
                return null;
            }

            DocVO docVO = new DocVO();
            if (entity.getTags() != null) {
                List<String> tagList = Arrays.asList(entity.getTags().split(","));
                docVO.setTags(tagList);
            }
            if (entity.getCollaborators() != null) {
                List<DocDTO.CollabDTO> collabList =
                        MAPPER.readValue(entity.getCollaborators(),
                                new TypeReference<List<DocDTO.CollabDTO>>() {
                                });
                docVO.setCollaborators(collabList);
            }
            Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                    .select(Category::getCategoryName)
                    .eq(Category::getId, entity.getCategoryId()));
            docVO.setCategoryName(category.getCategoryName());
            BeanUtils.copyProperties(entity, docVO);
            docMapper.incrementViewCount(id);
            return docVO;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("获取共享文章失败ID: " + id, e);
        }
    }

    @Override
    public List<DocVO> listDocs(ConditionDTO condition) {
        List<Doc> entities = docMapper.getDocList(
                (condition.getCurrent() - 1) * condition.getSize(),
                condition.getSize(),
                condition
        );
        return entities.stream().map(this::convertToDocVO).collect(Collectors.toList());
    }

    @Override
    public List<DocManagerVO> getAllDocs() {
        List<Doc> entities = docMapper.getAdminDocList();
        List<DocManagerVO> collect = entities.stream().map(this::convertToDocManagerVO).collect(Collectors.toList());
        return collect;
    }

    private DocVO convertToDocVO(Doc entity) {
        try {
            DocVO vo = new DocVO();
            BeanUtils.copyProperties(entity, vo);

            Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                    .select(Category::getCategoryName)
                    .eq(Category::getId, entity.getCategoryId()));
            vo.setCategoryName(category.getCategoryName());

            // 处理tags字段
            if (entity.getTags() != null) {
                List<String> tagList = Arrays.asList(entity.getTags().split(","));
                vo.setTags(tagList);
            }

            if (entity.getCollaborators() != null) {
                List<DocDTO.CollabDTO> collabList =
                        MAPPER.readValue(entity.getCollaborators(),
                                new TypeReference<List<DocDTO.CollabDTO>>() {
                                });
                vo.setCollaborators(collabList);
            }

            return vo;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("协作人员注入失败: " + entity.getId(), e);
        }
    }

    private DocManagerVO convertToDocManagerVO(Doc entity) {
        DocManagerVO docManagerVO = new DocManagerVO();
        BeanUtils.copyProperties(entity, docManagerVO);

        Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .select(Category::getCategoryName)
                .eq(Category::getId, entity.getCategoryId()));
        docManagerVO.setCategoryName(category.getCategoryName());

        // 处理tags字段
        if (entity.getTags() != null) {
            List<String> tagList = Arrays.asList(entity.getTags().split(","));
            docManagerVO.setTags(tagList);
        }
        return docManagerVO;
    }

    @Override
    public void createDoc(DocDTO docDTO) {
        try {
            Doc entity = new Doc();
            BeanUtils.copyProperties(docDTO, entity);
            Integer categoryId = saveDocCategory(docDTO);
            // 取第一个协作者作为主作者（根据业务需求可调整）
            if (!docDTO.getCollaborators().isEmpty()) {
                entity.setLeadAuthor(docDTO.getCollaborators().get(0).getName());
                entity.setCollaborators(
                        MAPPER.writeValueAsString(docDTO.getCollaborators())
                );
            }
            if (docDTO.getTags() != null) {
                entity.setTags(String.join(",", docDTO.getTags()));
            }
            entity.setLastUpdateDate(LocalDateTime.now());
            entity.setCategoryId(categoryId);
            docMapper.insert(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateDoc(DocDTO docDTO) {
        try {
            Doc entity = docMapper.selectById(docDTO.getId());
            Integer categoryId = saveDocCategory(docDTO);
            if (entity != null) {
                if (!docDTO.getCollaborators().isEmpty()) {
                    entity.setLeadAuthor(docDTO.getCollaborators().get(0).getName());
                    entity.setCollaborators(
                            MAPPER.writeValueAsString(docDTO.getCollaborators())
                    );
                }
                if (docDTO.getTags() != null) {
                    entity.setTags(String.join(",", docDTO.getTags()));
                }
                BeanUtils.copyProperties(docDTO, entity);
                entity.setEditCount(entity.getEditCount() + 1);
                entity.setLastUpdateDate(LocalDateTime.now());
                entity.setCategoryId(categoryId);
                docMapper.updateById(entity);
            }


        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 保存文章分类
     *
     * @param docDTO 文章信息
     * @return 文章分类
     */
    private Integer saveDocCategory(DocDTO docDTO) {
        // 查询分类
        Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .select(Category::getId)
                .eq(Category::getCategoryName, docDTO.getCategoryName()));
        // 分类不存在
        if (Objects.isNull(category)) {
            category = Category.builder()
                    .categoryName(docDTO.getCategoryName())
                    .build();
            // 保存分类
            categoryMapper.insert(category);
        }
        return category.getId();
    }

    @Override
    public List<CollabTagVO> getCollabTags() {
        // 1. 从数据库取出所有以逗号分隔的标签字符串
        List<String> tags = docMapper.getCollabTags();

        // 2. 统计每个标签在所有文档中的出现次数
        Map<String, Long> tagCountMap = tags.stream()
                .filter(Objects::nonNull)
                .flatMap(tagStr -> Arrays.stream(tagStr.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // 3. 生成结果列表
        AtomicInteger index = new AtomicInteger(0);
        return tagCountMap.entrySet().stream()
                .map(e -> CollabTagVO.builder()
                        .id(index.getAndIncrement())
                        .tagName(e.getKey())
                        .docCount(e.getValue().intValue())
                        .build())
                .collect(Collectors.toList());
    }


    @Override
    public List<Integer> listFavouriteDocs(Integer userId) {
        List<String> favoriteIds = userFavoriteMapper.selectFavoriteIdsByUserId(userId);
        return favoriteIds.stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    @Override
    public Integer getDocCount() {
        return docMapper.getTotalCount();
    }


    @Override
    public void addFavorite(Integer userId, Integer docId) {
        docMapper.addFavorite(userId, docId);
    }

    @Override
    public void cancelFavorite(Integer userId, Integer docId) {
        docMapper.cancelFavorite(userId, docId);
    }


    @Override
    public void deleteDoc(Long id) {
        docMapper.deleteById(id);
    }
}

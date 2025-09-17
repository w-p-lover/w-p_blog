package com.ican.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.DocDTO;
import com.ican.entity.Doc;
import com.ican.mapper.DocMapper;
import com.ican.service.DocService;
import com.ican.model.vo.DocVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocServiceImpl extends ServiceImpl<DocMapper, Doc> implements DocService {

    @Autowired
    private DocMapper docMapper;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public DocVO getDocById(Long id) {
        Doc entity = docMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        DocVO docVO = new DocVO();
        BeanUtils.copyProperties(entity, docVO);
        return docVO;
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

    private DocVO convertToDocVO(Doc entity) {
        try {
            DocVO vo = new DocVO();
            BeanUtils.copyProperties(entity, vo);

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


    @Override
    public void createDoc(DocDTO docDTO) {
        Doc entity = new Doc();
        BeanUtils.copyProperties(docDTO, entity);

        // 取第一个协作者作为主作者（根据你的业务需求可调整）
        if (!docDTO.getCollabs().isEmpty()) {
            entity.setLeadAuthor(docDTO.getCollabs().get(0).getName());
        }

        entity.setLastUpdateDate(LocalDateTime.now());
        docMapper.insert(entity);
    }

    @Override
    public void updateDoc(DocDTO docDTO) {
        Doc entity = docMapper.selectById(docDTO.getId());
        if (entity != null) {
            BeanUtils.copyProperties(docDTO, entity);
            entity.setLastUpdateDate(LocalDateTime.now());
            docMapper.updateById(entity);
        }
    }

    @Override
    public void deleteDoc(Long id) {
        docMapper.deleteById(id);
    }
}

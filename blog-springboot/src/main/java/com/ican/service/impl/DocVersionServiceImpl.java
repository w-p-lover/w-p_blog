package com.ican.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.entity.Doc;
import com.ican.entity.DocVersion;
import com.ican.mapper.DocVersionMapper;
import com.ican.model.dto.DocVersionDTO;
import com.ican.model.vo.DocVersionVO;
import com.ican.model.vo.Result;
import com.ican.service.DocVersionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocVersionServiceImpl extends ServiceImpl<DocVersionMapper, DocVersion>  implements DocVersionService {

    private final DocVersionMapper mapper;

    public DocVersionServiceImpl(DocVersionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveDraft(DocVersionDTO dto) {
        DocVersion v = new DocVersion();
        v.setDocId(dto.getDocId());
        v.setContent(dto.getContent());
        v.setAuthor(dto.getAuthor());
        v.setBaseMajor(1);
        v.setBaseMinor(1);
        // 小版本递增逻辑在这里查询后计算
        v.setStatus("DRAFT");
        mapper.insert(v);
    }

    @Override
    @Transactional
    public void submitForPublish(DocVersionDTO dto) {
        String[] version = dto.getVersion().split("\\.");
        DocVersion docVersion = DocVersion.builder()
                .docId(dto.getDocId())
                .content(dto.getContent())
                .author(dto.getAuthor())
                .baseMajor(1)
                .baseMinor(1)
                .status(dto.getStatus())
                .majorVersion(Integer.valueOf(version[0]))
                .minorVersion(Integer.valueOf(version[1]))
                .build();

        mapper.insert(docVersion);
    }

    @Override
    @Transactional
    public void approveVersion(Long docId, Integer major, Integer minor) {
        // 1. 更新 doc_version 状态为 PUBLISHED
        // 2. 更新 doc 表 currentMajor / currentMinor
    }

    @Override
    public Result<List<DocVersionVO>> listHistory(Long docId) {
        List<DocVersion> byDocId = mapper.findByDocId(docId);
        List<DocVersionVO> docVersionVOList = byDocId.stream().map(e -> {
            DocVersionVO vo = new DocVersionVO();
            vo.setId(e.getId());
            vo.setDocId(e.getDocId());
            vo.setVersion(e.getMajorVersion() + "." + e.getMinorVersion());
            vo.setAuthor(e.getAuthor());
            vo.setStatus(e.getStatus());
            vo.setCreatedAt(e.getCreatedAt());
            vo.setContent(e.getContent());
            return vo;
        }).collect(Collectors.toList());

        return Result.success(docVersionVOList);
    }
}

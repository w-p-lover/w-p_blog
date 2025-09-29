package com.ican.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ican.entity.Article;
import com.ican.entity.DocVersion;
import com.ican.model.dto.DocVersionDTO;
import com.ican.model.vo.DocVersionVO;
import com.ican.model.vo.Result;

import java.util.List;

public interface DocVersionService extends IService<DocVersion> {
    void saveDraft(DocVersionDTO dto);

    void submitForPublish(DocVersionDTO dto);

    void approveVersion(Long docId, Integer major, Integer minor);

    Result<List<DocVersionVO>> listHistory(Long docId);
}

package com.ican.controller;

import com.ican.model.dto.DocVersionDTO;
import com.ican.model.vo.DocVersionVO;
import com.ican.model.vo.Result;
import com.ican.service.DocVersionService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/docs/{docId}/versions")
public class DocVersionController {

    @Resource
    private DocVersionService docVersionService;

    @PostMapping("/draft")
    public void saveDraft(@PathVariable Long docId, @RequestBody DocVersionDTO dto) {
        dto.setDocId(docId);
        docVersionService.saveDraft(dto);
    }

    @PostMapping("/submit")
    public void submitForPublish(@PathVariable Long docId, @RequestBody DocVersionDTO dto) {
        dto.setDocId(docId);
        docVersionService.submitForPublish(dto);
    }

    @GetMapping("/history")
    public Result<List<DocVersionVO>> listHistory(@PathVariable Long docId) {
        return docVersionService.listHistory(docId);
    }
}

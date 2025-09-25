package com.ican.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.DocVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DocVersionMapper extends BaseMapper<DocVersion> {

    int insert(DocVersion version);

    @Select("SELECT * FROM t_doc_version WHERE doc_id=#{docId} ORDER BY major_version DESC, minor_version DESC")
    List<DocVersion> findByDocId(Long docId);

    DocVersion findSpecific(Long docId, Integer majorVersion, Integer minorVersion);
}

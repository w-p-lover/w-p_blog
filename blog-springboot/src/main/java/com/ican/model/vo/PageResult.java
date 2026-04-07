package com.ican.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页返回类
 *
 * @author xcs
 * @date 2022/12/03 21:44
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * 分页结果
     */
    private List<T> recordList;

    /**
     * 总数
     */
    private Long count;

}
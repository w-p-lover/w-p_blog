package com.ican.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Canal监听数据DTO
 *
 * @author xcs
 * @see <a href="https://www.bookstack.cn/read/tidb-6.1-zh/ticdc-ticdc-canal-json.md">消息格式</a>
 **/
@Data
public class CanalDTO {

    /**
     * Row 所在的 Database 的名字
     */
    private String database;

    /**
     * Row 所在的 Table 的名字
     */
    private String table;

    /**
     * 该条消息是否为 DDL事件
     */
    private Boolean isDdl;

    /**
     * 事件类型（QUERY、INSERT...)
     */
    private String type;

    /**
     * 当 isDdl 为 false 时，记录每一列的名字及其数据值
     */
    private List<Map<String, Object>> data;

    /**
     * 产生该条消息的事件发生时的13位时间戳
     */
    private Integer es;
}
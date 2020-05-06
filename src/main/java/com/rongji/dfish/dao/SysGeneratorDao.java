package com.rongji.dfish.dao;

import java.util.List;
import java.util.Map;

/**
 * 数据库接口
 */
public interface SysGeneratorDao {

    /**
     * 查询表
     * @param tableName 表名
     */
    Map<String, String> queryTable(String tableName);

    /**
     * 查询表中的列
     * @param tableName 表名
     */
    List<Map<String, String>> queryColumns(String tableName);

    /**
     * 查询表中的数据
     */
    List<Map<String, Object>> queryList(Map<String, Object> map);

}

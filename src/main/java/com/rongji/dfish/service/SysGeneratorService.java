package com.rongji.dfish.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.rongji.dfish.dao.SysGeneratorDao;
import com.rongji.dfish.utils.GenUtils;
import com.rongji.dfish.utils.PageUtils;
import com.rongji.dfish.utils.Query;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器
 *
 * @author CZZ
 */
@Service
public class SysGeneratorService {

    @Autowired
    SysGeneratorDao generatorDao;

    public PageUtils queryList(Query query) {
        Page<?> page = PageHelper.startPage(query.getPage(), query.getLimit());
        List<Map<String, Object>> list = generatorDao.queryList(query);
        return new PageUtils(list, (int)page.getTotal(), query.getLimit(), query.getPage());
    }


    public Map<String, String> queryTable(String tableName) {
        return generatorDao.queryTable(tableName);
    }


    public List<Map<String, String>> queryColumns(String tableName) {
        return generatorDao.queryColumns(tableName);
    }


    public byte[] generatorCode(String[] tableNames, String author, String pageName) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);

        for (String tableName : tableNames) {
            //查询表信息
            Map<String, String> table = queryTable(tableName);
            //查询列信息
            List<Map<String, String>> columns = queryColumns(tableName);
            //生成代码
            GenUtils.generatorCode(table, columns, zip, author, pageName);
        }
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }


}

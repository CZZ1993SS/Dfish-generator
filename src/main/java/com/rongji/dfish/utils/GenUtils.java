package com.rongji.dfish.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.rongji.dfish.entity.ColumnEntity;
import com.rongji.dfish.entity.TableEntity;

public class GenUtils {


    private static List<String> getTemplates() {
        List<String> templates = new ArrayList<>();
        templates.add("template/Entity.java.vm");
        templates.add("template/Service.java.vm");
        templates.add("template/Controller.java.vm");
        templates.add("template/Mapper.java.vm");
        templates.add("template/Param.java.vm");
        templates.add("template/Model.java.vm");
        templates.add("template/Impl.java.vm");
        templates.add("template/Xml.xml.vm");
        return templates;
    }


    /**
     * 获取配置信息
     */
    private static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new RuntimeException("获取配置文件失败，", e);
        }
    }


    /**
     * 生成代码
     */
    public static void generatorCode(Map<String, String> table, List<Map<String, String>> columns, ZipOutputStream zip,
                                     String author, String pageName) {
        // 配置信息
        Configuration config = getConfig();
        boolean hasBigDecimal = false;
        // 表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName"));
        tableEntity.setComments(table.get("tableComment"));
        // 表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), config.getStringArray("tablePrefix"));
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));

        // 列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        for (Map<String, String> column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName"));
            columnEntity.setDataType(column.get("dataType"));
            columnEntity.setComments(column.get("columnComment"));
            columnEntity.setExtra(column.get("extra"));

            // 列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));

            // 列的数据类型，转换成Java类型
            String attrType = config.getString(columnEntity.getDataType(), "unknowType");
            columnEntity.setAttrType(attrType);
            if (!hasBigDecimal && attrType.equals("BigDecimal")) {
                hasBigDecimal = true;
            }
            // 是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey")) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }

            columsList.add(columnEntity);
        }
        tableEntity.setColumns(columsList);

        // 没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        // 设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        String mainPath = config.getString("mainPath");
        mainPath = StringUtils.isBlank(mainPath) ? "com.rongji" : mainPath;
        // 封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableEntity.getTableName());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        conversionColumnType(tableEntity.getColumns());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("mainPath", mainPath);
        map.put("package", config.getString("package"));
        map.put("author", author);
        map.put("dateType", "Date");
        map.put("moduleName", pageName);
        map.put("email", config.getString("email"));
        map.put("date", DateUtils.format(new Date(), DateUtils.DATE_PATTERN));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        map.put("comments", tableEntity.getComments() == null ? "未知的表说明" : tableEntity.getComments());
        VelocityContext context = new VelocityContext(map);

        // 获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            // 渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);

            try {

                /*zip.putNextEntry(new ZipEntry(getFileName(template, tableEntity.getClassName(),
                  config.getString("package" ), config.getString("moduleName" ))));*/

                // 添加到zip
                zip.putNextEntry(new ZipEntry(Objects.requireNonNull(getFileName(template, tableEntity.getClassName(),
                  config.getString("package"), pageName, map))));


                IOUtils.write(sw.toString(), zip, "UTF-8");
                IOUtils.closeQuietly(sw);
                zip.closeEntry();
            } catch (IOException e) {
                throw new RuntimeException("渲染模板失败，表名：" + tableEntity.getTableName(), e);
            }
        }
    }


    /**
     * 表名转换成Java类名
     */
    private static String tableToJava(String tableName, String[] tablePrefixArray) {
        if (null != tablePrefixArray && tablePrefixArray.length > 0) {
            for (String tablePrefix : tablePrefixArray) {
                tableName = tableName.replace(tablePrefix, "");
            }
        }
        return columnToJava(tableName);
    }


    private static void conversionColumnType(List<ColumnEntity> list) {
        for (ColumnEntity column : list) {
            column.setDataType(COLUMN_TYPE_MAP.get(column.getDataType()));
        }
    }


    /**
     * 列名转换成Java属性名
     */
    private static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }


    public final static Map<String, String> COLUMN_TYPE_MAP = new HashMap<>();

    static {
        COLUMN_TYPE_MAP.put("VARCHAR2", "VARCHAR");
        COLUMN_TYPE_MAP.put("CHAR", "VARCHAR");
        COLUMN_TYPE_MAP.put("DATE", "TIMESTAMP");
        COLUMN_TYPE_MAP.put("NUMBER", "NUMERIC");
    }


    /**
     * 获取文件名
     */
    private static String getFileName(String template, String className, String packageName, String moduleName, Map<String, Object> map) {
        String packagePath = "main" + File.separator + "java" + File.separator;
        String xmlPackagePath = "main" + File.separator + "java" + File.separator;
        if (StringUtils.isNotBlank(packageName)) {
            packagePath += packageName.replace(".", File.separator) + File.separator + moduleName + File.separator;
            xmlPackagePath += packageName.replace(".", File.separator) + File.separator;
        }

        if (template.contains("Entity.java.vm")) {
            return packagePath + "entity" + File.separator + className + ".java";
        }

        if (template.contains("Xml.xml.vm")) {
            return xmlPackagePath + "xml" + File.separator + className + "Mapper.xml";
        }

        if (template.contains("Service.java.vm")) {
            return packagePath + "service" + File.separator + className + "Service.java";
        }

        if (template.contains("Controller.java.vm")) {
            return packagePath + "controller" + File.separator + className + "Controller.java";
        }

        if (template.contains("Mapper.java.vm")) {
            return packagePath + "mapper" + File.separator + className + "Mapper.java";
        }

        if (template.contains("Model.java.vm")) {
            return packagePath + "model" + File.separator + className + "Model.java";
        }

        if (template.contains("Param.java.vm")) {
            return packagePath + "param" + File.separator + className + "Param.java";
        }

        if (template.contains("Impl.java.vm")) {
            return packagePath + "service" + File.separator + "impl" + File.separator + className + "Impl.java";
        }

        // if (template.contains("index.vue.vm")) {
        //     return "main" + File.separator + "resources" + File.separator + "src" + File.separator + "views" + File.separator + "modules" +
        //       File.separator + moduleName + File.separator + className.toLowerCase() + ".vue";
        // }
        //
        // if (template.contains("add-or-update.vue.vm")) {
        //     return "main" + File.separator + "resources" + File.separator + "src" + File.separator + "views" + File.separator + "modules" +
        //       File.separator + moduleName + File.separator + className.toLowerCase() + "-add-or-update.vue";
        // }

        return null;
    }

}

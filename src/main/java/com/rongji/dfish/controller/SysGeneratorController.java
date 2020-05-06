package com.rongji.dfish.controller;

import com.rongji.dfish.service.SysGeneratorService;
import com.rongji.dfish.utils.PageUtils;
import com.rongji.dfish.utils.Query;
import com.rongji.dfish.utils.R;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 代码生成器
 */
@Controller
@RequestMapping("/generator")
public class SysGeneratorController {

    @Autowired
    private SysGeneratorService generatorService;

    /**
     * 列表
     */
    @ResponseBody
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils pageUtil = generatorService.queryList(new Query(params));

        return R.ok().put("page", pageUtil);
    }

    /**
     * 生成代码
     */
    @RequestMapping("/code")
    public void code(String tables, HttpServletResponse response) throws IOException {

        byte[] data = generatorService.generatorCode(tables.split(","));

        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"generator.zip\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");

        IOUtils.write(data, response.getOutputStream());
    }


}

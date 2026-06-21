package com.ai.aicodemother.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * AiService 的 MultiFile 方法生成的代码生成结果
 */
@Data
@Description("生成多文件代码的结果")
public class MultiFileCodeResult {

    /**
     * HTML 代码
     */
    @Description("生成的 HTML 代码")
    private String htmlCode;

    /**
     * CSS 代码
     */
    @Description("生成的 CSS 代码")
    private String cssCode;

    /**
     * js 代码
     */
    @Description("生成的 js 代码")
    private String jsCode;

    /**
     * 代码之外的描述
     */
    @Description("代码之外的描述")
    private String description;

}

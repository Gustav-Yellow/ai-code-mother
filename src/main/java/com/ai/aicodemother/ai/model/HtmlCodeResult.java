package com.ai.aicodemother.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * Structured Output
 * 调用 AiService 的 HTML 方法生成的代码生成结果
 */
@Data
@Description("生成 HTML 代码文件的结果")
public class HtmlCodeResult {

    /**
     * HTML 代码
     */
    @Description("生成的 HTML 代码")
    private String htmlCode;

    /**
     * 代码之外的描述
     */
    @Description("代码之外的描述")
    private String description;

}

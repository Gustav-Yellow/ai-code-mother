package com.ai.aicodemother.core.parser;

/**
 * 代码解析器接口
 * @param <T>
 */
public interface CodeParser<T> {

    /**
     * 解析代码
     * @param codeContent 原始代码内容
     * @return 解析后的结果对象
     */
    T parseCode(String codeContent);

}

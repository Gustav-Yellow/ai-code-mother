package com.ai.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新应用请求参数。
 *
 * @author <a href="https://github.com/Gustav-Yellow">GustavYellow</a>
 */
@Data
public class AppUpdateRequest implements Serializable {

    /*
    * 应用id
     */
    private Long id;

    /*
    * 应用名称
     */
    private String appName;

    private static final long serialVersionUID = 1L;

}

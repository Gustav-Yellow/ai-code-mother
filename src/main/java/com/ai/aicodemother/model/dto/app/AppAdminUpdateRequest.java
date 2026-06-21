package com.ai.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员更新应用请求参数。
 */
@Data
public class AppAdminUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面 （admin）
     */
    private String cover;

    /**
     * 优先级 （admin）
     */
    private Integer priority;

    private static final long serialVersionUID = 1L;
}

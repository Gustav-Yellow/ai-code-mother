package com.ai.aicodemother.model.dto.app;

import lombok.Data;

/**
 * 应用部署请求类
 */
@Data
public class AppDeployRequest {

    /**
     * 应用 id
     */
    private Long appId;

    private static final long serialVersionUID = 1L;

}

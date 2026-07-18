package com.ai.aicodemother.service;


import jakarta.servlet.http.HttpServletResponse;

/**
 * 项目下载服务
 */
public interface ProjectDownloadService {

    /**
     * 下载项目压缩包
     *
     * @param projectPath 项目路径
     * @param downloadFileName 下载文件名
     * @param response HttpServletResponse
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);

}

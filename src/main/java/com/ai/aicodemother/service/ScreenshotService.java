package com.ai.aicodemother.service;

public interface ScreenshotService {

    /**
     * 通用的截图服务，可以得到访问地址
     *
     * @param url 访问地址
     * @return 截图访问地址
     */
    String generateAndUploadScreenshot(String url);

}

package com.ai.aicodemother.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebScreenShotUtilsTest {

    @Test
    void saveWebPageScreenShot() {

        String testUrl = "https://www.baidu.com";
        String webPageScreenshot = WebScreenShotUtils.saveWebPageScreenShot(testUrl);
        Assertions.assertNotNull(webPageScreenshot);

    }
}
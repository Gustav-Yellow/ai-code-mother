package com.ai.aicodemother.controller;

import com.ai.aicodemother.common.BaseResponse;
import com.ai.aicodemother.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping("/")
    public BaseResponse<String> healthCheck() {

       return ResultUtils.success("ok");
    }

}

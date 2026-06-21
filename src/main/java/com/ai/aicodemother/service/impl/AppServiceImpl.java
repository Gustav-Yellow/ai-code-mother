package com.ai.aicodemother.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ai.aicodemother.model.entity.App;
import com.ai.aicodemother.mapper.AppMapper;
import com.ai.aicodemother.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/Gustav-Yellow">GustavYellow</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}

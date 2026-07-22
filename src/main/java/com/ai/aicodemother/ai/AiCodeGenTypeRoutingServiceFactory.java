package com.ai.aicodemother.ai;

import com.ai.aicodemother.utils.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI代码生成类型路由服务工厂
 */
@Slf4j
@Configuration
public class AiCodeGenTypeRoutingServiceFactory {

    @Resource
    private ApplicationContext applicationContext;

    /**
     * 创建AI代码生成类型路由服务实例
     * 每当有新的AI代码生成类型路由服务实例需要时，调用此方法创建一个新的实例
     */
    public AiCodeGenTypeRoutingService createAiCodeGenTypeRoutingService() {
        // ChatModel chatModel = SpringContextUtil.getBean("routingChatModelPrototype", ChatModel.class);
        ChatModel chatModel = applicationContext.getBean("routingChatModelPrototype", ChatModel.class);
        return AiServices.builder(AiCodeGenTypeRoutingService.class)
                .chatModel(chatModel)
                .build();
    }

    /**
     * 默认提供一个 Bean
     * @return AI代码生成类型路由服务实例
     */
    @Bean
    public AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService() {
        return createAiCodeGenTypeRoutingService();
    }

}

package com.ai.aicodemother.service;

import com.mybatisflex.core.service.IService;
import com.ai.aicodemother.model.entity.ChatHistoryOriginal;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 * 原始对话历史 服务层。
 * 为 vue 工程模式恢复对话记忆(包含工具调用信息)
 *
 * @author <a href="https://github.com/Gustav-Yellow">GustavYellow</a>
 */
public interface ChatHistoryOriginalService extends IService<ChatHistoryOriginal> {

    /**
     * 添加对话历史
     *
     * @param appId 应用ID
     * @param message 对话内容
     * @param messageType 对话类型
     * @param userId 用户ID
     * @return true 表示添加成功，false 表示添加失败
     */
    boolean addOriginalChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 批量添加对话历史
     *
     * @param chatHistoryOriginalList 对话历史列表
     * @return true 表示添加成功，false 表示添加失败
     */
    boolean addOriginalChatMessageBatch(List<ChatHistoryOriginal> chatHistoryOriginalList);

    /**
     * 根据 appId 关联删除对话历史记录
     *
     * @param appId 应用ID
     * @return true 表示删除成功，false 表示删除失败
     */
    boolean deleteByAppId(Long appId);

    /**
     * 将 APP 的对话历史加载到缓存中
     *
     * @param appId 应用ID
     * @param chatMemory 对话记忆
     * @param maxCount 最大数量
     * @return 加载的数量
     */
    int loadOriginalChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);


}

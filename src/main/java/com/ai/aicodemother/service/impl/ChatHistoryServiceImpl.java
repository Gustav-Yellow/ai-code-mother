package com.ai.aicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ai.aicodemother.constant.UserConstant;
import com.ai.aicodemother.exception.ErrorCode;
import com.ai.aicodemother.exception.ThrowUtils;
import com.ai.aicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.ai.aicodemother.model.entity.App;
import com.ai.aicodemother.model.entity.User;
import com.ai.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.ai.aicodemother.service.AppService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ai.aicodemother.model.entity.ChatHistory;
import com.ai.aicodemother.mapper.ChatHistoryMapper;
import com.ai.aicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/Gustav-Yellow">GustavYellow</a>
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

    @Resource
    @Lazy
    private AppService appService;

    /**
     * 保存用户 user 和 ai 的对话记录
     * @param appId 应用id
     * @param message 消息
     * @param messageType 消息类型
     * @param userId 用户id
     * @return 是否保存成功
     */
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        // 校验参数
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR, "appId不能为空");
        // 校验信息是否为空
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "message不能为空");
        // 校验消息类型是否为空
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        // 校验用户id是否为空
        ThrowUtils.throwIf(userId == null || userId < 0, ErrorCode.PARAMS_ERROR, "用户id不能为空");

        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型: " + messageType);
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistory);
    }

    /**
     * 根据 appId 删除对应的对话记录
     * @param appId 应用 ID
     * @return
     */
    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR, "appId不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("app_id", appId);
        return this.remove(queryWrapper);
    }

    /**
     * 构建根据请求构造查询条件
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 查询条件
     */
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            // 如果查询请求为空，则直接返回空的查询条件
            return queryWrapper;
        }

        // 读取查询请求中的参数
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long userId = chatHistoryQueryRequest.getUserId();
        Long appId = chatHistoryQueryRequest.getAppId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message, StrUtil.isNotBlank(message))
                .eq("messageType", messageType, StrUtil.isNotBlank(messageType))
                .eq("appId", appId)
                .eq("userId", userId);

        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }

        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按照创建时间降序排序
            queryWrapper.orderBy("createTime", false);
        }

        return queryWrapper;
    }

    /**
     * 分页查询应用对话记录
     *
     * @param appId 应用id
     * @param pageSize 分页大小
     * @param lastCreateTime 上一次查询的创建时间
     * @param loginUser 登录用户
     * @return 分页查询结果
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "分页大小不能为空且不能超过50条");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 验证权限：只有应用创建者或者管理员才能查看应用对话记录
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权限访问");

        // 创建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = getQueryWrapper(queryRequest);

        // 分页查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }


}

package com.ai.aicodemother.model.dto.chathistory;

import com.ai.aicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * 对话 id
     */
    private Long id;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * appId
     */
    private Long appId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 游标查询 - 最后一条记录的创建时间
     * 用于分页查询，直接通过在 SQL 中添加范围查询，获取早于此时间的记录
     * < lastCreateTime
     */
    private LocalDateTime lastCreateTime;


    /**
     * 序列化
     */
    private static final long serialVersionUID = 1L;
}

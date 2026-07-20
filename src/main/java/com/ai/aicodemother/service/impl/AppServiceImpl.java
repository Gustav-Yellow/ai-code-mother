package com.ai.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.ai.aicodemother.ai.AiCodeGenTypeRoutingService;
import com.ai.aicodemother.constant.AppConstant;
import com.ai.aicodemother.core.AiCodeGeneratorFacade;
import com.ai.aicodemother.core.builder.VueProjectBuilder;
import com.ai.aicodemother.core.handler.StreamHandlerExecutor;
import com.ai.aicodemother.exception.BusinessException;
import com.ai.aicodemother.exception.ErrorCode;
import com.ai.aicodemother.exception.ThrowUtils;
import com.ai.aicodemother.model.dto.app.AppAddRequest;
import com.ai.aicodemother.model.dto.app.AppQueryRequest;
import com.ai.aicodemother.model.entity.User;
import com.ai.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.ai.aicodemother.model.enums.CodeGenTypeEnum;
import com.ai.aicodemother.model.vo.AppVO;
import com.ai.aicodemother.model.vo.UserVO;
import com.ai.aicodemother.service.*;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ai.aicodemother.model.entity.App;
import com.ai.aicodemother.mapper.AppMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 App 服务层实现。
 *
 * @author <a href="https://github.com/Gustav-Yellow">GustavYellow</a>
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ChatHistoryOriginalService chatHistoryOriginalService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;


    /**
     * 根据 AppId 和消息，生成代码
     * @param appId 应用id
     * @param message 用户消息
     * @param loginUser 登录用户
     * @return 代码生成流
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        // 5. 通过校验之后，添加用户消息到历史对话
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        chatHistoryOriginalService.addOriginalChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());

        // 6. 调用 AI 生成代码（流式）
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);

        // 7. 收集 AI 响应内容并在完成之后记录到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, chatHistoryOriginalService, appId, loginUser, codeGenTypeEnum);
    }

    /**
     * 创建应用
     *
     * @param appAddRequest 应用创建请求
     * @param loginUser 登录用户
     * @return 应用id
     */
    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用 AI 智能选择代码生成类型
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }

    /**
     * 获取应用封装类，将查询到的 App 转换成 AppVO，添加上每个 App 对应的 UserVO
     * @param app 应用实体类 App
     * @return 应用封装类 AppVO
     */
    @Override
    public AppVO getAppVO(App app) {
        //  App 实体类不能为空
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        // 用户信息不为空的话就添加到 AppVO 中的 UserVO
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    /**
     * 批量获取应用封装类，将查询到的 App 转换成 AppVO，添加上每个 App 对应的 UserVO
     * @param appList 应用列表
     * @return 应用封装类列表
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    /**
     * 根据请求构造查询条件
     * @param appQueryRequest 应用查询请求
     * @return 查询条件
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName, StrUtil.isNotBlank(appName))
                .like("cover", cover, StrUtil.isNotBlank(cover))
                .like("initPrompt", initPrompt, StrUtil.isNotBlank(initPrompt))
                .eq("codeGenType", codeGenType, StrUtil.isNotBlank(codeGenType))
                .eq("deployKey", deployKey, StrUtil.isNotBlank(deployKey))
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 根据应用id部署应用
     * @param appId 应用id
     * @param loginUser 登录用户（用于身份校验）
     * @return 部署成功返回可以直接访问的 URL 地址，失败返回 null
     */
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");

        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 3. 权限校验，仅本人可以部署自己的应用
        if (!loginUser.getId().equals(app.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }

        // 4. 校验是否已经有 deployKey，
        String deployKey = app.getDeployKey();
        // 如果没有就生成 6 位 deployKey（字母 + 数字）
        // 在这里生成 deployKey 的时候没有校验是否重复的原因是，数据库中 deployKey 是一个唯一键，相当于在插入的时候就会帮你判断是否唯一
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }

        // 5. 获取代码生成类型，获取原始代码生成路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        // 获取原始代码生成路径 (code_output)
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;

        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }

        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 将 dist 目录作为部署源
            sourceDir = distDir;
            log.info("Vue 项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());
        }

        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
            log.info("Vue 项目部署成功，部署目录: {}", deployDirPath);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }

        // 9. 返回可访问的 URL 地址
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");

        // 10. 返回可访问的 URL 地址
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);

        // 11. 异步生成截图并且更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);

        return appDeployUrl;
    }

    /**
     * 异步生成应用截图并更新应用封面
     *
     * @param appId 应用id
     * @param appUrl 应用部署地址
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新数据库封面
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }

    /**
     * 删除应用的时候，一起删除关联的对话聊天历史
     *
     * @param id 应用id
     * @return 删除成功返回true，失败返回false
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }

        long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            return false;
        }

        // 先删除关联的对话历史，如果删除失败，不影响删除应用
        try {
            chatHistoryService.deleteByAppId(appId);
            chatHistoryOriginalService.deleteByAppId(appId);
        } catch (Exception e) {
            log.error("删除应用关联的对话历史失败: {}", e.getMessage());
        }

        // 删除应用
        return super.removeById(id);
    }
}

package com.ai.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.ai.aicodemother.constant.AppConstant;
import com.ai.aicodemother.core.AiCodeGeneratorFacade;
import com.ai.aicodemother.exception.BusinessException;
import com.ai.aicodemother.exception.ErrorCode;
import com.ai.aicodemother.exception.ThrowUtils;
import com.ai.aicodemother.model.dto.app.AppQueryRequest;
import com.ai.aicodemother.model.entity.User;
import com.ai.aicodemother.model.enums.CodeGenTypeEnum;
import com.ai.aicodemother.model.vo.AppVO;
import com.ai.aicodemother.model.vo.UserVO;
import com.ai.aicodemother.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ai.aicodemother.model.entity.App;
import com.ai.aicodemother.mapper.AppMapper;
import com.ai.aicodemother.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
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
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

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
        // 5. 调用 AI 生成代码
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
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
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
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

        // 6. 检查路径是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码路径不存在，请先生成应用");
        }

        // 7. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }

        // 8. 返回可访问的 URL 地址
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");

        // 9. 返回可访问的 URL 地址
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }
}

package com.ai.aicodemother.service;

import com.ai.aicodemother.model.dto.app.AppAddRequest;
import com.ai.aicodemother.model.dto.app.AppQueryRequest;
import com.ai.aicodemother.model.entity.User;
import com.ai.aicodemother.model.vo.AppVO;
import com.ai.aicodemother.model.vo.LoginUserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.ai.aicodemother.model.entity.App;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/Gustav-Yellow">GustavYellow</a>
 */
public interface AppService extends IService<App> {

    /**
     * 获取应用封装类
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 将 App 列表转换成 AppVO 列表
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 根据请求构造查询条件
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 根据应用id和消息，生成代码
     * @param appId
     * @param message
     * @param loginUser
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 创建 App
     * @param appCreateRequest
     * @param loginUser
     * @return
     */
    Long createApp(AppAddRequest appCreateRequest, User loginUser);

    /**
     * 从 code_output 中提取应用并部署到 code_deploy 中
     * @param id 应用id
     * @param loginUser 登录用户（用于身份校验）
     * @return 部署成功的应用 url 地址 (可访问的部署地址)
     */
    String deployApp(Long id, User loginUser);

    /**
     * 异步生成应用截图并更新应用封面
     *
     * @param appId 应用id
     * @param appUrl 应用部署地址
     */
     void generateAppScreenshotAsync(Long appId, String appUrl);
}

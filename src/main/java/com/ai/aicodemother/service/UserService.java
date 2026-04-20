package com.ai.aicodemother.service;

import com.ai.aicodemother.model.dto.UserQueryRequest;
import com.ai.aicodemother.model.vo.LoginUserVO;
import com.ai.aicodemother.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.ai.aicodemother.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author <a href="https://github.com/Gustav-Yellow">GustavYellow</a>
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 加密
     * @param userPassword 用户密钥
     * @return 加密后的用户密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return 脱敏的已登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取登录用户信息 (不脱敏)
     * @param request HttpServletRequest
     * @return 登录用户信息
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏的用户信息
     * @param user 用户信息
     * @return 脱敏的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户列表
     * @param userList 用户列表
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 根据查询条件构造数据查询参数
     * @param userQueryRequest 用户查询请求
     * @return QueryWrapper 查询参数
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    boolean userLogout(HttpServletRequest request);


}

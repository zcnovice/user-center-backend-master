package com.yupi.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.contant.UserConstant;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.service.UserService;
import com.yupi.usercenter.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.yupi.usercenter.contant.UserConstant.ADMIN_ROLE;
import static com.yupi.usercenter.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 *
 * @author <a href="https://github.com/zcnovice"> zcnovice</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    // https://www.code-nav.cn/

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yupi";

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 1. 校验
        /* isAnyBlank -- 检查多个字符串参数中是否存在任意一个为空白（blank）的情 */
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            return -1;
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        /* 查询满足指定条件的数据库记录总数 */
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);

        /* 这个是向数据库存数据 */
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        /*  */
        return user.getId();
    }


    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 4) {
            return null;
        }
        if (userPassword.length() < 8) {
            return null;
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }
        // 2. 加密
        /* 数据库存储的是加密后的密码，所以要加密后进行对比 */
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在

        /* Mybatis-Plus查询条件构造器 */
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        /*  第一个参数 `"userAccount"`：数据库表中的**字段名**
            第二个参数 `userAccount`：要匹配的**字段值**（变量） */
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        /* 上面两条相当于查询用户名与密码都匹配的数据 */

        /* selectOne -- 根据条件构造器`queryWrapper`查询唯一一条符合条件的数据 */
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            return null;
        }
        // 3. 用户脱敏

        /* (getSafetyUser)用户信息脱敏 -- 保护用户数据安全性 */
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录态
//        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        HttpSession session = request.getSession();
        session.setAttribute(USER_LOGIN_STATE, safetyUser);
        session.getId();
        System.out.println("sessionId:" + session.getId());
        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        // 对手机号脱敏：13800138000 → 138****8000
        safetyUser.setPhone(
                desensitizePhone(originUser.getPhone()));

        // 对邮箱脱敏：test@example.com → te**@example.com
        safetyUser.setEmail(desensitizeEmail(originUser.getEmail()));

        // 注意：以下字段根据业务需求决定是否返回
        safetyUser.setPlanetCode(originUser.getPlanetCode());

        // 通常不返回角色和状态给前端
        // safetyUser.setUserRole(originUser.getUserRole());
        // safetyUser.setUserStatus(originUser.getUserStatus());

        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setTags(originUser.getTags());
        return safetyUser;
    }


    // 手机号脱敏方法
    private String desensitizePhone(String phone) {
        if (StringUtils.isBlank(phone) || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    // 邮箱脱敏方法
    private String desensitizeEmail(String email) {
        if (StringUtils.isBlank(email) || !email.contains("@")) {
            return "****";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return "****" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "****" + email.substring(atIndex);
    }


    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态（移除对应的Session）
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签搜索用户。
     *
     * @param tagNameList 用户要搜索的标签
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        /* 判断tagNameList是否为空 */
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        /* 创建Mybaits-plus条件构造器 */
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //拼接tag
        // like '%Java%' and like '%Python%'
        /* 遍历每一个标签 */
        for (String tagList : tagNameList) {
            /* 给构造器添加模糊查找 */
            queryWrapper = queryWrapper.like("tags", tagList);
        }
        /* 按照构造器查找用户 */
        List<User> userList = userMapper.selectList(queryWrapper);


        /* 对用户信息脱敏然后发送给前端
        *   List<SafetyUser> resultList = new ArrayList<>();  // 创建新列表存放结果
            for (User user : userList) {                     // 遍历原始列表
                SafetyUser safetyUser = getSafetyUser(user);  // 对每个元素应用转换方法
                resultList.add(safetyUser);                  // 将结果添加到新列表
            }
            return resultList;                               // 返回处理后的列表 */

        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }


    /**
     * @Description: 自己编写的搜索代码
     * @return:
     * @Author: zcnovice
     * @date: 2025/6/27 上午11:38
     */
//    public List<User> searchUsersByTags_t(List<String> tagNameList){
//        /* 判断tagNameList是否为空 */
//        if(CollectionUtils.isEmpty(tagNameList))
//        {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR,"无相同标签的用户");
//        }
//        /* 创建Mybaits-plus条件构造器 */
//        QueryWrapper<User> wrapper = new QueryWrapper<>();
//
//        /* 遍历每一个标签 */
//        for(String tag : tagNameList)
//        {
//            wrapper = wrapper.like("tag", tag);
//        }
//        /* 按照构造器查找用户 */
//        List<User> users = userMapper.selectList(wrapper);
//        /* 对用户信息脱敏然后发送给前端 */
//        List<User> users2 = new ArrayList<>();
//        for(User user_t : users2)
//        {
//            /* 循环脱敏 */
//            User safetyUser = getSafetyUser(user_t);
//            users.add(safetyUser);
//        }
//        return users;
//    }


    /**
     * 根据标签搜索用户。(内存过滤版)
     *
     * @param tagNameList 用户要搜索的标签
     * @return
     */
    @Override
    public List<User> searchUsersByTags2(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //1.先查询所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        //2.判断内存中是否包含要求的标签 parallelStream()
        return userList.stream().filter(user -> {
            String tagstr = user.getTags();
//            if (StringUtils.isBlank(tagstr)){
//                return false;
//            }
            Set<String> tempTagNameSet = gson.fromJson(tagstr, new TypeToken<Set<String>>() {
            }.getType());
            //java8  Optional 来判断空
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());

            for (String tagName : tagNameList) {
                if (!tempTagNameSet.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 用户信息修改
     * @param user 当前要更新的用户信息
     * @param loginUser 当前登录的用户信息
     * @return
     */
    @Override
    public int updateUser(User user, User loginUser) {

        /* 判断当前待更新的用户ID是否存在 */
        long userId = user.getId();
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 如果是管理员，允许更新任意信息
        // 如果不是管理员，只允许更新自己的信息
        /* 判断是不是管理员            判断要修改的是不是当前用户 */
        if (!isAdmin(loginUser) && userId != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        /* 根据id查询当前要更新的用户是否存在 */
        User userOld = userMapper.selectById(userId);
        if (userOld == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        /* 存在，并且权限满足    根据ID修改用户  然后返回受影响的行数 */
        return userMapper.updateById(user);
    }

    /**
     * 获取当前用户信息
     * @param request
     * @return
     */
    @Override
    public User getLoginInUser(HttpServletRequest request) {
        /* 如果传入的 HttpServletRequest 对象 request 为 null，说明无法获取会话信息，直接返回 null。 */
        if (request == null) {
            return null;
        }
        /* 获取到当前登录的用户信息 */
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return (User) userObj;
    }

    /**
     * 是否为管理员(通过HttpServletRequest)
     * @param request
     * @return
     */
    public boolean isAdmin(HttpServletRequest request) {
        // 优先从session中取
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 是否为管理员(通过User)
     * @param loginUser
     * @return
     */
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getUserRole() == ADMIN_ROLE;
    }


}


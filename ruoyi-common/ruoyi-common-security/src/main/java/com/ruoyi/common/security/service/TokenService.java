package com.ruoyi.common.security.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.utils.JwtUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.ip.IpUtils;
import com.ruoyi.common.core.utils.uuid.IdUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.api.model.LoginUser;

/**
 * token验证处理
 *
 * @author ruoyi
 */
@Component
public class TokenService
{
    @Autowired
    private RedisService redisService;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private final static long expireTime = CacheConstants.EXPIRATION;

    private final static String ACCESS_TOKEN = CacheConstants.LOGIN_TOKEN_KEY;

    private final static Long MILLIS_MINUTE_TEN = CacheConstants.REFRESH_TIME * MILLIS_MINUTE;

    /**
     * 创建令牌
     */
    public Map<String, Object> createToken(LoginUser loginUser)     //1 登陆创建令牌token
    {
        String token = IdUtils.fastUUID();  //token是uuid
        Long userId = loginUser.getSysUser().getUserId();   //userid
        String userName = loginUser.getSysUser().getUserName(); //username
        loginUser.setToken(token);
        loginUser.setUserid(userId);
        loginUser.setUsername(userName);
        loginUser.setIpaddr(IpUtils.getIpAddr(ServletUtils.getRequest()));  //设置登陆ip
        refreshToken(loginUser);    //刷新登陆用户的令牌token,redis缓存user_key,并设置过期时间,这个时间就是用户的过期时间

        // Jwt存储信息,创建token时会用到
        Map<String, Object> claimsMap = new HashMap<String, Object>();  //设置数据声明
        claimsMap.put(SecurityConstants.USER_KEY, token);   //user_key:token
        claimsMap.put(SecurityConstants.DETAILS_USER_ID, userId);   //user_id
        claimsMap.put(SecurityConstants.DETAILS_USERNAME, userName);    //user_name

        // 接口返回信息
        Map<String, Object> rspMap = new HashMap<String, Object>();
        rspMap.put("access_token", JwtUtils.createToken(claimsMap));    //jwt创建并返回token
        rspMap.put("expires_in", expireTime);   //返回过期时间
        return rspMap;
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser()
    {
        return getLoginUser(ServletUtils.getRequest());
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser(HttpServletRequest request)   //1
    {
        // 获取请求携带的令牌
        String token = SecurityUtils.getToken(request);     //从request中获取token
        return getLoginUser(token);     //获取token对应的登陆用户
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser(String token) //1
    {
        LoginUser user = null;
        try
        {
            if (StringUtils.isNotEmpty(token))
            {
                String userkey = JwtUtils.getUserKey(token);        //根据token获取user key
                user = redisService.getCacheObject(getTokenKey(userkey));   //根据user_key获取token_key, redis中获取token_key缓存的登陆用户对象  login_tokens:<token>
                return user;
            }
        }
        catch (Exception e)
        {
        }
        return user;
    }

    /**
     * 设置用户身份信息
     */
    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 删除用户缓存信息
     */
    public void delLoginUser(String token)  //1
    {
        if (StringUtils.isNotEmpty(token))
        {
            String userkey = JwtUtils.getUserKey(token);    //根据token获得userKey
            redisService.deleteObject(getTokenKey(userkey));    //从redis中删除会话信息,键是login_tokens:<token>
        }
    }

    /**
     * 验证令牌有效期，相差不足120分钟，自动刷新缓存
     *
     * @param loginUser
     */
    public void verifyToken(LoginUser loginUser)
    {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TEN)
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新令牌有效期
     *
     * @param loginUser 登录信息
     */
    public void refreshToken(LoginUser loginUser)   //1  用redis记录令牌有效期
    {
        loginUser.setLoginTime(System.currentTimeMillis()); //设置登陆时间
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE); //设置过期时间: 开始登陆时间+过期时间
        // 根据uuid将loginUser缓存
        String userKey = getTokenKey(loginUser.getToken()); //获取user key  login_tokens:<token>
        redisService.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);  //user key缓存到redis,过期时间
    }

    private String getTokenKey(String token)    //1
    {
        return ACCESS_TOKEN + token;        //login_tokens:<token>
    }
}
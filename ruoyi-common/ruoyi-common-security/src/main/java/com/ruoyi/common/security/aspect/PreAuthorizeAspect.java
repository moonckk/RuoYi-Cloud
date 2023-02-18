package com.ruoyi.common.security.aspect;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import com.ruoyi.common.security.annotation.RequiresLogin;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.annotation.RequiresRoles;
import com.ruoyi.common.security.auth.AuthUtil;

/**
 * 基于 Spring Aop 的注解鉴权
 * 
 * @author kong
 */
@Aspect
@Component
public class PreAuthorizeAspect     //前置鉴权切面
{
    /**
     * 构建
     */
    public PreAuthorizeAspect()
    {
    }

    /**
     * 定义AOP签名 (切入所有使用鉴权注解的方法)
     */
    public static final String POINTCUT_SIGN = " @annotation(com.ruoyi.common.security.annotation.RequiresLogin) || "
            + "@annotation(com.ruoyi.common.security.annotation.RequiresPermissions) || "
            + "@annotation(com.ruoyi.common.security.annotation.RequiresRoles)";        //对指定方法设置切点

    /**
     * 声明AOP签名
     */
    @Pointcut(POINTCUT_SIGN)
    public void pointcut()
    {
    }

    /**
     * 环绕切入
     * 
     * @param joinPoint 切面对象
     * @return 底层方法执行后的返回值
     * @throws Throwable 底层方法抛出的异常
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable
    {
        // 注解鉴权
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); //获取环绕的方法
        checkMethodAnnotation(signature.getMethod());
        try
        {
            // 执行原有逻辑
            Object obj = joinPoint.proceed();
            return obj;
        }
        catch (Throwable e)
        {
            throw e;
        }
    }

    /**
     * 对一个Method对象进行注解检查
     */
    public void checkMethodAnnotation(Method method)
    {
        // 校验 @RequiresLogin 注解
        RequiresLogin requiresLogin = method.getAnnotation(RequiresLogin.class);    //获取需要登陆的注解
        if (requiresLogin != null)
        {
            AuthUtil.checkLogin();  //如果设置注解,则需要检查登陆
        }

        // 校验 @RequiresRoles 注解
        RequiresRoles requiresRoles = method.getAnnotation(RequiresRoles.class);    //需要校验角色权限注解
        if (requiresRoles != null)
        {
            AuthUtil.checkRole(requiresRoles);      //检查角色
        }

        // 校验 @RequiresPermissions 注解
        RequiresPermissions requiresPermissions = method.getAnnotation(RequiresPermissions.class);  //权限认证：必须具有指定权限才能进入该方法
        if (requiresPermissions != null)
        {
            AuthUtil.checkPermi(requiresPermissions);   //检查权限
        }
    }
}

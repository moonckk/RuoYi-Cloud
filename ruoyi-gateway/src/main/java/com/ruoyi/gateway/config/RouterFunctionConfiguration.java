package com.ruoyi.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import com.ruoyi.gateway.handler.ValidateCodeHandler;

/**
 * 路由配置信息
 * 
 * @author ruoyi
 */
@Configuration
public class RouterFunctionConfiguration
{
    @Autowired
    private ValidateCodeHandler validateCodeHandler;    //验证码获取

    @SuppressWarnings("rawtypes")
    @Bean
    public RouterFunction routerFunction()
    {
        //刷新验证码时的请求路径: http://localhost/dev-api/code  ,后端采用响应式编程,接收到这个url后,就调用对应的handler处理
        //public static <T extends ServerResponse> RouterFunction<T> route(RequestPredicate predicate, HandlerFunction<T> handlerFunction)
        //RequestPredicate这是Spring WebFlux响应式编程的组件
        //第一个参数 predicate 参数，是 RequestPredicate 类型，请求谓语，用于匹配请求。可以通过 RequestPredicates 来构建各种条件。
        //第二个参数 handlerFunction 参数，是 RouterFunction 类型，处理器函数。
        return RouterFunctions.route(
                RequestPredicates.GET("/code").and(RequestPredicates.accept(MediaType.TEXT_PLAIN)),
                validateCodeHandler);
    }
}

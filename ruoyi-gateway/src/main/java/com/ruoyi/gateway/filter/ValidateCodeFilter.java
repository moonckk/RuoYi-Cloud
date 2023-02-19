package com.ruoyi.gateway.filter;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.gateway.config.properties.CaptchaProperties;
import com.ruoyi.gateway.service.ValidateCodeService;
import reactor.core.publisher.Flux;

/**
 * 验证码过滤器
 *
 * @author ruoyi
 */
@Component
public class ValidateCodeFilter extends AbstractGatewayFilterFactory<Object>
{
    private final static String[] VALIDATE_URL = new String[] { "/auth/login", "/auth/register" };  //验证码网关过滤器针对的url

    @Autowired
    private ValidateCodeService validateCodeService;

    @Autowired
    private CaptchaProperties captchaProperties;

    private static final String CODE = "code";

    private static final String UUID = "uuid";

    @Override
    public GatewayFilter apply(Object config)
    {
        // Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain);
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 非登录/注册请求或验证码关闭，不处理
            if (!StringUtils.containsAnyIgnoreCase(request.getURI().getPath(), VALIDATE_URL) || !captchaProperties.getEnabled())
            {
                return chain.filter(exchange);  //处理下一个请求
            }

            try
            {
                String rspStr = resolveBodyFromRequest(request);    //获得请求体
                JSONObject obj = JSON.parseObject(rspStr);
                validateCodeService.checkCaptcha(obj.getString(CODE), obj.getString(UUID)); //如果验证码不正确,请求就终止了,抛出验证码错误异常
            }
            catch (Exception e)
            {
                return ServletUtils.webFluxResponseWriter(exchange.getResponse(), e.getMessage());
            }
            return chain.filter(exchange);  //验证码正确,通过过滤器
        };
    }

    private String resolveBodyFromRequest(ServerHttpRequest serverHttpRequest)  //解析servlet请求体
    {
        // 获取请求体
        Flux<DataBuffer> body = serverHttpRequest.getBody();    //获取请求体
        AtomicReference<String> bodyRef = new AtomicReference<>();  //原子引用,线程安全
        //public final Disposable subscribe(Consumer<? super T> consumer)
        body.subscribe(buffer -> {
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer.asByteBuffer());   //解码字节数组
            DataBufferUtils.release(buffer);    //释放buffer
            bodyRef.set(charBuffer.toString()); //
        });
        return bodyRef.get();
    }
}

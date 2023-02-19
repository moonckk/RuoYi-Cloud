package com.ruoyi.gateway.service.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FastByteArrayOutputStream;
import com.google.code.kaptcha.Producer;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.constant.Constants;
import com.ruoyi.common.core.exception.CaptchaException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.sign.Base64;
import com.ruoyi.common.core.utils.uuid.IdUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.gateway.config.properties.CaptchaProperties;
import com.ruoyi.gateway.service.ValidateCodeService;

/**
 * 验证码实现处理
 *
 * @author ruoyi
 */
@Service
public class ValidateCodeServiceImpl implements ValidateCodeService
{
    @Resource(name = "captchaProducer")
    private Producer captchaProducer;

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    @Autowired
    private RedisService redisService;

    @Autowired
    private CaptchaProperties captchaProperties;

    /**
     * 生成验证码
     */
    @Override
    public AjaxResult createCaptcha() throws IOException, CaptchaException
    {
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = captchaProperties.getEnabled();
        ajax.put("captchaEnabled", captchaEnabled);
        if (!captchaEnabled)
        {
            return ajax;
        }

        // 保存验证码信息
        String uuid = IdUtils.simpleUUID(); //获取uuid
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;  //验证码key,captcha_codes:9c07b7bb301a4a528fa9e39c0184ffe4

        String capStr = null, code = null;
        BufferedImage image = null;     //图像buffer

        String captchaType = captchaProperties.getType();
        // 生成验证码
        if ("math".equals(captchaType)) //math类验证码
        {
            String capText = captchaProducerMath.createText();  //获取数学验证码,8-0=?@8
            capStr = capText.substring(0, capText.lastIndexOf("@"));    //运算,用@分割,8-0=?
            code = capText.substring(capText.lastIndexOf("@") + 1);     //结果,8
            image = captchaProducerMath.createImage(capStr);    //根据字符串生成图片buffer
        }
        else if ("char".equals(captchaType))    //字符型验证码
        {
            capStr = code = captchaProducer.createText();
            image = captchaProducer.createImage(capStr);
        }

        redisService.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);   //将验证码结果缓存到redis
        // 转换流信息写出
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try
        {
            ImageIO.write(image, "jpg", os);
        }
        catch (IOException e)
        {
            return AjaxResult.error(e.getMessage());
        }

        ajax.put("uuid", uuid); //将该验证码对应的uuid返回
        ajax.put("img", Base64.encode(os.toByteArray()));   //将该验证码的图片返回
        return ajax;
    }

    /**
     * 校验验证码
     */
    @Override
    public void checkCaptcha(String code, String uuid) throws CaptchaException  //1
    {
        if (StringUtils.isEmpty(code))
        {
            throw new CaptchaException("验证码不能为空");
        }
        if (StringUtils.isEmpty(uuid))
        {
            throw new CaptchaException("验证码已失效");
        }
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;  //拼装验证码的redis key
        String captcha = redisService.getCacheObject(verifyKey);    //获得redis中的结果
        redisService.deleteObject(verifyKey);   //redis删除结果

        if (!code.equalsIgnoreCase(captcha))    //验证码匹配判断
        {
            throw new CaptchaException("验证码错误");
        }
    }
}

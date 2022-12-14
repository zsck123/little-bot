package com.zsck.bot.util;

import love.forte.simbot.component.mirai.message.MiraiMessageContentBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author QQ:825352674
 * @date 2022/7/27 - 9:06
 */
@Component
public class ContextUtil {
    /**
     * 无法Autowired
     */
    private static ApplicationContext applicationContext;

    public static <T> T getBeanByType(Class<T> clazz){
        return applicationContext.getBean(clazz);
    }
    public static MiraiMessageContentBuilderFactory getForwardBuilderFactory(){
        return ((MiraiMessageContentBuilderFactory) applicationContext.getBean("simbotMessageContentBuilderFactory"));
    }

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        ContextUtil.applicationContext = applicationContext;
    }

}

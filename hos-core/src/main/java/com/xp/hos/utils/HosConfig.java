package com.xp.hos.utils;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.Properties;

/**
 * 配置类,封装classpath下所有的properties文件中的属性
 */
public class HosConfig {

    private static HosConfig config;
    private static Properties properties;

    //获取classpath下所有的properties文件,将文件中的属性放到自定义的properties类中
    static {
        PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        config = new HosConfig();
        try {
            config.properties = new Properties();
            Resource[] resources = resourcePatternResolver.getResources("classpath:*.properties");
            for (Resource resource : resources) {
                Properties prop = new Properties();
                //读取properties文件中的属性
                InputStream inputStream = resource.getInputStream();
                //将读取到的属性放到properties对象中
                prop.load(inputStream);
                inputStream.close();

                //将properties对象中的属性放入到全局properties对象中
                config.properties.putAll(prop);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HosConfig() {
    }

    public static HosConfig getConfig() {
        return config;
    }

    //获取properties对象中int类型的属性
    public int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    //获取properties对象中string类型的属性
    public String getString(String key) {
        return properties.get(key).toString();
    }
}

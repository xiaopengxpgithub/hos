package com.xp.hos;

import com.xp.hos.config.ApplicationServerConfig;
import com.xp.hos.datasource.HosDatasourceConfig;
import com.xp.hos.security.SecurityInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.*;

/**
 * 程序启动类
 */
@EnableWebMvc
@EnableAutoConfiguration(exclude = MongoAutoConfiguration.class)
@Configuration
@ComponentScan({"com.xp.hos.*"})
@SpringBootApplication
@Import({HosDatasourceConfig.class, ApplicationServerConfig.class})
@MapperScan("com.xp.hos.*")
public class HosServerApp {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SecurityInterceptor securityInterceptor;

    public static void main(String[] args) {
        SpringApplication.run(HosServerApp.class,args);
    }

    //注册拦截器
    @Bean
    public WebMvcConfigurer configurer(){
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(securityInterceptor);
            }

            //跨域请求
            @Override
            public void addCorsMappings(CorsRegistry registry){
                registry.addMapping("/*").allowedOrigins("*");
            }
        };
    }
}

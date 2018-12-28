package com.xp.hos.datasource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 * 数据库配置类
 */
@Configuration
@MapperScan(basePackages = HosDatasourceConfig.PACKAGE,
sqlSessionFactoryRef ="HosSqlSessionFactory")
public class HosDatasourceConfig {

    static final String PACKAGE="com.xp.hos.**";

    @Primary
    @Bean(name = "HosDataSource")
    public DataSource hosDataSource() throws IOException {
        //1.获取DataSource配置信息
        ResourceLoader loader=new DefaultResourceLoader();
        //读取资源文件中的属性
        InputStream inputStream =loader.getResource("classpath:application.properties")
                .getInputStream();
        Properties properties=new Properties();
        properties.load(inputStream);
        //封装配置属性
        Set<Object> keys= properties.keySet();
        Properties dsproperties=new Properties();
        for (Object key : keys) {
            if (key.toString().startsWith("datasource")){
                dsproperties.put(key.toString().replace("datasource.",""),properties.get(key));
            }
        }

        //2.通过hikariDataSourceFactory生成一个DataSource
        HikariDataSourceFactory dataSourceFactory=new HikariDataSourceFactory();
        dataSourceFactory.setProperties(dsproperties);
        inputStream.close();

        return dataSourceFactory.getDataSource();
    }

    @Primary
    @Bean(name = "HosSqlSessionFactory")
    public SqlSessionFactory hosSqlSessionFactory(@Qualifier("HosDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean=new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);

        //读取mybatis相关配置
        ResourceLoader resourceLoader=new DefaultResourceLoader();
        sqlSessionFactoryBean.setConfigLocation(resourceLoader.getResource("classpath:mybatis-config.xml"));

        //获取sqlsessionFactory相关实例
        sqlSessionFactoryBean.setSqlSessionFactoryBuilder(new SqlSessionFactoryBuilder());
        return sqlSessionFactoryBean.getObject();
    }
}

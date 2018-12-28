package com.xp.hos.test;

import com.xp.hos.datasource.HosDatasourceConfig;
import org.junit.runner.RunWith;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 测试基类,其他的测试类继承这个类即可
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Import(HosDatasourceConfig.class)
@PropertySource("classpath:application.properties")
@ComponentScan("com.xp.hos.*")
@MapperScan("com.xp.hos.*")
public class BaseTest {

}

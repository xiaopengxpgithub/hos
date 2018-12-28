package com.xp.hos.config;

import com.xp.hos.hdfs.HDFSServerImpl;
import com.xp.hos.service.IHosStore;
import com.xp.hos.service.impl.HosStoreImpl;
import com.xp.hos.utils.HosConfig;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class ApplicationServerConfig {

    //获取hbase connection实例,注入bean
    @Bean
    public Connection getConnection() throws IOException {
        org.apache.hadoop.conf.Configuration configuration = HBaseConfiguration.create();
        HosConfig hosConfig = HosConfig.getConfig();

        //hbase 相关配置
        configuration.set("hbase.zookeeper.quorum", hosConfig.getString("hbase.zookeeper.quorum"));
        configuration.set("hbase.zookeeper.property.clientPort", hosConfig.getString("hbase.zookeeper.port"));

        return ConnectionFactory.createConnection(configuration);
    }

    //实例化一个hosstore实例
    @Bean(name = "iHosStore")
    public IHosStore getHosStore(@Autowired Connection connection) throws Exception {
        HosConfig hosConfig = HosConfig.getConfig();
        String zkHosts = hosConfig.getString("hbase.zookeeper.quorum_urls");
        HosStoreImpl hosStore = new HosStoreImpl(connection, new HDFSServerImpl(), zkHosts);

        return hosStore;
    }
}

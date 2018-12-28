package com.xp.hos.serivce.imple;

import com.xp.hos.serivce.IHosClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HosClientFactory {

    private static Map<String, IHosClient> clientMapCache = new ConcurrentHashMap<String, IHosClient>();

    public static IHosClient getOrCreateHosClient(String endpoints, String token) {
        String key = endpoints + "_" + token;

        //判断clientMapCache是否含有hosclient
        if (clientMapCache.containsKey(key)){
            return clientMapCache.get(key);
        }

        //创建client放到cache中
        IHosClient client=new HosClientImpl(endpoints,token);
        clientMapCache.put(key,client);

        return client;
    }

}

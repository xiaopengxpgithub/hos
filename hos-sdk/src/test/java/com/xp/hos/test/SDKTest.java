package com.xp.hos.test;

import com.xp.hos.pojo.BucketModel;
import com.xp.hos.serivce.IHosClient;
import com.xp.hos.serivce.imple.HosClientFactory;

import java.io.IOException;
import java.util.List;

public class SDKTest {

    //superAdmin角色的userId
    private static String token="28fb420e233f48b39442351d07467320";
    private static String endPoints="http://localhost:9080";

    public static void main(String[] args) throws IOException {
        final IHosClient client=HosClientFactory.getOrCreateHosClient(endPoints,token);
        List<BucketModel> bucketModels=client.listBuckets();

        bucketModels.forEach(bucketModel -> {
            System.out.println(bucketModel.toString());
        });
    }
}

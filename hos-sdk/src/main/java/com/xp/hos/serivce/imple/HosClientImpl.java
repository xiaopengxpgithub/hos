package com.xp.hos.serivce.imple;

import com.xp.hos.pojo.BucketModel;
import com.xp.hos.pojo.HosHeaders;
import com.xp.hos.pojo.HosObjectSummary;
import com.xp.hos.pojo.PutRequest;
import com.xp.hos.serivce.IHosClient;
import com.xp.hos.util.JsonUtil;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HosClientImpl implements IHosClient {

    private static Logger logger = Logger.getLogger(HosClientImpl.class.getName());

    private String hosServer;
    private String schema;
    private String host;
    private int port = 80;
    private String token;
    private OkHttpClient client;


    public HosClientImpl(String endpoint, String token) {
        //http://127.0.0.1:9080  https://127.0.0.1:9080
        this.hosServer = endpoint;
        String[] ss = endpoint.split("://", 2);
        this.schema = ss[0];
        String[] ss1 = ss[1].split(":", 2);
        this.host = ss1[0];

        if (ss1.length == 1) {
            if (schema.equals("https")) {
                port = 443;
            } else {
                port = 80;
            }
        } else {
            this.port = Integer.parseInt(ss1[1]);
        }

        this.token = token;

        ConnectionPool pool = new ConnectionPool(10, 30, TimeUnit.SECONDS);
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(120L, TimeUnit.SECONDS)
                .writeTimeout(120L, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(pool);
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Response response = null;
                boolean success = false;
                int tryCount = 0;
                int maxLimit = 5;
                while (!success && tryCount < maxLimit) {
                    if (tryCount > 0) {
                        logger.info("intercept:" + "retry request - " + tryCount);
                    }
                    response = chain.proceed(request);
                    if (response.code() == 404) {
                        break;
                    }
                    success = response.isSuccessful();
                    tryCount++;
                    if (success) {
                        return response;
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return response;
            }
        };
        this.client = httpClientBuilder.addInterceptor(interceptor).build();
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
    }

    @Override
    public void createBucket(String bucketName, String detail) throws IOException {
        //请求头
        Headers headers = this.buildHeaders(null, token, null);
        //封装请求
        RequestBody requestBody = RequestBody.create(null, new byte[0]);
        //请求体
        Request request = new Request.Builder().headers(headers).url(
                new HttpUrl.Builder()
                        .scheme(schema)
                        .host(host)
                        .port(port)
                        .addPathSegments("/hos/v1/bucket/create")
                        .addQueryParameter("bucket", bucketName)
                        .addQueryParameter("detail", detail).build())
                        .post(requestBody).build();

        //执行请求
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()){
            //请求执行失败,抛出异常
            String msg=response.body().string();
            response.close();

            throw new IOException("create bucket fail:"+msg);
        }

    }

    @Override
    public void deleteBucket(String bucketName) throws IOException {
        //请求头
        Headers headers = this.buildHeaders(null, token, null);
        //请求体
        RequestBody requestBody = RequestBody.create(null, new byte[0]);
        //封装请求
        Request request = new Request.Builder().headers(headers).url(
                new HttpUrl.Builder()
                        .scheme(schema)
                        .host(host)
                        .port(port)
                        .addPathSegments("/hos/v1/bucket/delete")
                        .addQueryParameter("bucket", bucketName)
                        .build())
                        .delete(requestBody).build();

        //执行请求
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()){
            //请求执行失败,抛出异常
            String msg=response.body().string();
            response.close();

            throw new IOException("delete bucket fail:"+msg);
        }
    }

    @Override
    public void deleteBucket(String bucketName, String key) throws IOException {
        //请求头
        Headers headers = this.buildHeaders(null, token, null);
        //封装请求
        Request request = new Request.Builder().headers(headers).url(
                new HttpUrl.Builder()
                        .scheme(schema)
                        .host(host)
                        .port(port)
                        .addPathSegments("/hos/v1/bucket/deletev2")
                        .addQueryParameter("bucket", bucketName)
                        .addQueryParameter("key", key)
                        .build())
                        .delete()
                        .build();

        //执行请求
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()){
            //请求执行失败,抛出异常
            String msg=response.body().string();
            response.close();

            throw new IOException("delete bucket fail:"+msg);
        }
    }

    @Override
    public List<BucketModel> listBuckets() throws IOException {
        //请求头
        Headers headers = this.buildHeaders(null, token, null);
        //请求体
        Request request = new Request.Builder().headers(headers).url(
                new HttpUrl.Builder()
                        .scheme(schema)
                        .host(host)
                        .port(port)
                        .addPathSegments("/hos/v1/bucket/list")
                        .build())
                        .get().build();

        //执行请求
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()){
            //请求执行失败,抛出异常
            String msg=response.body().string();
            response.close();
            throw new IOException("list bucket fail:"+msg);
        }

        String json=response.body().string();
        List<BucketModel> bucketModels =JsonUtil.fromJsonList(BucketModel.class,json);
        response.close();

        return bucketModels;
    }

    @Override
    public HosObjectSummary getObjectSummery(String bucket, String key) throws IOException {
        //请求头
        Headers headers = this.buildHeaders(null, token, null);
        //请求体
        Request request = new Request.Builder().headers(headers).url(
                new HttpUrl.Builder()
                        .scheme(schema)
                        .host(host)
                        .port(port)
                        .addPathSegments("/hos/v1/object/info")
                        .addQueryParameter("bucket", bucket)
                        .addQueryParameter("key", key)
                        .build())
                .get().build();

        //执行请求
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()){
            //请求执行失败,抛出异常
            String msg=response.body().string();
            response.close();
            throw new IOException("getObjectSummery bucket fail:"+msg);
        }

        String json=response.body().string();
        HosObjectSummary hosObjectSummary =JsonUtil.fromJson(HosObjectSummary.class,json);
        response.close();

        return hosObjectSummary;
    }

    @Override
    public void putObject(PutRequest putRequest) {
        //判断是否为上传文件
        if (!putRequest.getKey().startsWith("/")) {
            throw new RuntimeException("object key must start with /");
        }

        RequestBody contentBody = null;
        if (putRequest.getContent() != null) {
            if (putRequest.getMediaType() == null) {
                putRequest.setMediaType("application/octet-stream");
            }
            contentBody = RequestBody
                    .create(MediaType.parse(putRequest.getMediaType()), putRequest.getContent());
        }

        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        bodyBuilder.addFormDataPart("bucket", putRequest.getBucket());
        if (putRequest.getMediaType() != null) {
            bodyBuilder.addFormDataPart("mediaType", putRequest.getMediaType());
        }
        bodyBuilder.addFormDataPart("key", putRequest.getKey());

        RequestBody requestBody = null;
        if (contentBody != null) {
            bodyBuilder.addFormDataPart("content", "content", contentBody);
        }
        requestBody = bodyBuilder.build();

        Headers headers = this.buildHeaders(putRequest.getAttr(), this.token, putRequest.getContentEncoding());
        Request.Builder reqBuilder = new Request.Builder()
                        .headers(headers)
                        .url(new HttpUrl.Builder()
                                .scheme(this.schema)
                                .host(this.host)
                                .port(this.port)
                                .addPathSegment("/hos/v1/object")
                                .build())
                        .post(requestBody);
        Request request = reqBuilder.build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                String error = "";
                if (response.body() != null) {
                    error = response.body().string();
                }
                throw new IOException("put object failed:" + error);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            response.close();
        }
    }

    @Override
    public void putObject(String bucket, String key) {
        PutRequest request=new PutRequest(bucket,key,null);
        putObject(request);
    }

    @Override
    public void putObject(String bucket, String key, byte[] content, String fileType) {
        PutRequest request=new PutRequest(bucket,key,content,fileType);
        putObject(request);
    }

    //构建sdk请求头
    private Headers buildHeaders(Map<String, String> attrs, String token, String contentEncoding) {
        Map<String, String> headerMap = new HashMap<>();
        if (contentEncoding != null) {
            headerMap.put("content-encoding", contentEncoding);
        }
        headerMap.put("X-Auth-Token", token);
        if (attrs != null && attrs.size() > 0) {
            attrs.forEach(new BiConsumer<String, String>() {
                @Override
                public void accept(String s, String s2) {
                    headerMap.put(HosHeaders.COMMON_ATTR_PREFIX + s, s2);
                }
            });
        }
        Headers headers = Headers.of(headerMap);
        return headers;
    }
}

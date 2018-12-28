package com.xp.hos.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.File;
import java.util.Map;

/**
 * 封装文件的一些属性
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class PutRequest {

    private String bucket;
    private String key;
    private File file;
    private byte[] content;
    private String contentEncoding;
    private String mediaType;
    private Map<String,String> attr;

    public PutRequest(String bucket, String key, File file) {
        this.file = file;
        this.bucket = bucket;
        this.key = key;
    }

    public PutRequest(String bucket, String key, File file, String mediaType) {
        this.file = file;
        this.bucket = bucket;
        this.mediaType = mediaType;
        this.key = key;
    }

    public PutRequest(String bucket, String key, byte[] content, String mediaType) {
        this.content = content;
        this.bucket = bucket;
        this.mediaType = mediaType;
        this.key = key;
    }
}

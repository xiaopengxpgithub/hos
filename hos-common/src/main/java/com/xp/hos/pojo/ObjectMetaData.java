package com.xp.hos.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * hos 对象的描述对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ObjectMetaData {

    private String bucket;
    private String key;
    private String fileType;
    private long length;
    private long lastModifyTime;
    private Map<String,String> attrs;

    public String getContentEncoding(){
        return attrs!=null?attrs.get("content-encoding"):null;
    }
}

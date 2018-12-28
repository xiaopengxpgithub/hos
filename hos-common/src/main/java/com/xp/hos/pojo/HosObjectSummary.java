package com.xp.hos.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;

/**
 * 将hbase查询结果转换成该对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class HosObjectSummary implements Comparable<HosObjectSummary>,Serializable {
    private String id;
    private String key;
    private String name;
    private long length;
    private String fileType;
    private long lastModifyTime;
    private String bucket;
    private Map<String,String> attrs;

    public String getContentEncoding(){
        return attrs!=null?attrs.get("content-encoding"):null;
    }

    @Override
    public int compareTo(HosObjectSummary o) {
        return this.getKey().compareTo(o.key);
    }
}

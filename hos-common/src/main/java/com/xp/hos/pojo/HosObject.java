package com.xp.hos.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import okhttp3.Response;

import java.io.InputStream;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class HosObject {
    private ObjectMetaData objectMetaData;
    private InputStream inputStream;
    private Response response;

    public HosObject(Response response){
        this.response=response;
    }

    public void close(){
        try {
            if (inputStream!=null){
                this.inputStream.close();
            }

            if (response!=null){
                this.response.close();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

package com.xp.hos.rest;


import com.google.common.base.Splitter;
import com.xp.hos.exception.ErrorCodes;
import com.xp.hos.pojo.HosObject;
import com.xp.hos.pojo.SystemRole;
import com.xp.hos.pojo.UserInfo;
import com.xp.hos.security.ContextUtil;
import com.xp.hos.service.IBucketService;
import com.xp.hos.service.IHosStore;
import com.xp.hos.utils.CoreUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
@RequestMapping(value = "/hos/v1/")
public class HosController extends BaseController {

    @Autowired
    @Qualifier("bucketServiceImpl")
    private IBucketService iBucketService;

    @Autowired
    @Qualifier("iHosStore")
    private IHosStore iHosStore;

    private static long MAX_FILE_IN_MEMORY = 2 * 1024 * 1024; //2M
    private final int readBufferSize = 32 * 1024;  //32K
    private static String TMP_DIR = System.getProperty("user.dir") + File.separator + "tmp";

    public HosController() {
        File file = new File(TMP_DIR);
        file.mkdirs();
    }

    //创建bucket
    @ResponseBody
    @RequestMapping(value = "/createBucket", method = RequestMethod.POST)
    public Object createBucket(@RequestParam(value = "bucket") String bucketName,
                               @RequestParam(value = "detail", required = false, defaultValue = "") String detail) {
        UserInfo userInfo = ContextUtil.getCurrentUser();
        if (userInfo.getSystemRole().equals(SystemRole.VISITOR)) {
            //如果当用户是游客,没有权限创建bucket
            return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "创建bucket失败!");
        } else {
            //mysql中添加bucket记录
            iBucketService.addBucket(userInfo, bucketName, detail);
            try {
                //hbase创建bucket
                iHosStore.createBucketStore(bucketName);
            } catch (Exception e) {
                //如果hbase中创建bucket失败,那么要删除mysql新添加的bucket记录
                iBucketService.deleteBucket(bucketName);

                return getError(ErrorCodes.ERROR_HBASE, "创建bucket失败");
            }

            return getResult("success");
        }
    }

    //删除bucket
    @ResponseBody
    @RequestMapping(value = "/deleteBucket", method = RequestMethod.DELETE)
    public Object deleteBucket(@RequestParam(value = "bucket") String bucket) {
        UserInfo userInfo = ContextUtil.getCurrentUser();
        if (iOperationAccessController.checkBucketOwner(userInfo.getUserName(), bucket)) {
            //如果当前bucket是当前用户创建的,那么可以删除
            try {
                //先删除hbase上的bucket
                iHosStore.deleteBucketStore(bucket);
            } catch (Exception e) {
                return getError(ErrorCodes.ERROR_HBASE, "删除bucket:" + bucket + "失败");
            }

            iBucketService.deleteBucket(bucket);

            return "success";
        } else {

            return "permission denied";
        }
    }

    //获取bucket列表
    @ResponseBody
    @RequestMapping(value = "/bucketList", method = RequestMethod.GET)
    public Object getBucketList() {
        UserInfo userInfo = ContextUtil.getCurrentUser();

        return iBucketService.getUserAuthBuckets(userInfo.getUserId());
    }

    //上传文件(/创建目录)
    @ResponseBody
    @RequestMapping(value = "/putObject", method = {RequestMethod.PUT, RequestMethod.POST})
    public Object putObjecct(@RequestParam("bucket") String bucket,
                             @RequestParam("key") String key,
                             @RequestParam(value = "mediaType", required = false) String mediaType,
                             @RequestParam(value = "content", required = false) MultipartFile file,
                             HttpServletRequest request, HttpServletResponse response) throws Exception {
        //创建目录
        UserInfo userInfo = ContextUtil.getCurrentUser();
        if (iOperationAccessController.checkPermission(userInfo.getUserId(), bucket)) {
            //如果当前用户有对bucket的操作权限
            if (key.endsWith("/")) {
                //创建目录
                iHosStore.put(bucket, key, null, 0, mediaType, null);
            }

            ByteBuffer byteBuffer = null;
            if (file != null) {
                //上传文件
                if (file.getSize() > MAX_FILE_IN_MEMORY) {
                    //如果文件大小大于设定的大小,先缓存到本地临时目录
                    File dsFile = new File(TMP_DIR + File.separator + CoreUtil.getUUIDStr());
                    file.transferTo(dsFile);
                    file.getInputStream().close();

                    //将file内容缓存到缓冲区中
                    byteBuffer = new FileInputStream(dsFile).getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.getSize());
                } else {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    IOUtils.copy(file.getInputStream(), outputStream);
                    //将file内容缓存到缓冲区中
                    byteBuffer = ByteBuffer.wrap(outputStream.toByteArray());
                    file.getInputStream().close();
                }

                //文件上传
                iHosStore.put(bucket, key, byteBuffer, file.getSize(), mediaType, null);

                return "success";
            } else {
                return getError(ErrorCodes.ERROR_HBASE, "文件上传失败!");
            }
        } else {
            return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "permission denied");
        }
    }

    //列出目录下的文件
    @ResponseBody
    @RequestMapping(value = "/object/list/dir", method = RequestMethod.GET)
    public Object listObjectDir(@RequestParam("bucket") String bucket,
                                @RequestParam("dir") String dir,
                                @RequestParam(value = "startKey", required = false, defaultValue = "") String start,
                                HttpServletResponse response) throws IOException {
        UserInfo userInfo = ContextUtil.getCurrentUser();
        if (iOperationAccessController.checkPermission(userInfo.getUserId(), bucket)) {
            if (!dir.startsWith("/") || !dir.endsWith("/")) {
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                response.getWriter().write("dir must start with / and end with /");
                return null;
            }

            if (start != null) {
                List<String> segs = StreamSupport.stream(Splitter.on("/").trimResults().omitEmptyStrings()
                        .split(start).spliterator(), false).collect(Collectors.toList());

                start = segs.get(segs.size() - 1);
            }

            iHosStore.listDir(bucket, dir, start, 100);
        }

        return null;
    }

    //删除文件
    @ResponseBody
    @RequestMapping(value = "/deleteObject", method = RequestMethod.DELETE)
    public Object deleteObject(@RequestParam(value = "bucket") String bucket,
                               @RequestParam(value = "key") String key) throws Exception {
        UserInfo userInfo = ContextUtil.getCurrentUser();
        if (iOperationAccessController.checkPermission(userInfo.getUserId(), bucket)) {
            iHosStore.deleteObject(bucket, key);
            return "success";
        } else {
            return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "permission denied");
        }
    }

    //文件下载
    @ResponseBody
    @RequestMapping(value = "/downLoadObject", method = RequestMethod.GET)
    public void downLoadObject(@RequestParam(value = "bucket") String bucket,
                               @RequestParam(value = "key") String key,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        UserInfo userInfo = ContextUtil.getCurrentUser();
        if (iOperationAccessController.checkPermission(userInfo.getUserId(), bucket)) {
            HosObject object = iHosStore.getObject(bucket, key);
            if (object == null) {
                response.setStatus(ErrorCodes.ERROR_FILE_NOT_FOUND);
                return;
            }

            String fileName = new String(object.getObjectMetaData().getKey().getBytes("iso-8859-1"), "utf-8");
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.addHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));

            OutputStream outputStream = response.getOutputStream();
            InputStream inputStream = object.getInputStream();
            try {
                byte[] buffer = new byte[readBufferSize];
                int len = -1;
                while ((len = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer,0,len);
                }
                response.flushBuffer();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                inputStream.close();
                outputStream.close();
            }
        }

    }

}

## hos对象存储服务
可以用来存储图片,文件,视频,音频等内容

### 技术架构
* 1.java后台框架:springboot+mybatis <br/>
* 2.数据库: MySQL <br/>
* 3.大数据相关技术:Hadoop+Hbase+zookeeper <br/>

### 架构图
![架构图](https://github.com/xiaopengxpgithub/hos/blob/master/imgs/%E6%9E%B6%E6%9E%84%E5%9B%BE.png)

### 流程设计
![流程](https://github.com/xiaopengxpgithub/hos/blob/master/imgs/%E6%B5%81%E7%A8%8B%E5%9B%BE1.png)
![流程](https://github.com/xiaopengxpgithub/hos/blob/master/imgs/%E6%B5%81%E7%A8%8B%E5%9B%BE2.png)

### MySQL数据库设计
![数据库](https://github.com/xiaopengxpgithub/hos/blob/master/imgs/%E6%95%B0%E6%8D%AE%E5%BA%93%E8%AE%BE%E8%AE%A1.png)

### Hbase数据库设计
#### hbase目录表
![目录表](https://github.com/xiaopengxpgithub/hos/blob/master/imgs/%E7%9B%AE%E5%BD%95%E8%A1%A8.png)<br />
`rowkey为全路径`<br />
`sub的列族内为子目录的名称`<br />
`cf的列族内为目录的详细信息`<br />

#### hbase文件表
![文件表](https://github.com/xiaopengxpgithub/hos/blob/master/imgs/%E6%96%87%E4%BB%B6%E8%A1%A8%E7%BB%93%E6%9E%84.png)
`rowkey为文件所在目录seqid+_+文件名`<br />
`c列族内为文件内容`<br />
`cf的列族内为文件的详细信息`<br />

### 功能设计
#### 文件上传
* 1.判断有无需要上传到服务器的文件夹,无则新建
* 2.新建目录时需要在目录表中插入一条记录外还需要添加到父目录的列

#### 文件下载
* 1.根据目录地址获取该目录对应的seqid,将seqid和文件名称拼接成文件表的rowkey
* 2.根据文件的rowkey快速找到这个文件,读取文件内容

#### 文件过滤
* 1.首先需要读取某个文件夹下子文件夹的数据.然后进行数据展示
* 2.rowkey支持startrow,stoprow过滤,也支持prefix过滤

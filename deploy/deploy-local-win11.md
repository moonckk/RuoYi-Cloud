### 必备微服务模块
Ruoyi-cloud微服务项目包含7个微服务,其中系统运行必备服务是:
RuoYiAuthApplication
RuoYiGatewayApplication
RuoYiSystemApplication

### 安装mysql(必须)
使用phpstudy软件安装mysql8.x
root
密码password

### 安装Redis(必须)
使用phpstudy软件安装redis
本地密码不用设置,如果加了redis密码,那么需要在nacos中修改redis的密码

### 安装nacos(必须)
下载nacos.zip包解压,需要先导入项目中和nacos相关的sql,再修改配置文件application.properties中的datasource配置,
配置JAVA_HOME环境变量(如果不行,直接修改启动脚本中的%JAVA_HOME%)  然后在IDEA中设置启动脚本,

### 安装sentinel(可选)
下载sentinel压缩包,启动脚本需要自己写,修改默认的8080端口为8718
```bash
java -Dproject.name=sentinel -Dserver.port=8718 -jar sentinel-dashboard-1.8.6.jar
```

### 前端页面
ruoyi-ui是前端工程,参考readme.md启动

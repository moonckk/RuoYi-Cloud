部署到vagrant虚拟机的centos7服务器上,基于docker-compose
4C6G配置

### 拉取项目
git clone 项目地址
git pull

### 切换分支
部署哪个分支就切到哪个分支
git checkout dev

### 打包
cd Ruoyi-cloud
call mvn clean package -Dmaven.test.skip=true
等待打包完成

### jar包复制
指定copy.sh脚本

### sql文件复制
把sql/*.sql 复制到  ../docker/mysql/db下

### 创建运行容器
docker-compose up -d

### 启动前端项目
参照readme.md
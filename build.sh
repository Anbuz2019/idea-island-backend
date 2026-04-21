
# 构建镜像
docker-compose build app
# 启动容器
docker-compose up -d app

# 执行SQL脚本
# docker exec -i mysql容器名 mysql -uroot -p'你的密码' idea_island < tables_xxl_job.sql
mysql -h 127.0.0.1 -P 3306 -uroot -p idea_island < tables_xxl_job.sql





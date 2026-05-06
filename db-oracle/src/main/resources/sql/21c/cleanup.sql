-- Oracle 21c 不支持 IF EXISTS，依赖 SqlRunner stopOnError=false 逐条尽力删除
DROP TABLE test_orders PURGE;
DROP TABLE test_users PURGE;

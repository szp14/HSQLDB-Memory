Testdb.java中共用到5个项，分别为：

dss.ddl		用于建表关系与主键外键
_insertSQL.txt	原始数据，1.5G，没上传到git
test文件夹	查询操作，其中test.sql为22个SELECT语句，testX.sql为第X个SELECT语句
		（因为有的测试要一次运行22个，有的要一个个运行）
_updateSQL.txt	RF1操作，插入新数据
_deleteSQL.txt	RF2操作，删除旧数据
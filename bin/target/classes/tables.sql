-- 记录数据传输的碎片文件信息，用于断点续传
CREATE TABLE trans_temp_file (
	id VARCHAR(64) PRIMARY KEY,
	bus_type varchar(100),-- 业务类型，用于记录这次数据传输是哪个业务，随意定义
	path VARCHAR(100),-- 临时碎片文件存放绝对路径
	file_name VARCHAR(100),-- 拆分前的文件名
	pice_file_name VARCHAR(100),-- 临时碎片文件名
	point BIGINT-- 文件写入起始偏移量
);

-- 有可被拉取数据的标识
CREATE TABLE trans_pull_data_flag(
	app_uri VARCHAR(50),-- 应用唯一标识
	bus_type VARCHAR(100),-- 业务类型
	rows_json_str text,-- 待拉取的数据
	PRIMARY KEY(app_uri,bus_type)
);
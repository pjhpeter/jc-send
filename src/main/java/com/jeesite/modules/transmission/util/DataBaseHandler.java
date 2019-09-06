package com.jeesite.modules.transmission.util;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jeesite.common.collect.ListUtils;
import com.jeesite.common.config.Global;
import com.jeesite.common.datasource.RoutingDataSource;
import com.jeesite.common.io.FileUtils;
import com.jeesite.common.lang.DateUtils;
import com.jeesite.modules.file.entity.FileEntity;
import com.jeesite.modules.file.entity.FileUpload;
import com.jeesite.modules.file.service.FileEntityService;
import com.jeesite.modules.file.service.FileUploadService;

/**
 * 数据库操作处理类
 * 
 * @author 彭嘉辉
 *
 */
@Component
public class DataBaseHandler {
	@Autowired
	private RoutingDataSource routingDataSource;
	@Autowired
	private FileUploadService fileUploadService;
	@Autowired
	private FileEntityService fileEntityService;

	private Connection connection;
	private Statement statement;

	/*
	 * 初始化
	 */
	private void init() throws SQLException {
		DataSource dataSource = routingDataSource.getTargetDataSource("default");
		this.connection = dataSource.getConnection();
		this.connection.setAutoCommit(false);
		this.statement = this.connection.createStatement();
	}

	/*
	 * 判断是否新的记录
	 * 
	 * @param tableName 数据库表名
	 * 
	 * @param idMap 主键map，用map是因为可能是联合主键
	 */
	private boolean isNewRecord(String tableName, Map<String, Object> idMap) {
		ResultSet resultSet = null;
		try {
			List<String> queryCondition = ListUtils.newArrayList();
			idMap.keySet().forEach(key -> {
				queryCondition.add(key + " = '" + idMap.get(key) + "'");
			});
			String querySQL = "SELECT COUNT(0) FROM " + tableName + " WHERE " + StringUtils.join(queryCondition.toArray(), " AND ");
			System.out.println("查询语句：" + querySQL);
			resultSet = this.statement.executeQuery(querySQL);
			if (resultSet.next() && resultSet.getInt(1) > 0) {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	/**
	 * 处理数据表数据
	 * 
	 * @param tableName
	 *            表名
	 * @param rows
	 *            所有行数据
	 * @param unZipDir
	 *            解压后的目录
	 * @throws SQLException
	 */
	public void setData(String tableName, JSONArray rows, String unZipDir) throws Exception {
		if (this.connection == null) {
			init();
		}
		for (Object object : rows) {

			JSONObject row = JSON.parseObject(object.toString());
			// 主键
			JSONObject id = row.getJSONObject("id");
			try {
				if (isNewRecord(tableName, id.getInnerMap())) {
					// 插入数据
					System.out.println("新增");
					this.statement.executeUpdate(insertSQLBuilder(tableName, row));
				} else {
					// 更新数据
					System.out.println("更新");
					this.statement.executeUpdate(updateSQLBuilder(tableName, row));
				}

				// 处理附件
				JSONArray fileListJson = row.getJSONArray("fileList");
				if (fileListJson != null) {
					fileListJson.forEach(obj -> {
						JSONObject fileJson = JSON.parseObject(obj.toString());
						FileUpload fileUpload = JSON.toJavaObject(fileJson, FileUpload.class);
						FileEntity fileEntity = fileUpload.getFileEntity();

						// 处理系统附件表
						try {// 新增
							fileUpload.setIsNewRecord(true);
							fileEntity.setIsNewRecord(true);
							fileUploadService.save(fileUpload);
							fileEntityService.save(fileEntity);
						} catch (Exception e) {// 更新
							fileUpload.setIsNewRecord(false);
							fileEntity.setIsNewRecord(false);
							fileUploadService.save(fileUpload);
							fileEntityService.save(fileEntity);
						}

						// 处理文件
						String fileId = fileEntity.getFileId();
						String filePath = fileEntity.getFilePath();
						String fileExtension = fileEntity.getFileExtension();
						String attachmentName = fileId + "." + fileExtension;
						String fileUploadPath = Global.getUserfilesBaseDir("fileupload");
						FileUtils.copyFileCover(unZipDir + File.separator + attachmentName, fileUploadPath + File.separator + filePath + File.separator + attachmentName, true);
					});
				}

			} catch (Exception e) {
				try {
					connection.rollback();
				} catch (Exception e1) {
					e1.printStackTrace();
					throw new Exception(e.getMessage());
				}
				throw new Exception(e.getMessage());
			}

		}
	}

	/**
	 * 提交数据
	 */
	public void commit() {
		try {
			if (this.connection != null) {
				this.connection.commit();
			}
		} catch (SQLException e) {
			try {
				this.connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			try {
				if (this.statement != null) {
					this.statement.close();
					this.statement = null;
				}
				if (this.connection != null) {
					this.connection.close();
					this.connection = null;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * 生成insert语句
	 */
	private String insertSQLBuilder(String tableName, JSONObject row) {
		String insertSQL = "INSERT INTO " + tableName;
		List<String> columnList = ListUtils.newArrayList();
		List<String> valueList = ListUtils.newArrayList();
		// 解析各列的值
		JSONArray rowData = row.getJSONArray("rowData");
		rowData.forEach(rowDataObj -> {
			JSONObject column = JSON.parseObject(rowDataObj.toString());
			columnList.add(column.getString("to"));
			if (column.getString("type").equals("number")) {// 数字
				valueList.add(column.get("value").toString());
			} else if (column.getString("type").equals("date")) {// 日期
				valueList.add("'" + DateUtils.formatDate(column.getLong("value"), "yyyy-MM-dd HH:mm:ss") + "'");
			} else {// 布尔值和字符串
				valueList.add("'" + column.get("value") + "'");
			}
		});
		insertSQL = insertSQL + " (" + StringUtils.join(columnList.toArray(), ",") + ") VALUES (" + StringUtils.join(valueList.toArray(), ",") + ")";
		System.out.println("插入语句：" + insertSQL);
		return insertSQL;
	}

	/*
	 * 生成update语句
	 */
	private String updateSQLBuilder(String tableName, JSONObject row) {
		String updateSQL = "UPDATE " + tableName + " SET";
		List<String> valueList = ListUtils.newArrayList();
		List<String> idList = ListUtils.newArrayList();
		// 解析各列的值
		JSONArray rowData = row.getJSONArray("rowData");
		rowData.forEach(rowDataObj -> {
			JSONObject column = JSON.parseObject(rowDataObj.toString());
			if (column.get("isPK") != null && column.getBoolean("isPK")) {
				idList.add(column.getString("to") + " = '" + column.get("value") + "'");
			} else if (column.getString("type").equals("number")) {// 数字
				valueList.add(column.getString("to") + " = " + column.get("value").toString());
			} else if (column.getString("type").equals("date")) {// 日期
				valueList.add(column.getString("to") + " = '" + DateUtils.formatDate(column.getLong("value"), "yyyy-MM-dd HH:mm:ss") + "'");
			} else {// 布尔值和字符串
				valueList.add(column.getString("to") + " = '" + column.get("value") + "'");
			}
		});
		updateSQL = updateSQL + " " + StringUtils.join(valueList.toArray(), ",") + " WHERE " + StringUtils.join(idList.toArray(), " AND ");
		System.out.println("更新语句：" + updateSQL);
		return updateSQL;
	}

}

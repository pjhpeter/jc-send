package com.jeesite.modules.transmission.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jeesite.common.collect.ListUtils;
import com.jeesite.common.datasource.RoutingDataSource;
import com.jeesite.common.idgen.IdGen;
import com.jeesite.common.service.CrudService;
import com.jeesite.modules.transmission.dao.TempFileDao;
import com.jeesite.modules.transmission.entity.TempFile;

/**
 * 临时碎片文件信息service层接口，用于断点续传
 * 
 * @author 彭嘉辉
 *
 */
@Service
public class TempFileService extends CrudService<TempFileDao, TempFile> {
	@Autowired
	private RoutingDataSource routingDataSource;

	private DataSource dataSource;
	private Connection connection;
	private Statement statement;

	/**
	 * 插入碎片临时文件信息，直接用框架的api要等到所有操作完成才提交数据，影响后面操作，所以只能自己写jdbc
	 * 
	 * @param tempFileList 碎片临时文件对象列表
	 * @return 加上id的碎片临时文件对象列表
	 */
	public List<TempFile> insertBatch(List<TempFile> tempFileList) {
		List<TempFile> returnList = ListUtils.newArrayList();
		System.out.println("初始化获取数据库连接");
		this.dataSource = routingDataSource.getDefaultDataSource();
		try {
			connection = this.dataSource.getConnection();
			connection.setAutoCommit(true);
			statement = connection.createStatement();
			for (TempFile tempFile : tempFileList) {
				String id = IdGen.nextId();
				String insertSql = "INSERT INTO trans_temp_file (id,bus_type,path,file_name,pice_file_name,point) VALUES ('"
						+ id + "','" + tempFile.getBusType() + "','" + tempFile.getPath() + "','"
						+ tempFile.getFileName() + "','" + tempFile.getPiceFileName() + "'," + tempFile.getPoint()
						+ ")";
				this.statement.addBatch(insertSql);
				tempFile.setId(id);
				returnList.add(tempFile);
			}
			this.statement.executeBatch();
		} catch (SQLException e) {
			try {
				this.connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			try {
				statement.close();
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return returnList;
	}

}

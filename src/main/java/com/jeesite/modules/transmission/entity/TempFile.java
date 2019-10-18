package com.jeesite.modules.transmission.entity;

import com.jeesite.common.entity.DataEntity;
import com.jeesite.common.mybatis.annotation.Column;
import com.jeesite.common.mybatis.annotation.Table;

/**
 * 临时文件记录实体
 * 
 * @author 彭嘉辉
 *
 */
@Table(name = "trans_temp_file", columns = { @Column(name = "id", attrName = "id", label = "id", isPK = true),
		@Column(name = "bus_type", attrName = "busType", label = "业务类型，用于记录这次数据传输是哪个业务，随意定义"),
		@Column(name = "path", attrName = "path", label = "临时碎片文件存放目录的绝对路径"),
		@Column(name = "file_name", attrName = "fileName", label = "拆分前的文件名"),
		@Column(name = "pice_file_name", attrName = "piceFileName", label = "临时碎片文件名"),
		@Column(name = "point", attrName = "point", label = "文件写入起始偏移量") })
public class TempFile extends DataEntity<TempFile> {

	private static final long serialVersionUID = 2656195373716391093L;

	private String busType;
	private String path;
	private String fileName;
	private String piceFileName;
	private Long point;

	public String getBusType() {
		return busType;
	}

	public void setBusType(String busType) {
		this.busType = busType;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getPiceFileName() {
		return piceFileName;
	}

	public void setPiceFileName(String piceFileName) {
		this.piceFileName = piceFileName;
	}

	public Long getPoint() {
		return point;
	}

	public void setPoint(Long point) {
		this.point = point;
	}

}

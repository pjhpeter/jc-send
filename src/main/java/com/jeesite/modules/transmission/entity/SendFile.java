package com.jeesite.modules.transmission.entity;

import java.io.Serializable;

/**
 * 需要传输的附件信息实体
 * 
 * @author 彭嘉辉
 *
 */
public class SendFile implements Serializable {

	private static final long serialVersionUID = 7043493306335934567L;

	/*
	 * 文件保存的相对路径（fileupload后的部分）
	 */
	private String path;
	/*
	 * 文件名
	 */
	private String fileName;

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

}

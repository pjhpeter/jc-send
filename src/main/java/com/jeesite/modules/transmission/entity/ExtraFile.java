package com.jeesite.modules.transmission.entity;

import java.io.Serializable;

/**
 * 额外要传输的文件类
 * 
 * @author 彭嘉辉
 *
 */
public class ExtraFile implements Serializable {

	private static final long serialVersionUID = 77467215153684549L;

	private String path;
	private String fileName;

	/**
	 * 构造额外传输的文件对象
	 * 
	 * @param path
	 *            文件存放的目录，注意是userfiles之后的相对目录，比如存放在userfiles/temp下，就写temp
	 * @param fileName
	 *            文件名
	 */
	public ExtraFile(String path, String fileName) {
		this.path = path;
		this.fileName = fileName;
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

}

package com.jeesite.modules.transmission.entity;

import com.jeesite.common.entity.DataEntity;
import com.jeesite.common.mybatis.annotation.Column;
import com.jeesite.common.mybatis.annotation.Table;

/**
 * 有可拉取数据的标识
 * 
 * @author 彭嘉辉
 *
 */
@Table(name = "trans_pull_data_flag", columns = { @Column(name = "id", attrName = "id", label = "主键", isPK = true), @Column(name = "app_uri", attrName = "appUri", label = "应用唯一标识"),
		@Column(name = "bus_type", attrName = "busType", label = "业务类型"), @Column(name = "rows_json_str", attrName = "rowsJsonStr", label = "待拉取的数据") })
public class PullDataFlag extends DataEntity<PullDataFlag> {

	private static final long serialVersionUID = 2763657670464225686L;

	private String appUri;
	private String busType;
	private String rowsJsonStr;

	/**
	 * 构造有可拉取数据的标识对象
	 * @param id TODO
	 * @param appUri
	 *            应用唯一标识
	 * @param busType
	 *            业务类型
	 * @param rowsJsonStr
	 *            待拉取的数据
	 */
	public PullDataFlag(String id, String appUri, String busType, String rowsJsonStr) {
		this.appUri = appUri;
		this.busType = busType;
		this.rowsJsonStr = rowsJsonStr;
	}

	public String getAppUri() {
		return appUri;
	}

	public void setAppUri(String appUri) {
		this.appUri = appUri;
	}

	public String getBusType() {
		return busType;
	}

	public void setBusType(String busType) {
		this.busType = busType;
	}

	public String getRowsJsonStr() {
		return rowsJsonStr;
	}

	public void setRowsJsonStr(String rowsJsonStr) {
		this.rowsJsonStr = rowsJsonStr;
	}

}

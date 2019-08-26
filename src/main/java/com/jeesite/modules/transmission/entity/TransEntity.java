package com.jeesite.modules.transmission.entity;

import java.io.Serializable;
import java.util.List;

import com.jeesite.common.entity.DataEntity;
import com.jeesite.modules.transmission.util.Constant;

/**
 * 传输接口参数实体 参数说明： 
 * 
 * list 需要传输的对象集合，传输多个对象时使用（不能与entity并用）
 * 
 * entity 需要传输的单个对象，传输单个对象时使用（不能与list并用）
 * 
 * entityType 需要传输的对象的实体类型
 * 
 * url 传输接收方的地址，如：192.168.1.1:8080/temp busType 业务类型，该传输业务的唯一标识，自己定义 renewal
 * 断点续传的标识
 * 
 * renewal 是否断点续传，默认false
 * 
 * requireSysColumn 是否需要传输系统的5个字段（status,create_date.....），默认false
 * 
 * requireSysColumnArr 如果系统的5个字段只传输其中一部分的话，在这里设置，如只用了create_date和update_date，{create_date,update_date}
 * 
 * extraFileList 额外要传输的文件列表，有需要额外传输的文件，这些文件不存在于附件中，比如跳过系统上传组件自动生成的文件，需要传如此参数
 * 
 * extraFile 单个额外要传输的文件，有需要额外传输的文件，这些文件不存在于附件中，比如跳过系统上传组件自动生成的文件，需要传如此参数
 * 
 * triggerName 触发器注入名称，一般为类名首字母小写后的字符串，用于数据传输完成后，在接收端需要执行的一些业务逻辑，触发器类需要在接收端写好，实现ReceiveTrigger接口
 * 
 * @author 彭嘉辉
 *
 */
public class TransEntity<T extends DataEntity<?>> implements Serializable {

	private static final long serialVersionUID = 3455698535999176893L;

	private List<T> list;
	private T entity;
	private Class<T> entityType;
	private String url;
	private String busType;
	private boolean renewal = false;
	private boolean requireSysColumn = false;
	private String[] requireSysColumnArr;
	private List<ExtraFile> extraFileList;
	private ExtraFile extraFile;
	private String triggerName = Constant.HAS_NO_TRIGGER;

	public List<T> getList() {
		return list;
	}

	public void setList(List<T> list) {
		this.list = list;
	}

	public T getEntity() {
		return entity;
	}

	public void setEntity(T entity) {
		this.entity = entity;
	}

	public Class<T> getEntityType() {
		return entityType;
	}

	public void setEntityType(Class<T> entityType) {
		this.entityType = entityType;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getBusType() {
		return busType;
	}

	public void setBusType(String busType) {
		this.busType = busType;
	}

	public boolean isRenewal() {
		return renewal;
	}

	public void setRenewal(boolean renewal) {
		this.renewal = renewal;
	}

	public boolean isRequireSysColumn() {
		return requireSysColumn;
	}

	public void setRequireSysColumn(boolean requireSysColumn) {
		this.requireSysColumn = requireSysColumn;
	}

	public String[] getRequireSysColumnArr() {
		return requireSysColumnArr;
	}

	public void setRequireSysColumnArr(String[] requireSysColumnArr) {
		this.requireSysColumnArr = requireSysColumnArr;
	}

	public List<ExtraFile> getExtraFileList() {
		return extraFileList;
	}

	public void setExtraFileList(List<ExtraFile> extraFileList) {
		this.extraFileList = extraFileList;
	}

	public ExtraFile getExtraFile() {
		return extraFile;
	}

	public void setExtraFile(ExtraFile extraFile) {
		this.extraFile = extraFile;
	}

	public String getTriggerName() {
		return triggerName;
	}

	public void setTriggerName(String triggerName) {
		this.triggerName = triggerName;
	}

}

package com.jeesite.modules.transmission.entity;

import java.io.Serializable;

import com.jeesite.common.mapper.JsonMapper;

/**
 * 响应结果
 * 
 * @author 彭嘉辉
 *
 */
public class Result implements Serializable {

	private static final long serialVersionUID = -4154323070846171144L;

	private boolean success;
	private String msg;
	private String data;

	/**
	 * 构造函数
	 * 
	 * @param success 是否成功
	 * @param msg     响应信息
	 * @param data    返回的数据
	 */
	public Result(boolean success, String msg, String data) {
		this.success = success;
		this.msg = msg;
		this.data = data;
	}

	/**
	 * 构造函数
	 * 
	 * @param success 是否成功
	 * @param msg     响应信息
	 */
	public Result(boolean success, String msg) {
		this.success = success;
		this.msg = msg;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return JsonMapper.toJson(this);
	}
}

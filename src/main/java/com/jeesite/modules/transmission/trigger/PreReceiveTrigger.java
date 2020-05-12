package com.jeesite.modules.transmission.trigger;

import com.alibaba.fastjson.JSONArray;

/**
 * 接收传输过了的数据前调用的预触发器接口
 * 
 * @author 彭嘉辉
 *
 */
public interface PreReceiveTrigger {
	void run(JSONArray rows, String busType);
}

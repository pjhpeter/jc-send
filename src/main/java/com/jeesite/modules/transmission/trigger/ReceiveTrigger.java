package com.jeesite.modules.transmission.trigger;

import com.alibaba.fastjson.JSONArray;

/**
 * 接收数据完成后触发的触发器，接收数据结束后如果想做一些操作请写一个触发器类，实现此接口，发送数据时把触发器的注入名传过来就行
 * 
 * @author 彭嘉辉
 *
 */
public interface ReceiveTrigger {
	void run(JSONArray rows, String busType);
}

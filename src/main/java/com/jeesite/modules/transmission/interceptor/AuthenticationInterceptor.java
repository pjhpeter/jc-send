package com.jeesite.modules.transmission.interceptor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.jeesite.common.codec.AesUtils;
import com.jeesite.modules.transmission.util.Constant;

/**
 * 安全认证拦截器
 * 
 * @author 彭嘉辉
 *
 */
public class AuthenticationInterceptor implements HandlerInterceptor {

	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		@SuppressWarnings("unchecked")
		Map<String, String> pathVariables = (Map<String, String>) request
				.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		String token = pathVariables.get("token");
		if (identify(token)) {
			return true;
		}
		response.setStatus(Constant.ResponseCode.未授权);
		return false;
	}

	/*
	 * 验证是否合法应用发出的请求
	 */
	private boolean identify(String token) {
		String s = AesUtils.decode(token, Constant.TOKEN_KEY);
		if (Long.valueOf(s.substring(s.lastIndexOf("_") + 1)) + 10000 < System.currentTimeMillis()) {
			return false;
		}
		return s.substring(0, s.lastIndexOf("_")).equals(Constant.TOKEN);
	}

}

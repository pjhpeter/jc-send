package com.jeesite.modules.transmission.interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebAppConfigurer implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// 可添加多个，这里选择拦截所有请求地址，进入后判断是否有加注解即可
		registry.addInterceptor(new AuthenticationInterceptor()).addPathPatterns("/trans/**");
	}
}

package com.jeesite.modules.transmission.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 需要报送的实体类添可加此注解，如果不添加，表名则读取Table注解的name值
 * 
 * @author 彭嘉辉
 *
 */
@Documented
@Target(value = { ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SendTable {
	/**
	 * 接收方对应数据库表名
	 */
	String to();
}

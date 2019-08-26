package com.jeesite.modules.transmission.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 报送字段设置注解，成员变量设置此注解，说明该变量需要被报送
 * 
 * @author 彭嘉辉
 *
 */
@Documented
@Target(value = { ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SendField {
	/**
	 * 接收方对应数据库列名
	 */
	String to() default "";

	/**
	 * 是否主键
	 */
	boolean isPK() default false;
}

package com.jeesite.modules.transmission.util;

/**
 * 常量
 * 
 * @author 彭嘉辉
 *
 */
public class Constant {

	/**
	 * 文件解密秘钥
	 */
	public static final String FILE_KEY = "80548e7c0c539985af86807ef5a32f1e";

	/**
	 * 令牌解密秘钥
	 */
	public static final String TOKEN_KEY = "fa4062399b4ed7350ed1a3ef590f5556";

	/**
	 * 令牌字符串
	 */
	public static final String TOKEN = "tq26556570";

	/**
	 * 没有触发器
	 */
	public static final String HAS_NO_TRIGGER = "has_no_trigger";

	/**
	 * 响应信息
	 */
	public class Message {
		public static final String 操作成功 = "操作成功";
		public static final String 操作失败 = "操作失败";
		public static final String 未授权 = "应用未授权";
		public static final String 传输成功 = "传输数据成功";
		public static final String 传输失败 = "传输数据失败";
		public static final String 无可传输文件 = "没有可传输的文件";
		public static final String 解析成功 = "解析数据成功";
		public static final String 解析失败 = "解析数据失败";
		public static final String 拉取成功 = "拉取数据成功";
		public static final String 拉取失败 = "拉取数据失败";
		public static final String 无可拉取数据 = "没有可以拉取的数据";
		public static final String 推送成功 = "推送数据成功";
		public static final String 推送失败 = "推送数据失败";
		public static final String 服务没响应 = "服务没响应，可能网络不通";
		public static final String 导出成功 = "导出数据成功";
		public static final String 导出失败 = "导出数据失败";
		public static final String 导入成功 = "导入数据成功";
		public static final String 导入失败 = "导入数据失败";
	}

	/**
	 * 响应码
	 */
	public class ResponseCode {
		public static final int 未授权 = 401;
	}

	/**
	 * 临时目录
	 */
	public class TemplDir {
		/**
		 * 发送时生成的临时文件目录
		 */
		public static final String SEND_TEMP = "temp";
		/**
		 * 接收时生成的临时文件目录
		 */
		public static final String RECEIVE_TEMP = "temp_r_";
		/**
		 * 等待拉取的临时文件目录
		 */
		public static final String WEIT_FOR_PULL_TEMP = "pull_file";
		/**
		 * 拉取过来的临时文件目录
		 */
		public static final String PULL_TEMP = "pull_file_r";

		/**
		 * 额外传输文件的目录
		 */
		public static final String EXTRA_FILE_TEMP = "extra_file";

		/**
		 * 导出离线传输数据的临时文件目录
		 */
		public static final String EXPORT_TEMP = "export_temp";

		/**
		 * 导入离线传输数据的临时文件目录
		 */
		public static final String IMPORT_TEMP = "import_temp";
	}

	/**
	 * 参数设置项
	 */
	public class SysConfig {
		/**
		 * 应用唯一标识
		 */
		public static final String APP_URI = "appUri";
		/**
		 * 接收端地址，如192.168.1.1:8080/temp
		 */
		public static final String SEND_URL = "send.url";
	}
}

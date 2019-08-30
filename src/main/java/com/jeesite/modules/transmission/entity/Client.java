package com.jeesite.modules.transmission.entity;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.alibaba.fastjson.JSON;
import com.jeesite.common.codec.AesUtils;
import com.jeesite.common.config.Global;
import com.jeesite.common.io.FileUtils;
import com.jeesite.modules.transmission.util.Constant;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import reactor.core.publisher.Mono;

/**
 * web请求配置类
 * 
 * @author 彭嘉辉
 *
 */
public class Client implements Serializable {

	private static final long serialVersionUID = -1828859714063726630L;

	/*
	 * 请求ip
	 */
	private String url;
	/*
	 * 应用唯一标识
	 */
	private String appUri;

	/**
	 * 默认读取系统参数send.ip和send.port
	 */
	public Client() {
		this.url = Global.getConfig(Constant.SysConfig.SEND_URL);
		this.appUri = Global.getConfig(Constant.SysConfig.APP_URI);
	}

	public Client(String url) {
		this.url = url;
		this.appUri = Global.getConfig(Constant.SysConfig.APP_URI);
	}

	/**
	 * 发送文件
	 * 
	 * @param piceFilePath
	 *            碎片文件全路径
	 * @param point
	 *            开始写入文件的偏移量
	 * @param busType
	 *            业务类型
	 * @return 响应结果
	 */
	public Result send(String piceFilePath, long point, String busType) {
		WebClient webClient = WebClient.create("http://" + this.url);
		System.out.println("向http://" + this.url + "发送请求");
		HttpHeaders headers = new HttpHeaders();
		// 发送文件一定要设置这个包头
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		HttpEntity<FileSystemResource> entity = new HttpEntity<>(new FileSystemResource(piceFilePath), headers);
		MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
		data.add("file", entity);
		data.add("point", point);
		data.add("busType", busType);
		Mono<String> bodyToMono = webClient.post().uri("/trans/receive/{token}/{appUri}", AesUtils.encode(Constant.TOKEN + "_" + System.currentTimeMillis(), Constant.TOKEN_KEY), this.appUri)
				.body(BodyInserters.fromMultipartData(data)).retrieve().bodyToMono(String.class);
		return JSON.toJavaObject(JSON.parseObject(bodyToMono.block()), Result.class);
	}

	/**
	 * 解析数据
	 * 
	 * @param busType
	 *            业务类型
	 * @param triggerName
	 *            接收端解析数据成功后需要执行的触发器名称
	 * @return 响应结果
	 */
	public Result analysis(String busType, String triggerName) {
		WebClient webClient = WebClient.create("http://" + this.url);
		System.out.println("向http://" + this.url + "发送请求");
		Mono<String> bodyToMono = webClient.post().uri("/trans/analysis/{busType}/{token}/{appUri}/{triggerName}", busType,
				AesUtils.encode(Constant.TOKEN + "_" + System.currentTimeMillis(), Constant.TOKEN_KEY), this.appUri, triggerName).retrieve().bodyToMono(String.class);
		return JSON.toJavaObject(JSON.parseObject(bodyToMono.block()), Result.class);
	}

	/**
	 * 针对批量传输的数据解析
	 * 
	 * @param transFlag
	 *            这一组传输的标识字符串
	 * @param triggerName
	 *            接收端解析数据成功后需要执行的触发器名称
	 * @return 响应结果
	 */
	public Result analysisMulti(String transFlag, String triggerName) {
		if (StringUtils.isBlank(triggerName)) {
			triggerName = Constant.HAS_NO_TRIGGER;
		}
		WebClient webClient = WebClient.create("http://" + this.url);
		System.out.println("向http://" + this.url + "发送请求");
		Mono<String> bodyToMono = webClient.post().uri("/trans/analysis_multi/{transFlag}/{token}/{appUri}/{triggerName}", transFlag,
				AesUtils.encode(Constant.TOKEN + "_" + System.currentTimeMillis(), Constant.TOKEN_KEY), this.appUri, triggerName).retrieve().bodyToMono(String.class);
		return JSON.toJavaObject(JSON.parseObject(bodyToMono.block()), Result.class);
	}

	/**
	 * 清除临时文件
	 * 
	 * @param busType
	 *            业务类型
	 * @return 响应结果
	 */
	public Result cleanTempFile(String busType) {
		WebClient webClient = WebClient.create("http://" + this.url);
		System.out.println("向http://" + this.url + "发送请求");
		Mono<String> bodyToMono = webClient.post()
				.uri("/trans/clean/{busType}/{token}/{appUri}", busType, AesUtils.encode(Constant.TOKEN + "_" + System.currentTimeMillis(), Constant.TOKEN_KEY), this.appUri).retrieve()
				.bodyToMono(String.class);
		return JSON.toJavaObject(JSON.parseObject(bodyToMono.block()), Result.class);
	}

	/**
	 * 检测是否有可拉取的数据
	 * 
	 * @param busType
	 *            业务类型
	 * @return 响应结果
	 */
	public Result hasPullData(String busType) {
		WebClient webClient = WebClient.create("http://" + this.url);
		System.out.println("向http://" + this.url + "发送请求");
		Mono<String> bodyToMono = webClient.get()
				.uri("/trans/has_pull_data/{busType}/{token}/{appUri}", busType, AesUtils.encode(Constant.TOKEN + "_" + System.currentTimeMillis(), Constant.TOKEN_KEY), this.appUri).retrieve()
				.bodyToMono(String.class);
		return JSON.toJavaObject(JSON.parseObject(bodyToMono.block()), Result.class);
	}

	/**
	 * 拉取数据
	 * 
	 * @param busType
	 *            业务类型
	 * @return 响应结果
	 */
	public Result pull(String busType) {
		// 拉取文件
		// WebClient文件下载估计是我不会写，一直报错，用okhttp就可以了，哈哈！不过WebClient的rest写法比较好看，其他请求还是用Webclient吧
		String url = "http://" + this.url + "/trans/pull/" + busType + "/" + AesUtils.encode(Constant.TOKEN + "_" + System.currentTimeMillis(), Constant.TOKEN_KEY) + "/" + this.appUri;
		System.out.println("向http://" + this.url + "发送请求");
		OkHttpClient okHttpClient = new OkHttpClient();
		final Request request = new Request.Builder().url(url).build();
		final Call call = okHttpClient.newCall(request);
		try {
			Response response = call.execute();
			if (!response.isSuccessful()) {
				return new Result(false, "响应码：" + response.code(), null);
			}
			// 写文件
			String pullFileDir = Global.getUserfilesBaseDir(Constant.TemplDir.PULL_TEMP + "_" + busType);
			FileUtils.createDirectory(pullFileDir);
			File out = new File(pullFileDir + File.separator + busType + ".zip");
			FileUtils.copyInputStreamToFile(response.body().byteStream(), out);
			System.out.println("拉取文件成功");
			return new Result(true, "拉取成功，准备解析", null);
		} catch (IOException e) {
			e.printStackTrace();
			return new Result(false, Constant.Message.拉取失败, null);
		}
	}

	/**
	 * 清空推送的临时文件
	 * 
	 * @param busType
	 *            业务类型
	 * @param triggerName
	 *            拉取数据成功后要执行的触发器注入名称
	 * @return 响应结果
	 */
	public Result cleanPushTempFile(String busType, String triggerName) {
		// 拉取成功，删除远端临时文件
		WebClient webClient = WebClient.create("http://" + this.url);
		System.out.println("向http://" + this.url + "发送请求");
		Mono<String> bodyToMono = webClient.post().uri("/trans/pull_success/{busType}/{token}/{appUri}/{triggerName}", busType,
				AesUtils.encode(Constant.TOKEN + "_" + System.currentTimeMillis(), Constant.TOKEN_KEY), this.appUri, triggerName).retrieve().bodyToMono(String.class);
		return JSON.toJavaObject(JSON.parseObject(bodyToMono.block()), Result.class);
	}

}

package com.jeesite.modules.transmission.web;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jeesite.modules.transmission.entity.Result;
import com.jeesite.modules.transmission.service.impl.TransmissionServiceImpl;

/**
 * 数据传输控制器
 * 
 * @author 彭嘉辉
 *
 */
@RestController
@RequestMapping(value = "/trans")
public class TransmissionController {
	@Resource
	private TransmissionServiceImpl transmissionService;

	/**
	 * 接收传输的文件
	 * 
	 * @param file    文件对象
	 * @param point   开始写入的位置
	 * @param busType 业务类型
	 * @return 响应结果
	 * @throws Exception
	 */
	@PostMapping("receive/{token}/{appUri}")
	public Result receive(MultipartFile file, long point, String busType, @PathVariable("appUri") String appUri) throws Exception {
		return this.transmissionService.serverReceive(file, point, busType, appUri);
	}

	/**
	 * 解析传输过来的数据
	 * 
	 * @param busType     业务类型
	 * @param appUri      应用唯一标识
	 * @param triggerName 解析数据成功后需要执行的触发器名称
	 * @return 响应结果
	 */
	@PostMapping("analysis/{busType}/{token}/{appUri}/{triggerName}/{preTriggerName}")
	public Result analysis(@PathVariable("busType") String busType, @PathVariable("appUri") String appUri, @PathVariable("triggerName") String triggerName, @PathVariable("preTriggerName") String preTriggerName) {
		return this.transmissionService.serverAnalysis(appUri, busType, triggerName, preTriggerName);
	}

	/**
	 * 解析批量传输的数据
	 * 
	 * @param transFlag   传输业务的标识，用于标记一组批量传输的操作，作用类似于busType
	 * @param appUri      应用唯一标识
	 * @param triggerName 触发器注入名称，一般为类名首字母小写后的字符串，用于数据传输完成后，在接收端需要执行的一些业务逻辑，触发器类需要在接收端写好，实现ReceiveTrigger接口
	 * @return 响应结果
	 */
	@PostMapping("analysis_multi/{transFlag}/{token}/{appUri}/{triggerName}/{preTriggerName}")
	public Result analysisMulti(@PathVariable("transFlag") String transFlag, @PathVariable("appUri") String appUri, @PathVariable("triggerName") String triggerName, @PathVariable("preTriggerName") String preTriggerName) {
		return this.transmissionService.serverAnalysisMulti(appUri, transFlag, triggerName, preTriggerName);
	}

	/**
	 * 清除传输的临时文件
	 * 
	 * @param busType 业务类型
	 * @param appUri  应用唯一标识
	 * @return 响应结果
	 */
	@PostMapping("clean/{busType}/{token}/{appUri}")
	public Result cleanTempFile(@PathVariable("busType") String busType, @PathVariable("appUri") String appUri) {
		return this.transmissionService.serverCleanTempFile(busType, appUri);
	}

	@GetMapping("has_pull_data/{busType}/{token}/{appUri}")
	public Result hasPullData(@PathVariable("busType") String busType, @PathVariable("appUri") String appUri, String pullDataFlagId, String fileName) {
		if (StringUtils.isNotBlank(pullDataFlagId)) {
			return this.transmissionService.serverCleanPullFilePice(appUri, busType, pullDataFlagId, fileName);
		}
		if (this.transmissionService.serverHasPullData(appUri, busType)) {
			return new Result(true, "有可拉取的数据", null);
		}
		return new Result(false, "无可拉取的数据", null);
	}

	/**
	 * 拉取数据
	 * 
	 * @param busType  业务类型
	 * @param appUri   应用唯一标识
	 * @param request  请求对象
	 * @param response 响应对象
	 */
	@GetMapping("pull/{busType}/{token}/{appUri}")
	public void pull(@PathVariable("busType") String busType, @PathVariable("appUri") String appUri, HttpServletRequest request, HttpServletResponse response) {
		this.transmissionService.serverPull(appUri, busType, request, response);
	}

	/**
	 * 拉取成功，清除待拉取的临时文件
	 * 
	 * @param busType 业务类型
	 * @param appUri  应用唯一标识
	 * @return 响应结果
	 */
	@PostMapping("pull_success/{busType}/{token}/{appUri}/{triggerName}")
	public Result cleanPullFile(@PathVariable("busType") String busType, @PathVariable("appUri") String appUri, @PathVariable("triggerName") String triggerName) {
		return this.transmissionService.serverCleanPullFile(appUri, busType, triggerName);
	}

	/**
	 * 同一业务有多项多送数据，客户端每解析成功一条数据则删除一项推送的临时文件
	 * 
	 * @param busType        业务类型
	 * @param appUri         应用唯一标识
	 * @param pullDataFlagId 推送数据缓存表id
	 * @param fileName       推送临时文件名
	 * @return 响应结果
	 */
	@GetMapping("clean_pull_file_pice/{busType}/{token}/{appUri}")
	public Result cleanPullFilePice(@PathVariable("busType") String busType, @PathVariable("appUri") String appUri, String pullDataFlagId, String fileName) {
		return this.transmissionService.serverCleanPullFilePice(appUri, busType, pullDataFlagId, fileName);
	}

	/**
	 * 每次请求前都会检查连接是否成功
	 * 
	 * @return 响应结果
	 */
	@GetMapping("check_link/{token}")
	public Result checkLink() {
		return new Result(true, "连接成功");
	}

}

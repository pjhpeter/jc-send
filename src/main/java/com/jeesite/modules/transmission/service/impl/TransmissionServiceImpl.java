package com.jeesite.modules.transmission.service.impl;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jeesite.common.codec.AesUtils;
import com.jeesite.common.collect.ListUtils;
import com.jeesite.common.config.Global;
import com.jeesite.common.entity.DataEntity;
import com.jeesite.common.io.FileUtils;
import com.jeesite.common.mapper.JsonMapper;
import com.jeesite.common.mybatis.annotation.Table;
import com.jeesite.modules.file.entity.FileUpload;
import com.jeesite.modules.file.service.FileUploadService;
import com.jeesite.modules.transmission.annotation.PushField;
import com.jeesite.modules.transmission.annotation.PushTable;
import com.jeesite.modules.transmission.annotation.SendField;
import com.jeesite.modules.transmission.annotation.SendTable;
import com.jeesite.modules.transmission.entity.Client;
import com.jeesite.modules.transmission.entity.ExtraFile;
import com.jeesite.modules.transmission.entity.PullDataFlag;
import com.jeesite.modules.transmission.entity.Result;
import com.jeesite.modules.transmission.entity.SendFile;
import com.jeesite.modules.transmission.entity.TempFile;
import com.jeesite.modules.transmission.entity.TransEntity;
import com.jeesite.modules.transmission.service.PullDataFlagService;
import com.jeesite.modules.transmission.service.TempFileService;
import com.jeesite.modules.transmission.service.TransmissionService;
import com.jeesite.modules.transmission.trigger.ReceiveTrigger;
import com.jeesite.modules.transmission.util.Constant;
import com.jeesite.modules.transmission.util.DataBaseHandler;
import com.jeesite.modules.transmission.util.FileHandler;
import com.jeesite.modules.transmission.util.SpringContextsUtil;

/**
 * 数据传输接口实现类
 * 
 * @author 彭嘉辉
 *
 */
@Service(value = "transmissionService")
public class TransmissionServiceImpl implements TransmissionService {
	@Autowired
	private FileUploadService fileUploadService;
	@Autowired
	private TempFileService tempFileService;
	@Autowired
	private PullDataFlagService pullDataFlagService;
	@Autowired
	private FileHandler fileHandler;
	@Autowired
	private DataBaseHandler dataBaseHandler;
	@Autowired
	private SpringContextsUtil springContextsUtil;

	/*
	 * 批量传输的json数据
	 */
	private JSONArray tables;
	/*
	 * 批量传输的附件列表
	 */
	private List<File> fileList;
	/*
	 * 批量传输的额外文件
	 */
	private List<ExtraFile> extraFileList;

	@Override
	public <T extends DataEntity<?>> Result clientSend(List<T> list, Class<T> entityType, String busType, boolean renewal, boolean requireSysColumn) {
		if (renewal) {
			return renewal(list, entityType, new Client(), busType, null);
		}
		return sendData(list, entityType, new Client(), busType, requireSysColumn, null, null, null);
	}

	@Override
	public <T extends DataEntity<?>> Result clientSend(T entity, Class<T> entityType, String busType, boolean renewal, boolean requireSysColumn) {
		ArrayList<T> list = ListUtils.newArrayList();
		list.add(entity);
		if (renewal) {
			return renewal(list, entityType, new Client(), busType, null);
		}
		return sendData(list, entityType, new Client(), busType, requireSysColumn, null, null, null);
	}

	@Override
	public <T extends DataEntity<?>> Result clientSend(List<T> list, Class<T> entityType, String url, String busType, boolean renewal, boolean requireSysColumn) {
		if (renewal) {
			return renewal(list, entityType, new Client(url), busType, null);
		}
		return sendData(list, entityType, new Client(url), busType, requireSysColumn, null, null, null);
	}

	@Override
	public <T extends DataEntity<?>> Result clientSend(T entity, Class<T> entityType, String url, String busType, boolean renewal, boolean requireSysColumn) {
		ArrayList<T> list = ListUtils.newArrayList();
		list.add(entity);
		if (renewal) {
			return renewal(list, entityType, new Client(url), busType, null);
		}
		return sendData(list, entityType, new Client(url), busType, requireSysColumn, null, null, null);
	}

	@Override
	public <T extends DataEntity<?>> Result clientSend(List<T> list, Class<T> entityType, String busType, boolean renewal, boolean requireSysColumn, List<ExtraFile> extraFileList) {
		if (renewal) {
			return renewal(list, entityType, new Client(), busType, null);
		}
		return sendData(list, entityType, new Client(), busType, requireSysColumn, null, extraFileList, null);
	}

	@Override
	public <T extends DataEntity<?>> Result clientSend(T entity, Class<T> entityType, String busType, boolean renewal, boolean requireSysColumn, List<ExtraFile> extraFileList) {
		ArrayList<T> list = ListUtils.newArrayList();
		list.add(entity);
		if (renewal) {
			return renewal(list, entityType, new Client(), busType, null);
		}
		return sendData(list, entityType, new Client(), busType, requireSysColumn, null, extraFileList, null);
	}

	@Override
	public <T extends DataEntity<?>> Result clientSend(List<T> list, Class<T> entityType, String url, String busType, boolean renewal, boolean requireSysColumn, List<ExtraFile> extraFileList) {
		if (renewal) {
			return renewal(list, entityType, new Client(url), busType, null);
		}
		return sendData(list, entityType, new Client(url), busType, requireSysColumn, null, extraFileList, null);
	}

	@Override
	public <T extends DataEntity<?>> Result clientSend(T entity, Class<T> entityType, String url, String busType, boolean renewal, boolean requireSysColumn, List<ExtraFile> extraFileList) {
		ArrayList<T> list = ListUtils.newArrayList();
		list.add(entity);
		if (renewal) {
			return renewal(list, entityType, new Client(url), busType, null);
		}
		return sendData(list, entityType, new Client(url), busType, requireSysColumn, null, extraFileList, null);
	}

	@Override
	public <T extends DataEntity<?>> Result clientSend(TransEntity<T> transEntity) {
		List<T> list = null;
		Client client = null;
		List<ExtraFile> extraFileList = null;
		if (transEntity.getEntity() != null) {
			list = ListUtils.newArrayList();
			list.add(transEntity.getEntity());
		} else {
			list = transEntity.getList();
		}
		if (StringUtils.isNotBlank(transEntity.getUrl())) {
			client = new Client(transEntity.getUrl());
		} else {
			client = new Client();
		}
		if (transEntity.getExtraFile() != null) {
			extraFileList = ListUtils.newArrayList();
			extraFileList.add(transEntity.getExtraFile());
		} else if (transEntity.getExtraFileList() != null) {
			extraFileList = transEntity.getExtraFileList();
		}
		if (transEntity.isRenewal()) {
			// 断点续传
			return renewal(list, transEntity.getEntityType(), client, transEntity.getBusType(), transEntity.getTriggerName());
		}
		// 新的传输
		return sendData(list, transEntity.getEntityType(), client, transEntity.getBusType(), transEntity.isRequireSysColumn(), transEntity.getRequireSysColumnArr(), extraFileList,
				transEntity.getTriggerName());
	}

	@Override
	public <T extends DataEntity<?>> void addTransBatch(TransEntity<T> transEntity) throws Exception {
		List<T> list = null;
		List<ExtraFile> extraFileList = null;
		if (transEntity.getEntity() != null) {
			list = ListUtils.newArrayList();
			list.add(transEntity.getEntity());
		} else {
			list = transEntity.getList();
		}
		if (transEntity.getExtraFile() != null) {
			extraFileList = ListUtils.newArrayList();
			extraFileList.add(transEntity.getExtraFile());
		} else if (transEntity.getExtraFileList() != null) {
			extraFileList = transEntity.getExtraFileList();
		}

		// 初始化
		if (this.fileList == null) {
			this.fileList = ListUtils.newArrayList();
		}
		if (this.tables == null) {
			this.tables = new JSONArray();
		}
		if (this.extraFileList == null) {
			this.extraFileList = ListUtils.newArrayList();
		}

		// 生成json数据
		JSONObject table = jsonTableBuilder(list, transEntity.getEntityType(), this.fileList, transEntity.isRequireSysColumn(), transEntity.getRequireSysColumnArr());

		// 加入批处理列表中
		this.tables.add(table);
		if (extraFileList != null) {
			this.extraFileList.addAll(extraFileList);
		}
	}

	@Override
	public Result clientSendBatch(String transFlag, String url, String triggerName) {
		// 临时目录
		String tempPath = Global.getUserfilesBaseDir(Constant.TemplDir.SEND_TEMP + transFlag);
		// 应用唯一标识
		String appUri = Global.getConfig("appUri");
		// 存放json数据文件和传输相关附件文件的目录
		String jsonPath = tempPath + File.separator + "json";
		String jsonFileName = jsonPath + File.separator + appUri + transFlag + ".json";
		String zipName = tempPath + File.separator + appUri + transFlag + ".zip";

		// 创建压缩包
		buildZip(this.extraFileList, tempPath, jsonPath, jsonFileName, zipName, this.fileList, this.tables.toJSONString());

		// 分割压缩包文件
		List<TempFile> tempFileList = this.fileHandler.splitFile(zipName, transFlag);

		// 传输数据
		Client client = null;
		if (StringUtils.isNotBlank(url)) {
			client = new Client(url);
		} else {
			client = new Client();
		}
		transData(client, tempPath, tempFileList);

		// 传输成功后调用接收方数据解析接口
		return analysisMultiData(transFlag, client, triggerName);
	}

	@Override
	public boolean clientHasRenewal(String busType) {
		TempFile entity = new TempFile();
		entity.setBusType(busType);
		if (tempFileService.findCount(entity) > 0) {
			return true;
		}
		return false;
	}

	@Override
	public Result clientPull(String busType, String triggerName) {
		Client client = new Client();
		if (checkPullData(busType, client)) {
			if (StringUtils.isBlank(triggerName)) {
				triggerName = Constant.HAS_NO_TRIGGER;
			}
			// 拉取数据
			Result pullResult = client.pull(busType, triggerName);
			if (pullResult.isSuccess()) {
				// 解析数据
				String pullFileDir = Global.getUserfilesBaseDir(Constant.TemplDir.PULL_TEMP);
				String pullFileName = busType + ".zip";
				String unZipDir = pullFileDir + File.separator + busType;
				FileUtils.unZipFiles(pullFileDir + File.separator + pullFileName, pullFileDir + File.separator + busType);
				try {
					doAnalysis(unZipDir, busType + ".json");
					return new Result(true, Constant.Message.拉取成功);
				} catch (Exception e) {
					e.printStackTrace();
					return new Result(false, Constant.Message.拉取失败);
				}
			} else {
				return new Result(false, pullResult.getMsg());
			}
		}
		return new Result(false, Constant.Message.无可拉取数据);
	}

	@Override
	public <T extends DataEntity<?>> void export(TransEntity<T> transEntity, String exportFileName, HttpServletRequest request, HttpServletResponse response) {
		List<T> list = null;
		List<ExtraFile> extraFileList = null;
		if (transEntity.getEntity() != null) {
			list = ListUtils.newArrayList();
			list.add(transEntity.getEntity());
		} else {
			list = transEntity.getList();
		}
		if (transEntity.getExtraFile() != null) {
			extraFileList = ListUtils.newArrayList();
			extraFileList.add(transEntity.getExtraFile());
		} else if (transEntity.getExtraFileList() != null) {
			extraFileList = transEntity.getExtraFileList();
		}
		// 临时目录
		String tempPath = Global.getUserfilesBaseDir(Constant.TemplDir.EXPORT_TEMP + transEntity.getBusType());
		// 存放json数据文件和传输相关附件文件的目录
		String jsonPath = tempPath + File.separator + "json";
		String jsonFileName = jsonPath + File.separator + transEntity.getBusType() + ".json";
		// 压缩文件全名
		String zipName = "";
		if (StringUtils.isBlank(exportFileName)) {
			zipName = tempPath + File.separator + transEntity.getBusType() + ".zip";
		} else {
			zipName = tempPath + File.separator + exportFileName + ".zip";
		}
		List<File> fileList = ListUtils.newArrayList();
		try {
			// 生成json数据
			JSONObject json = jsonTableBuilder(list, transEntity.getEntityType(), fileList, transEntity.isRequireSysColumn(), transEntity.getRequireSysColumnArr());
			// 将所有要传输的数据压缩成压缩包
			buildZip(extraFileList, tempPath, jsonPath, jsonFileName, zipName, fileList, json.toJSONString());
			// 下载
			FileUtils.downFile(new File(zipName), request, response);
			// 删除临时文件
			FileUtils.deleteQuietly(new File(tempPath));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Result importData(MultipartFile file, String busType) {
		String tempDir = Global.getUserfilesBaseDir(Constant.TemplDir.IMPORT_TEMP);
		String fileName = file.getOriginalFilename();
		String zipName = tempDir + File.separator + fileName;
		String unZipDir = tempDir;
		String jsonFileName = busType + ".json";
		FileUtils.createDirectory(tempDir);
		try {
			FileUtils.copyToFile(file.getInputStream(), new File(zipName));
			// 解压
			FileUtils.unZipFiles(zipName, unZipDir);
			// 解析数据
			doAnalysis(unZipDir, jsonFileName);
			return new Result(true, Constant.Message.导入成功);
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, Constant.Message.导入失败);
		}
	}

	@Override
	public Result importDataBatch(MultipartFile file, String transFlag) {
		String tempDir = Global.getUserfilesBaseDir(Constant.TemplDir.IMPORT_TEMP);
		String fileName = file.getOriginalFilename();
		String zipName = tempDir + File.separator + fileName;
		String unZipDir = tempDir;
		String jsonFileName = transFlag + ".json";
		FileUtils.createDirectory(tempDir);
		try {
			FileUtils.copyToFile(file.getInputStream(), new File(zipName));
			// 解压
			FileUtils.unZipFiles(zipName, unZipDir);
			// 解析数据
			doAnalysisMulti(unZipDir, jsonFileName);
			return new Result(true, Constant.Message.导入成功);
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, Constant.Message.导入失败);
		}
	}

	@Override
	public void exportBatch(String transFlag, String exportFileName, HttpServletRequest request, HttpServletResponse response) {
		// 临时目录
		String tempPath = Global.getUserfilesBaseDir(Constant.TemplDir.EXPORT_TEMP + transFlag);
		// 存放json数据文件和传输相关附件文件的目录
		String jsonPath = tempPath + File.separator + "json";
		String jsonFileName = jsonPath + File.separator + transFlag + ".json";
		// 压缩文件全名
		String zipName = "";
		if (StringUtils.isBlank(exportFileName)) {
			zipName = tempPath + File.separator + transFlag + ".zip";
		} else {
			zipName = tempPath + File.separator + exportFileName + ".zip";
		}

		// 将所有要传输的数据压缩成压缩包
		buildZip(this.extraFileList, tempPath, jsonPath, jsonFileName, zipName, this.fileList, this.tables.toJSONString());

		// 下载
		FileUtils.downFile(new File(zipName), request, response);

		// 删除临时文件
		FileUtils.deleteQuietly(new File(tempPath));
	}

	/**
	 * 解析传输过来的数据
	 * 
	 * @param appUri
	 *            应用唯一标识
	 * @param busType
	 *            业务类型
	 * @param triggerName
	 *            数据解析成功后执行的触发器
	 * @return 响应結果
	 */
	public Result serverAnalysis(String appUri, String busType, String triggerName) {
		// 临时目录
		String tempDir = Global.getUserfilesBaseDir(Constant.TemplDir.RECEIVE_TEMP + appUri + busType);
		// 数据文件
		String targetFileName = tempDir + File.separator + appUri + busType + ".zip";
		// 解压后目录名
		String unZipDir = tempDir + File.separator + appUri + busType;
		String jsonFileName = appUri + busType + ".json";
		// 解压
		FileUtils.unZipFiles(targetFileName, unZipDir);
		try {
			JSONArray rows = doAnalysis(unZipDir, jsonFileName);
			if (!triggerName.equals(Constant.HAS_NO_TRIGGER)) {
				// 执行触发器
				@SuppressWarnings("static-access")
				ReceiveTrigger trigger = (ReceiveTrigger) springContextsUtil.getBean(triggerName);
				trigger.run(rows, busType);
			}
			return new Result(true, Constant.Message.解析成功);
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, Constant.Message.解析失败 + "：" + e.getMessage());
		} finally {
			// 删除临时文件
			FileUtils.deleteQuietly(new File(tempDir));
		}
	}

	/**
	 * 解析批量传输的数据
	 * 
	 * @param appUri
	 *            应用唯一标识
	 * @param transFlag
	 *            传输业务的标识，用于标记一组批量传输的操作，作用类似于busType
	 * @param triggerName
	 *            触发器注入名称，一般为类名首字母小写后的字符串，用于数据传输完成后，在接收端需要执行的一些业务逻辑，触发器类需要在接收端写好，实现ReceiveTrigger接口
	 * @return 响应结果
	 */
	public Result serverAnalysisMulti(String appUri, String transFlag, String triggerName) {
		// 临时目录
		String tempDir = Global.getUserfilesBaseDir(Constant.TemplDir.RECEIVE_TEMP + appUri + transFlag);
		// 数据文件
		String targetFileName = tempDir + File.separator + appUri + transFlag + ".zip";
		// 解压后目录名
		String unZipDir = tempDir + File.separator + appUri + transFlag;
		String jsonFileName = appUri + transFlag + ".json";
		// 解压
		FileUtils.unZipFiles(targetFileName, unZipDir);
		try {
			// 解析数据
			JSONArray tables = doAnalysisMulti(unZipDir, jsonFileName);
			if (!triggerName.equals(Constant.HAS_NO_TRIGGER)) {
				// 执行触发器
				@SuppressWarnings("static-access")
				ReceiveTrigger trigger = (ReceiveTrigger) springContextsUtil.getBean(triggerName);
				trigger.run(tables, transFlag);
			}
			return new Result(true, Constant.Message.解析成功);
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, Constant.Message.解析失败 + "：" + e.getMessage());
		} finally {
			// 删除临时文件
			FileUtils.deleteQuietly(new File(tempDir));
		}
	}

	/**
	 * 接收传输的文件
	 * 
	 * @param file
	 *            文件对象
	 * @param point
	 *            开始写入的位置
	 * @param busType
	 *            业务类型
	 * @param appUri
	 *            应用唯一标识
	 * @return 响应结果
	 */
	public Result serverReceive(MultipartFile file, long point, String busType, String appUri) {
		// 临时目录
		String tempDir = Global.getUserfilesBaseDir(Constant.TemplDir.RECEIVE_TEMP + appUri + busType);
		FileUtils.createDirectory(tempDir);
		try {
			// 合并文件
			if (fileHandler.mergeFile(tempDir + File.separator + appUri + busType + ".zip", file.getBytes(), point)) {
				return new Result(true, Constant.Message.操作成功);
			}
			return new Result(false, Constant.Message.操作失败);
		} catch (IOException e) {
			e.printStackTrace();
			return new Result(false, Constant.Message.操作失败);
		}
	}

	/**
	 * 接收数据前清空临时文件
	 * 
	 * @param busType
	 *            业务类型
	 * @param appUri
	 *            应用唯一标识
	 * @return 响应结果
	 */
	public Result serverCleanTempFile(String busType, String appUri) {
		// 临时目录
		String tempDir = Global.getUserfilesBaseDir(Constant.TemplDir.RECEIVE_TEMP + appUri + busType);
		if (FileUtils.deleteQuietly(new File(tempDir))) {
			return new Result(true, "删除临时目录文件成功");
		}
		return new Result(false, "删除临时目录文件失败");
	}

	/**
	 * 检测是否有可拉取的数据
	 * 
	 * @param appUri
	 *            应用唯一标识
	 * @param busType
	 *            业务类型
	 * @return 是否
	 */
	public boolean serverHasPullData(String appUri, String busType) {
		long count = pullDataFlagService.findCount(new PullDataFlag(appUri, busType, null));
		if (count > 0) {
			return true;
		}
		return false;
	}

	/**
	 * 拉取数据
	 * 
	 * @param appUri
	 *            应用唯一标识
	 * @param busType
	 *            业务类型
	 * @param request
	 *            请求对象
	 * @param response
	 *            响应对象
	 */
	public void serverPull(String appUri, String busType, HttpServletRequest request, HttpServletResponse response) {
		PullDataFlag pullDataFlag = pullDataFlagService.get(new PullDataFlag(appUri, busType, null));
		if (pullDataFlag != null) {
			String tempFileDir = Global.getUserfilesBaseDir(Constant.TemplDir.WEIT_FOR_PULL_TEMP);
			String tempFileName = appUri + busType + ".zip";
			FileUtils.downFile(new File(tempFileDir + File.separator + tempFileName), request, response);
		}
	}

	/**
	 * 清除待拉取的临时文件
	 * 
	 * @param appUri
	 *            应用唯一标识
	 * @param busType
	 *            业务类型
	 * @param triggerName
	 *            拉取数据成功后要执行的触发器注入名称
	 * @return 响应结果
	 */
	public Result serverCleanPullFile(String appUri, String busType, String triggerName) {
		PullDataFlag pullDataFlag = new PullDataFlag(appUri, busType, null);
		pullDataFlag = pullDataFlagService.get(pullDataFlag);
		// 调用这个接口说明拉取已经成功，执行拉取成功的触发器
		if (!triggerName.equals(Constant.HAS_NO_TRIGGER)) {
			@SuppressWarnings("static-access")
			ReceiveTrigger receiveTrigger = (ReceiveTrigger) springContextsUtil.getBean(triggerName);
			receiveTrigger.run(JSON.parseArray(pullDataFlag.getRowsJsonStr()), busType);
		}
		// 删除待拉取的临时文件
		String tempFileDir = Global.getUserfilesBaseDir(Constant.TemplDir.WEIT_FOR_PULL_TEMP);
		String tempFileName = appUri + busType + ".zip";
		if (FileUtils.deleteFile(tempFileDir + File.separator + tempFileName)) {
			// 删除数据库记录
			pullDataFlagService.delete(pullDataFlag);
			return new Result(true, "删除待拉取的临时文件成功");
		}
		return new Result(false, "删除待拉取的临时文件失败");
	}

	public <T extends DataEntity<?>> Result serverPush(String appUri, TransEntity<T> transEntity) {
		List<T> list = null;
		List<File> fileList = ListUtils.newArrayList();
		if (transEntity.getEntity() != null) {
			list = ListUtils.newArrayList();
			list.add(transEntity.getEntity());
		} else {
			list = transEntity.getList();
		}
		try {
			String tempPath = Global.getUserfilesBaseDir(Constant.TemplDir.WEIT_FOR_PULL_TEMP);
			String jsonPath = tempPath + File.separator + "json";
			String jsonFileName = jsonPath + File.separator + transEntity.getBusType() + ".json";
			String zipName = tempPath + File.separator + appUri + transEntity.getBusType() + ".zip";
			JSONObject json = jsonTableBuilder4Push(list, transEntity.getEntityType(), fileList, transEntity.isRequireSysColumn(), transEntity.getRequireSysColumnArr());
			System.out.println(json);
			buildZip(transEntity.getExtraFileList(), tempPath, jsonPath, jsonFileName, zipName, fileList, json.toJSONString());
			// 记录待拉取的标识
			PullDataFlag entity = new PullDataFlag(appUri, transEntity.getBusType(), json.getJSONArray("rows").toJSONString());
			try {
				// 新增
				entity.setIsNewRecord(true);
			} catch (Exception e) {
				// 更新
				entity.setIsNewRecord(false);
			}
			pullDataFlagService.save(entity);
			FileUtils.deleteQuietly(new File(jsonPath));
			return new Result(true, Constant.Message.推送成功);
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, Constant.Message.推送失败);
		}
	}

	/*
	 * 重新传输数据
	 */
	private <T extends DataEntity<?>> Result sendData(List<T> list, Class<T> entityType, Client client, String busType, boolean requireSysColumn, String[] requireSysColumnArr,
			List<ExtraFile> extraFileList, String triggerName) {
		// 重新传输前先删除之前的临时文件
		System.out.println("删除上次传输残留的文件");
		this.cleanHistoryData(busType, client);

		Result result = null;
		// 临时目录
		String tempPath = Global.getUserfilesBaseDir(Constant.TemplDir.SEND_TEMP + busType);
		// 应用唯一标识
		String appUri = Global.getConfig("appUri");
		// 存放json数据文件和传输相关附件文件的目录
		String jsonPath = tempPath + File.separator + "json";
		String jsonFileName = jsonPath + File.separator + appUri + busType + ".json";
		String zipName = tempPath + File.separator + appUri + busType + ".zip";
		List<File> fileList = ListUtils.newArrayList();
		try {
			// 生成json数据字符串和收集报送的附件
			JSONObject json = jsonTableBuilder(list, entityType, fileList, requireSysColumn, requireSysColumnArr);
			System.out.println(json);
			buildZip(extraFileList, tempPath, jsonPath, jsonFileName, zipName, fileList, json.toJSONString());
			// 将zip文件拆分成若干小块
			List<TempFile> tempFileList = fileHandler.splitFile(zipName, busType);
			// 发送文件
			transData(client, tempPath, tempFileList);
			// 调用对方的解析文件接口
			result = analysisData(busType, client, triggerName);
		} catch (Exception e) {
			e.printStackTrace();
			result = new Result(false, e.getMessage());
		}
		return result;
	}

	/*
	 * 生成需要传输的压缩吧
	 */
	private void buildZip(List<ExtraFile> extraFileList, String tempPath, String jsonPath, String jsonFileName, String zipName, List<File> fileList, String jsonStr) {
		// 加密数据
		String aesStr = AesUtils.encode(jsonStr, Constant.FILE_KEY);
		// 将json数据字符串写入文件中
		FileUtils.createDirectory(tempPath);
		FileUtils.createDirectory(jsonPath);
		FileUtils.writeToFile(jsonFileName, aesStr, true);
		// 将相关附件复制到json数据文件同目录下
		for (File file : fileList) {
			FileUtils.copyFile(file.getAbsolutePath(), jsonPath + File.separator + file.getName());
		}
		copyExtraFile(extraFileList, jsonPath);
		// 将所有东西压缩成zip并加密
		FileUtils.createFile(zipName);
		FileUtils.zipFiles(jsonPath, "*", zipName);
	}

	/*
	 * 将要额外传输的文件复制到临时目录
	 */
	private void copyExtraFile(List<ExtraFile> extraFileList, String jsonPath) {
		// 处理额外传输的文件
		if (extraFileList != null) {
			String extraFilesTempDir = jsonPath + File.separator + Constant.TemplDir.EXTRA_FILE_TEMP;
			FileUtils.createDirectory(extraFilesTempDir);
			extraFileList.forEach(extraFile -> {
				FileUtils.createDirectory(extraFilesTempDir + File.separator + extraFile.getPath());
				FileUtils.copyFile(Global.getUserfilesBaseDir(extraFile.getPath()) + File.separator + extraFile.getFileName(),
						extraFilesTempDir + File.separator + extraFile.getPath() + File.separator + extraFile.getFileName());
			});
		}
	}

	/*
	 * 断点续传
	 */
	private <T extends DataEntity<?>> Result renewal(List<T> list, Class<T> entityType, Client client, String busType, String triggerName) {
		// 获取临时文件信息
		TempFile entity = new TempFile();
		entity.setBusType(busType);
		List<TempFile> tempFileList = tempFileService.findList(entity);
		if (tempFileList.size() > 0) {
			// 临时目录
			String tempPath = Global.getUserfilesBaseDir(Constant.TemplDir.SEND_TEMP + busType);
			// 发送文件
			transData(client, tempPath, tempFileList);
			// 调用对方的解析文件接口
			if (analysisData(busType, client, triggerName).isSuccess()) {
				return new Result(true, Constant.Message.传输成功);
			}
			return new Result(false, Constant.Message.传输失败);
		}
		return new Result(false, Constant.Message.无可传输文件);
	}

	/*
	 * 多线程传输数据
	 */
	private void transData(Client client, String tempPath, List<TempFile> tempFileList) {
		// 获取所有临时碎片文件信息
		int piceCount = tempFileList.size();
		// 创建线程池，一个线程传输一个碎片文件
		ExecutorService threadPool = Executors.newFixedThreadPool(piceCount);
		List<Callable<Void>> threadList = ListUtils.newArrayList();
		for (int i = 0; i < piceCount; i++) {
			TempFile tempFileEntity = tempFileList.get(i);
			threadList.add(new TransExecutor(client, tempFileEntity));
		}
		try {
			// 发送文件
			threadPool.invokeAll(threadList);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			threadPool.shutdown();
		}
		// 删除临时目录
		File temp = new File(tempPath);
		FileUtils.deleteQuietly(temp);
		System.out.println("删除临时文件成功");
	}

	/*
	 * 解析集合，生成报送json
	 */
	private <T extends DataEntity<?>> JSONObject jsonTableBuilder(List<T> list, Class<T> entityType, List<File> fileList, boolean requireSysColumn, String[] requireSysColumnArr) throws Exception {
		JSONObject table = new JSONObject();
		JSONArray rows = new JSONArray();
		String tableName = "";
		if (entityType.isAnnotationPresent(SendTable.class)) {
			tableName = entityType.getAnnotation(SendTable.class).to();
		} else {
			tableName = entityType.getAnnotation(Table.class).name();
		}
		table.put("table", tableName);
		for (DataEntity<?> entity : list) {
			rows.add(jsonRowBuilder(entity, fileList, requireSysColumn, requireSysColumnArr));
		}
		table.put("rows", rows);
		return table;
	}

	/*
	 * 解析集合，生成推送json
	 */
	private <T extends DataEntity<?>> JSONObject jsonTableBuilder4Push(List<T> list, Class<T> entityType, List<File> fileList, boolean requireSysColumn, String[] requireSysColumnArr)
			throws Exception {
		JSONObject table = new JSONObject();
		JSONArray rows = new JSONArray();
		String tableName = "";
		if (entityType.isAnnotationPresent(PushTable.class)) {
			tableName = entityType.getAnnotation(PushTable.class).to();
		} else {
			tableName = entityType.getAnnotation(Table.class).name();
		}
		table.put("table", tableName);
		for (DataEntity<?> entity : list) {
			rows.add(jsonRowBuilder4Push(entity, fileList, requireSysColumn, requireSysColumnArr));
		}
		table.put("rows", rows);
		return table;
	}

	/*
	 * 解析实体，生成报送json
	 */
	private <T extends DataEntity<?>> JSONObject jsonRowBuilder(T entity, List<File> fileList, boolean requireSysColumn, String[] requireSysColumnArr) throws Exception {
		// 拼接表数据json
		// 设置主键
		JSONObject row = new JSONObject();
		JSONArray rowData = new JSONArray();
		@SuppressWarnings("rawtypes")
		Class<? extends DataEntity> css = entity.getClass();
		// 主键集合
		List<Field> pkList = ListUtils.newArrayList();
		Field[] fields = entity.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (field.isAnnotationPresent(SendField.class)) {
				SendField annotation = field.getAnnotation(SendField.class);
				// 获取接收方对应数据库列名
				String to = annotation.to();
				if (StringUtils.isBlank(to)) {
					to = toUnderLine(field.getName());
				}
				// 获取成员变量的值
				PropertyDescriptor pd = new PropertyDescriptor(field.getName(), css);
				Method readMethod = pd.getReadMethod();
				Class<?> type = field.getType();
				Object obj = readMethod.invoke(entity);
				if (obj != null) {
					JSONObject column = new JSONObject();
					if (annotation.isPK()) {
						pkList.add(field);
						column.put("isPK", true);
					}
					column.put("to", to);
					if (type.isAssignableFrom(Number.class)) {// 判断是否数字
						if (type.isAssignableFrom(Integer.class)) {
							column.put("value", Integer.parseInt(obj.toString()));
						} else if (type.isAssignableFrom(Float.class)) {
							column.put("value", Float.parseFloat(obj.toString()));
						} else if (type.isAssignableFrom(Double.class)) {
							column.put("value", Double.parseDouble(obj.toString()));
						} else if (type.isAssignableFrom(Long.class)) {
							column.put("value", Long.parseLong(obj.toString()));
						} else {
							column.put("value", Short.parseShort(obj.toString()));
						}
						column.put("type", "number");
					} else if (type.isAssignableFrom(Boolean.class)) {// 布尔值
						column.put("value", Boolean.parseBoolean(obj.toString()));
						column.put("type", "boolean");
					} else if (type.isAssignableFrom(Date.class)) {// 日期
						column.put("value", obj);
						column.put("type", "date");
					} else {// 字符串
						column.put("value", obj.toString());
						column.put("type", "string");
					}
					rowData.add(column);
				}
			}
		}

		// 拼接主键信息
		JSONObject idJson = new JSONObject();
		if (pkList.size() > 0) {// 联合组建
			for (Field field : pkList) {
				String idKey = "";
				// 获取接收方对应数据库列名
				if (field.isAnnotationPresent(SendField.class)) {
					idKey = field.getAnnotation(SendField.class).to();
					if (StringUtils.isBlank(idKey)) {
						idKey = toUnderLine(field.getName());
					}
				} else {
					idKey = toUnderLine(field.getName());
				}
				// 获取成员变量的值
				PropertyDescriptor pd = new PropertyDescriptor(field.getName(), css);
				Method readMethod = pd.getReadMethod();
				Object obj = readMethod.invoke(entity);
				if (obj != null) {
					idJson.put(idKey, obj);
				}
			}
		} else {
			idJson.put("id", entity.getId());
			// 把id加到报送字段中
			JSONObject id = new JSONObject();
			id.put("isPK", true);
			id.put("to", "id");
			id.put("value", entity.getId());
			id.put("type", "string");
			rowData.add(id);
		}

		// 系统五个默认字段
		if (requireSysColumn) {
			Date createDate = entity.getCreateDate();
			Date updateDate = entity.getUpdateDate();
			String createBy = entity.getCreateBy();
			String updateBy = entity.getUpdateBy();
			String status = entity.getStatus();
			if (StringUtils.isNotBlank(status)) {
				JSONObject statusJson = new JSONObject();
				statusJson.put("to", Constant.SysCoumn.STATUS);
				statusJson.put("value", status);
				statusJson.put("type", "string");
				rowData.add(statusJson);
			}
			if (StringUtils.isNotBlank(createBy)) {
				JSONObject createByJson = new JSONObject();
				createByJson.put("to", Constant.SysCoumn.CREATE_BY);
				createByJson.put("value", createBy);
				createByJson.put("type", "string");
				rowData.add(createByJson);
			}
			if (createDate != null) {
				JSONObject createDateJson = new JSONObject();
				createDateJson.put("to", Constant.SysCoumn.CREATE_DATE);
				createDateJson.put("value", createDate);
				createDateJson.put("type", "date");
				rowData.add(createDateJson);
			}
			if (StringUtils.isNotBlank(updateBy)) {
				JSONObject updateByJson = new JSONObject();
				updateByJson.put("to", Constant.SysCoumn.UPDATE_BY);
				updateByJson.put("value", updateBy);
				updateByJson.put("type", "string");
				rowData.add(updateByJson);
			}
			if (updateDate != null) {
				JSONObject updateDateJson = new JSONObject();
				updateDateJson.put("to", Constant.SysCoumn.UPDATE_DATE);
				updateDateJson.put("value", updateDate);
				updateDateJson.put("type", "date");
				rowData.add(updateDateJson);
			}
		} else if (requireSysColumnArr != null) {
			for (String columnName : requireSysColumnArr) {
				if (columnName.equals(Constant.SysCoumn.STATUS)) {
					String status = entity.getStatus();
					if (StringUtils.isNotBlank(status)) {
						JSONObject statusJson = new JSONObject();
						statusJson.put("to", Constant.SysCoumn.STATUS);
						statusJson.put("value", status);
						statusJson.put("type", "string");
						rowData.add(statusJson);
					}
				} else if (columnName.equals(Constant.SysCoumn.CREATE_BY)) {
					String createBy = entity.getCreateBy();
					if (StringUtils.isNotBlank(createBy)) {
						JSONObject createByJson = new JSONObject();
						createByJson.put("to", Constant.SysCoumn.CREATE_BY);
						createByJson.put("value", createBy);
						createByJson.put("type", "string");
						rowData.add(createByJson);
					}
				} else if (columnName.equals(Constant.SysCoumn.CREATE_DATE)) {
					Date createDate = entity.getCreateDate();
					if (createDate != null) {
						JSONObject createDateJson = new JSONObject();
						createDateJson.put("to", "create_date");
						createDateJson.put("value", createDate);
						createDateJson.put("type", "date");
						rowData.add(createDateJson);
					}
				} else if (columnName.equals(Constant.SysCoumn.UPDATE_BY)) {
					String updateBy = entity.getUpdateBy();
					if (StringUtils.isNotBlank(updateBy)) {
						JSONObject updateByJson = new JSONObject();
						updateByJson.put("to", Constant.SysCoumn.UPDATE_BY);
						updateByJson.put("value", updateBy);
						updateByJson.put("type", "string");
						rowData.add(updateByJson);
					}
				} else if (columnName.equals(Constant.SysCoumn.UPDATE_DATE)) {
					Date updateDate = entity.getUpdateDate();
					if (updateDate != null) {
						JSONObject updateDateJson = new JSONObject();
						updateDateJson.put("to", Constant.SysCoumn.UPDATE_DATE);
						updateDateJson.put("value", updateDate);
						updateDateJson.put("type", "date");
						rowData.add(updateDateJson);
					}
				}
			}
		}

		row.put("id", idJson);
		row.put("rowData", rowData);

		// 获取附件信息
		FileUpload fileUpload = new FileUpload();
		fileUpload.setBizKey(entity.getId());
		List<FileUpload> fileUploadList = fileUploadService.findList(fileUpload);
		if (fileUploadList.size() > 0) {
			JSONArray fileJsonArr = new JSONArray();
			for (FileUpload fileUploadEntity : fileUploadList) {
				JSONObject json = JSON.parseObject(JsonMapper.toJson(fileUploadEntity));
				fileList.add(new File(Global.getUserfilesBaseDir("fileupload") + File.separator + fileUploadEntity.getFileEntity().getFilePath() + fileUploadEntity.getFileEntity().getFileId() + "."
						+ fileUploadEntity.getFileEntity().getFileExtension()));
				SendFile sendFile = new SendFile();
				sendFile.setPath(fileUploadEntity.getFileEntity().getFilePath());
				sendFile.setFileName(fileUploadEntity.getFileEntity().getFileId() + "." + fileUploadEntity.getFileEntity().getFileExtension());
				json.put("fileInfo", JSON.parseObject(JsonMapper.toJson(sendFile)));
				fileJsonArr.add(json);
			}
			row.put("fileList", fileJsonArr);
		}

		return row;
	}

	/*
	 * 解析实体，生成报送json
	 */
	private <T extends DataEntity<?>> JSONObject jsonRowBuilder4Push(T entity, List<File> fileList, boolean requireSysColumn, String[] requireSysColumnArr) throws Exception {
		// 拼接表数据json
		// 设置主键
		JSONObject row = new JSONObject();
		JSONArray rowData = new JSONArray();
		@SuppressWarnings("rawtypes")
		Class<? extends DataEntity> css = entity.getClass();
		// 主键集合
		List<Field> pkList = ListUtils.newArrayList();
		Field[] fields = entity.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (field.isAnnotationPresent(PushField.class)) {
				PushField annotation = field.getAnnotation(PushField.class);
				// 获取接收方对应数据库列名
				String to = annotation.to();
				if (StringUtils.isBlank(to)) {
					to = toUnderLine(field.getName());
				}
				// 获取成员变量的值
				PropertyDescriptor pd = new PropertyDescriptor(field.getName(), css);
				Method readMethod = pd.getReadMethod();
				Class<?> type = field.getType();
				Object obj = readMethod.invoke(entity);
				if (obj != null) {
					JSONObject column = new JSONObject();
					if (annotation.isPK()) {
						pkList.add(field);
						column.put("isPK", true);
					}
					column.put("to", to);
					if (type.isAssignableFrom(Number.class)) {// 判断是否数字
						if (type.isAssignableFrom(Integer.class)) {
							column.put("value", Integer.parseInt(obj.toString()));
						} else if (type.isAssignableFrom(Float.class)) {
							column.put("value", Float.parseFloat(obj.toString()));
						} else if (type.isAssignableFrom(Double.class)) {
							column.put("value", Double.parseDouble(obj.toString()));
						} else if (type.isAssignableFrom(Long.class)) {
							column.put("value", Long.parseLong(obj.toString()));
						} else {
							column.put("value", Short.parseShort(obj.toString()));
						}
						column.put("type", "number");
					} else if (type.isAssignableFrom(Boolean.class)) {// 布尔值
						column.put("value", Boolean.parseBoolean(obj.toString()));
						column.put("type", "boolean");
					} else if (type.isAssignableFrom(Date.class)) {// 日期
						column.put("value", obj);
						column.put("type", "date");
					} else {// 字符串
						column.put("value", obj.toString());
						column.put("type", "string");
					}
					rowData.add(column);
				}
			}
		}

		// 拼接主键信息
		JSONObject idJson = new JSONObject();
		if (pkList.size() > 0) {// 联合组建
			for (Field field : pkList) {
				String idKey = "";
				// 获取接收方对应数据库列名
				if (field.isAnnotationPresent(PushField.class)) {
					idKey = field.getAnnotation(PushField.class).to();
					if (StringUtils.isBlank(idKey)) {
						idKey = toUnderLine(field.getName());
					}
				} else {
					idKey = toUnderLine(field.getName());
				}
				// 获取成员变量的值
				PropertyDescriptor pd = new PropertyDescriptor(field.getName(), css);
				Method readMethod = pd.getReadMethod();
				Object obj = readMethod.invoke(entity);
				if (obj != null) {
					idJson.put(idKey, obj);
				}
			}
		} else {
			idJson.put("id", entity.getId());
			// 把id加到报送字段中
			JSONObject id = new JSONObject();
			id.put("isPK", true);
			id.put("to", "id");
			id.put("value", entity.getId());
			id.put("type", "string");
			rowData.add(id);
		}

		// 系统五个默认字段
		if (requireSysColumn) {
			Date createDate = entity.getCreateDate();
			Date updateDate = entity.getUpdateDate();
			String createBy = entity.getCreateBy();
			String updateBy = entity.getUpdateBy();
			String status = entity.getStatus();
			if (StringUtils.isNotBlank(status)) {
				JSONObject statusJson = new JSONObject();
				statusJson.put("to", Constant.SysCoumn.STATUS);
				statusJson.put("value", status);
				statusJson.put("type", "string");
				rowData.add(statusJson);
			}
			if (StringUtils.isNotBlank(createBy)) {
				JSONObject createByJson = new JSONObject();
				createByJson.put("to", Constant.SysCoumn.CREATE_BY);
				createByJson.put("value", createBy);
				createByJson.put("type", "string");
				rowData.add(createByJson);
			}
			if (createDate != null) {
				JSONObject createDateJson = new JSONObject();
				createDateJson.put("to", Constant.SysCoumn.CREATE_DATE);
				createDateJson.put("value", createDate);
				createDateJson.put("type", "date");
				rowData.add(createDateJson);
			}
			if (StringUtils.isNotBlank(updateBy)) {
				JSONObject updateByJson = new JSONObject();
				updateByJson.put("to", Constant.SysCoumn.UPDATE_BY);
				updateByJson.put("value", updateBy);
				updateByJson.put("type", "string");
				rowData.add(updateByJson);
			}
			if (updateDate != null) {
				JSONObject updateDateJson = new JSONObject();
				updateDateJson.put("to", Constant.SysCoumn.UPDATE_DATE);
				updateDateJson.put("value", updateDate);
				updateDateJson.put("type", "date");
				rowData.add(updateDateJson);
			}
		} else if (requireSysColumnArr != null) {
			for (String columnName : requireSysColumnArr) {
				if (columnName.equals(Constant.SysCoumn.STATUS)) {
					String status = entity.getStatus();
					if (StringUtils.isNotBlank(status)) {
						JSONObject statusJson = new JSONObject();
						statusJson.put("to", Constant.SysCoumn.STATUS);
						statusJson.put("value", status);
						statusJson.put("type", "string");
						rowData.add(statusJson);
					}
				} else if (columnName.equals(Constant.SysCoumn.CREATE_BY)) {
					String createBy = entity.getCreateBy();
					if (StringUtils.isNotBlank(createBy)) {
						JSONObject createByJson = new JSONObject();
						createByJson.put("to", Constant.SysCoumn.CREATE_BY);
						createByJson.put("value", createBy);
						createByJson.put("type", "string");
						rowData.add(createByJson);
					}
				} else if (columnName.equals(Constant.SysCoumn.CREATE_DATE)) {
					Date createDate = entity.getCreateDate();
					if (createDate != null) {
						JSONObject createDateJson = new JSONObject();
						createDateJson.put("to", "create_date");
						createDateJson.put("value", createDate);
						createDateJson.put("type", "date");
						rowData.add(createDateJson);
					}
				} else if (columnName.equals(Constant.SysCoumn.UPDATE_BY)) {
					String updateBy = entity.getUpdateBy();
					if (StringUtils.isNotBlank(updateBy)) {
						JSONObject updateByJson = new JSONObject();
						updateByJson.put("to", Constant.SysCoumn.UPDATE_BY);
						updateByJson.put("value", updateBy);
						updateByJson.put("type", "string");
						rowData.add(updateByJson);
					}
				} else if (columnName.equals(Constant.SysCoumn.UPDATE_DATE)) {
					Date updateDate = entity.getUpdateDate();
					if (updateDate != null) {
						JSONObject updateDateJson = new JSONObject();
						updateDateJson.put("to", Constant.SysCoumn.UPDATE_DATE);
						updateDateJson.put("value", updateDate);
						updateDateJson.put("type", "date");
						rowData.add(updateDateJson);
					}
				}
			}
		}

		row.put("id", idJson);
		row.put("rowData", rowData);

		// 获取附件信息
		FileUpload fileUpload = new FileUpload();
		fileUpload.setBizKey(entity.getId());
		List<FileUpload> fileUploadList = fileUploadService.findList(fileUpload);
		if (fileUploadList.size() > 0) {
			JSONArray fileJsonArr = new JSONArray();
			for (FileUpload fileUploadEntity : fileUploadList) {
				JSONObject json = JSON.parseObject(JsonMapper.toJson(fileUploadEntity));
				fileList.add(new File(Global.getUserfilesBaseDir("fileupload") + File.separator + fileUploadEntity.getFileEntity().getFilePath() + fileUploadEntity.getFileEntity().getFileId() + "."
						+ fileUploadEntity.getFileEntity().getFileExtension()));
				SendFile sendFile = new SendFile();
				sendFile.setPath(fileUploadEntity.getFileEntity().getFilePath());
				sendFile.setFileName(fileUploadEntity.getFileEntity().getFileId() + "." + fileUploadEntity.getFileEntity().getFileExtension());
				json.put("fileInfo", JSON.parseObject(JsonMapper.toJson(sendFile)));
				fileJsonArr.add(json);
			}
			row.put("fileList", fileJsonArr);
		}

		return row;
	}

	/*
	 * 驼峰式命名转化成下划线名称
	 */
	private String toUnderLine(String humpName) {
		Pattern pattern = Pattern.compile("[A-Z]");
		Matcher matcher = pattern.matcher(humpName);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/*
	 * 重新传输前需要清楚上一次的临时数据
	 */
	private void cleanHistoryData(String busType, Client client) {
		// 删除远端临时文件
		Result result = client.cleanTempFile(busType);
		System.out.println(result.toString());
		// 删除本地临时文件
		FileUtils.deleteQuietly(new File(Global.getUserfilesBaseDir(Constant.TemplDir.SEND_TEMP + busType)));
		// 删除数据库临时文件记录
		TempFile entity = new TempFile();
		entity.setBusType(busType);
		List<TempFile> findList = this.tempFileService.findList(entity);
		for (TempFile tempFile : findList) {
			this.tempFileService.delete(tempFile);
		}
	}

	/*
	 * 调用远端解析数据接口
	 */
	private Result analysisData(String busType, Client client, String triggerName) {
		return client.analysis(busType, triggerName);
	}

	/*
	 * 调用远端针对批量传输的解析数据接口
	 */
	private Result analysisMultiData(String transFlag, Client client, String triggerName) {
		return client.analysisMulti(transFlag, triggerName);
	}

	/*
	 * 检测是否有可拉取的数据
	 */
	private boolean checkPullData(String busType, Client client) {
		try {
			Result result = client.hasPullData(busType);
			return result.isSuccess();
		} catch (NullPointerException e) {
			try {
				throw new Exception(Constant.Message.服务没响应);
			} catch (Exception e1) {
				e1.printStackTrace();
				return false;
			}
		}
	}

	/*
	 * 解析数据
	 */
	private JSONArray doAnalysis(String unZipDir, String jsonFileName) throws Exception {
		// 读取json数据
		String aesStr = FileUtils.readFileToString(new File(unZipDir + File.separator + jsonFileName), "UTF-8");
		String jsonStr = AesUtils.decode(aesStr, Constant.FILE_KEY);
		System.out.println(jsonStr);
		JSONObject jsonObj = JSON.parseObject(jsonStr);
		// 数据库表名
		String tableName = jsonObj.getString("table");
		// 行数据json
		JSONArray rows = jsonObj.getJSONArray("rows");
		// 数据库数据和附件文件
		this.dataBaseHandler.setData(tableName, rows, unZipDir);
		// 处理额外传输文件
		File extraFileDir = new File(unZipDir + File.separator + Constant.TemplDir.EXTRA_FILE_TEMP);
		if (extraFileDir.exists()) {
			List<String> childrenList = FileUtils.findChildrenList(extraFileDir, true);
			childrenList.forEach(fileName -> {
				File file = new File(extraFileDir + File.separator + fileName);
				if (file.isDirectory()) {
					try {
						// jeesite的copyDirectory不给力，有bug，逼我用apache的-_-
						org.apache.commons.io.FileUtils.copyDirectory(file, new File(Global.getUserfilesBaseDir(fileName)));
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					FileUtils.copyFileCover(file.getAbsolutePath(), Global.getUserfilesBaseDir(fileName), true);
				}
			});
		}
		// 提交数据
		this.dataBaseHandler.commit();
		return rows;
	}

	/*
	 * 解析批量传输的数据
	 */
	private JSONArray doAnalysisMulti(String unZipDir, String jsonFileName) throws Exception {
		// 读取json数据
		String aesStr = FileUtils.readFileToString(new File(unZipDir + File.separator + jsonFileName), "UTF-8");
		String jsonStr = AesUtils.decode(aesStr, Constant.FILE_KEY);
		System.out.println(jsonStr);

		// 解析json数据
		JSONArray jsonArr = JSON.parseArray(jsonStr);
		for (Object object : jsonArr) {
			JSONObject jsonObj = JSON.parseObject(object.toString());
			// 数据库表名
			String tableName = jsonObj.getString("table");
			// 行数据json
			JSONArray rows = jsonObj.getJSONArray("rows");
			// 数据库数据和附件文件
			this.dataBaseHandler.setData(tableName, rows, unZipDir);
		}

		// 处理额外传输文件
		File extraFileDir = new File(unZipDir + File.separator + Constant.TemplDir.EXTRA_FILE_TEMP);
		if (extraFileDir.exists()) {
			List<String> childrenList = FileUtils.findChildrenList(extraFileDir, true);
			childrenList.forEach(fileName -> {
				File file = new File(extraFileDir + File.separator + fileName);
				if (file.isDirectory()) {
					try {
						// jeesite的copyDirectory不给力，有bug，逼我用apache的-_-
						org.apache.commons.io.FileUtils.copyDirectory(file, new File(Global.getUserfilesBaseDir(fileName)));
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					FileUtils.copyFileCover(file.getAbsolutePath(), Global.getUserfilesBaseDir(fileName), true);
				}
			});
		}

		// 提交数据
		this.dataBaseHandler.commit();

		return jsonArr;
	}

	/*
	 * 传输文件执行者
	 */
	private class TransExecutor implements Callable<Void> {
		private Client client;
		private TempFile tempFile;

		/**
		 * 传输文件执行者的构造函数
		 * 
		 * @param client
		 *            请求配置对象
		 * @param tempFile
		 *            临时碎片文件信息对象
		 */
		private TransExecutor(Client client, TempFile tempFile) {
			this.client = client;
			this.tempFile = tempFile;
		}

		@Override
		public Void call() throws Exception {
			Result result = this.client.send(this.tempFile.getPath() + File.separator + this.tempFile.getPiceFileName(), this.tempFile.getPoint(), this.tempFile.getBusType());
			System.out.println("响应结果为：" + result.toString());
			// 发送成功后删除临时碎片文件信息
			if (result.isSuccess()) {
				tempFileService.delete(this.tempFile);
			}
			return null;
		}

	}

}

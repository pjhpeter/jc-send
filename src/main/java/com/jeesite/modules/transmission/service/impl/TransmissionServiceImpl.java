package com.jeesite.modules.transmission.service.impl;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import com.jeesite.common.entity.TreeEntity;
import com.jeesite.common.idgen.IdGen;
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
	 * 批量发送的json数据
	 */
	private JSONArray sendTables;
	/*
	 * 批量发送的附件列表
	 */
	private List<File> sendFileList;
	/*
	 * 批量发送的额外文件
	 */
	private List<ExtraFile> sendExtraFileList;

	/*
	 * 批量推送的json数据
	 */
	private JSONArray pushTables;
	/*
	 * 批量推送的附件列表
	 */
	private List<File> pushFileList;
	/*
	 * 批量推送的额外文件
	 */
	private List<ExtraFile> pushExtraFileList;

	@Override
	public <T extends DataEntity<?>> Result clientSend(TransEntity<T> transEntity) {
		Client client = null;
		List<ExtraFile> extraFileList = null;
		if (transEntity.getEntity() != null) {
			List<T> list = ListUtils.newArrayList();
			list.add(transEntity.getEntity());
			transEntity.setList(list);
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
			return renewal(client, transEntity.getBusType(), transEntity.getTriggerName(), false);
		}
		// 新的传输
		return sendData(transEntity, client);
	}

	@Override
	public <T extends DataEntity<?>> void addSendBatch(TransEntity<T> transEntity) throws Exception {
		addTransBatch(transEntity, false);
	}

	@Override
	public <T extends DataEntity<?>> void addPushBatch(TransEntity<T> transEntity) throws Exception {
		addTransBatch(transEntity, true);
	}

	@Override
	public Result clientSendBatch(String transFlag, boolean renewal, String url, String triggerName) {
		// 传输数据
		Client client = null;
		if (StringUtils.isNotBlank(url)) {
			client = new Client(url);
		} else {
			client = new Client();
		}

		// 断点续传
		if (renewal) {
			return renewal(client, transFlag, triggerName, true);
		}

		// 临时目录
		String tempPath = Global.getUserfilesBaseDir(Constant.TemplDir.SEND_TEMP + transFlag);
		// 应用唯一标识
		String appUri = Global.getConfig("appUri");
		// 存放json数据文件和传输相关附件文件的目录
		String jsonPath = tempPath + File.separator + "json";
		String jsonFileName = jsonPath + File.separator + appUri + transFlag + ".json";
		String zipName = tempPath + File.separator + appUri + transFlag + ".zip";

		// 创建压缩包
		buildZip(this.sendExtraFileList, tempPath, jsonPath, jsonFileName, zipName, this.sendFileList, this.sendTables.toJSONString());

		// 分割压缩包文件
		List<TempFile> tempFileList = this.fileHandler.splitFile(zipName, transFlag);

		transData(client, tempPath, tempFileList);

		// 清除批处理临时变量
		cleanSendBatch();

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
	public boolean clientHasPullData(String busType) {
		Client client = new Client();
		return this.checkPullData(busType, client);
	}

	@Override
	public Result clientPull(String busType, String triggerName) {
		Client client = new Client();
		if (checkPullData(busType, client)) {
			if (StringUtils.isBlank(triggerName)) {
				triggerName = Constant.HAS_NO_TRIGGER;
			}
			// 拉取数据
			Result pullResult = client.pull(busType);
			if (pullResult.isSuccess()) {
				// 解析数据
				String pullFileDir = Global.getUserfilesBaseDir(Constant.TemplDir.PULL_TEMP + "_" + busType);
				String pullFileName = busType + ".zip";
				String unZipDir = pullFileDir + File.separator + busType;
				FileUtils.unZipFiles(pullFileDir + File.separator + pullFileName, pullFileDir + File.separator + busType);
				try {
					File unZipDirFile = new File(unZipDir);
					File[] listFiles = unZipDirFile.listFiles();
					JSONArray tables = new JSONArray();
					for (File file : listFiles) {
						String descFileName = unZipDir + File.separator + file.getName().substring(0, file.getName().lastIndexOf(".") + 1);
						FileUtils.unZipFiles(file.getAbsolutePath(), descFileName);
						tables.addAll(doAnalysisMulti(descFileName, busType + ".json"));
					}
					// 清除推送端的临时文件
					cleanPushTempFile(busType, triggerName, client);
					return new Result(true, Constant.Message.拉取成功, tables.toJSONString());
				} catch (Exception e) {
					e.printStackTrace();
					return new Result(false, Constant.Message.拉取失败);
				} finally {
					// 删除临时文件
					FileUtils.deleteQuietly(new File(pullFileDir));
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
			JSONObject json = jsonTableBuilder(transEntity, fileList, list);
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
	public Result importData(MultipartFile file, String busType, String triggerName) {
		String tempDir = Global.getUserfilesBaseDir(Constant.TemplDir.IMPORT_TEMP);
		String fileName = file.getOriginalFilename();
		String zipName = tempDir + File.separator + fileName;
		String unZipDir = tempDir + File.separator + fileName.substring(0, fileName.lastIndexOf("."));
		String jsonFileName = busType + ".json";
		FileUtils.createDirectory(tempDir);
		try {
			FileUtils.copyToFile(file.getInputStream(), new File(zipName));
			// 解压
			FileUtils.unZipFiles(zipName, unZipDir);
			// 解析数据
			JSONArray tables = null;
			try {
				tables = doAnalysis(unZipDir, jsonFileName);
			}catch(Exception e) {
				tables = doAnalysisMulti(unZipDir, jsonFileName);
			}
			excuteTrigger(busType, triggerName, tables);
			return new Result(true, Constant.Message.导入成功, tables.toJSONString());
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, Constant.Message.导入失败);
		} finally {
			FileUtils.deleteDirectory(unZipDir);
		}
	}

	@Override
	public Result importDataBatch(MultipartFile file, String transFlag, String triggerName) {
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
			JSONArray tables = doAnalysisMulti(unZipDir, jsonFileName);
			excuteTrigger(transFlag, triggerName, tables);
			return new Result(true, Constant.Message.导入成功, tables.toJSONString());
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
		buildZip(this.sendExtraFileList, tempPath, jsonPath, jsonFileName, zipName, this.sendFileList, this.sendTables.toJSONString());

		// 下载
		FileUtils.downFile(new File(zipName), request, response);

		// 删除临时文件
		FileUtils.deleteQuietly(new File(tempPath));

		// 清除临时变量
		cleanSendBatch();
	}

	/**
	 * 解析传输过来的数据
	 * 
	 * @param appUri      应用唯一标识
	 * @param busType     业务类型
	 * @param triggerName 数据解析成功后执行的触发器
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
			JSONArray tables = doAnalysis(unZipDir, jsonFileName);
			excuteTrigger(busType, triggerName, tables);
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
	 * @param appUri      应用唯一标识
	 * @param transFlag   传输业务的标识，用于标记一组批量传输的操作，作用类似于busType
	 * @param triggerName 触发器注入名称，一般为类名首字母小写后的字符串，用于数据传输完成后，在接收端需要执行的一些业务逻辑，触发器类需要在接收端写好，实现ReceiveTrigger接口
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
			excuteTrigger(transFlag, triggerName, tables);
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
	 * @param file    文件对象
	 * @param point   开始写入的位置
	 * @param busType 业务类型
	 * @param appUri  应用唯一标识
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
	 * @param busType 业务类型
	 * @param appUri  应用唯一标识
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
	 * @param appUri  应用唯一标识
	 * @param busType 业务类型
	 * @return 是否
	 */
	public boolean serverHasPullData(String appUri, String busType) {
		long count = pullDataFlagService.findCount(new PullDataFlag(null, appUri, busType, null));
		if (count > 0) {
			return true;
		}
		return false;
	}

	/**
	 * 拉取数据
	 * 
	 * @param appUri   应用唯一标识
	 * @param busType  业务类型
	 * @param request  请求对象
	 * @param response 响应对象
	 */
	public void serverPull(String appUri, String busType, HttpServletRequest request, HttpServletResponse response) {
		List<PullDataFlag> pullDataFlagList = pullDataFlagService.findList(new PullDataFlag(null, appUri, busType, null));
		String tempFileDir = Global.getUserfilesBaseDir(Constant.TemplDir.WEIT_FOR_PULL_TEMP);
		String pullDir = tempFileDir + File.separator + appUri + busType;
		String pullFileName = tempFileDir + File.separator + appUri + busType + ".zip";
		if (pullDataFlagList.size() > 0) {
			FileUtils.zipFiles(pullDir, "*", pullFileName);
			FileUtils.downFile(new File(pullFileName), request, response);
		}
	}

	/**
	 * 清除待拉取的临时文件
	 * 
	 * @param appUri      应用唯一标识
	 * @param busType     业务类型
	 * @param triggerName 拉取数据成功后要执行的触发器注入名称
	 * @return 响应结果
	 */
	public Result serverCleanPullFile(String appUri, String busType, String triggerName) {
		PullDataFlag pullDataFlag = new PullDataFlag(null, appUri, busType, null);
		// 调用这个接口说明拉取已经成功，执行拉取成功的触发器
		excuteTrigger(busType, triggerName, JSON.parseArray(pullDataFlag.getRowsJsonStr()));
		// 删除待拉取的临时文件
		String tempFileDir = Global.getUserfilesBaseDir(Constant.TemplDir.WEIT_FOR_PULL_TEMP);
		String tempFileName = appUri + busType + ".zip";
		if (FileUtils.deleteFile(tempFileDir + File.separator + tempFileName) && FileUtils.deleteQuietly(new File(tempFileDir + File.separator + appUri + busType))) {
			// 删除数据库记录
			List<PullDataFlag> pullDataFlagList = pullDataFlagService.findList(pullDataFlag);
			for (PullDataFlag pullDataFlag2 : pullDataFlagList) {
				pullDataFlagService.delete(pullDataFlag2);
			}
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
			String pullDataFlagId = IdGen.nextId();
			String busType = transEntity.getBusType();
			String tempPath = Global.getUserfilesBaseDir(Constant.TemplDir.WEIT_FOR_PULL_TEMP);
			String busTypeTempPath = tempPath + File.separator + appUri + busType;
			String jsonPath = busTypeTempPath + File.separator + "json";
			String jsonFileName = jsonPath + File.separator + busType + ".json";
			String zipName = busTypeTempPath + File.separator + pullDataFlagId + "_" + busType + ".zip";
			JSONArray tables = new JSONArray();
			JSONObject json = jsonTableBuilder4Push(transEntity, fileList, list);
			// 统一变成JSONArray，拉取的时候好处理
			tables.add(json);
			System.out.println(tables);
			FileUtils.createDirectory(tempPath);
			buildZip(transEntity.getExtraFileList(), busTypeTempPath, jsonPath, jsonFileName, zipName, fileList, tables.toJSONString());
			// 记录待拉取的标识
			PullDataFlag entity = new PullDataFlag(pullDataFlagId, appUri, transEntity.getBusType(), tables.toJSONString());
			entity.setIsNewRecord(true);
			pullDataFlagService.save(entity);
			FileUtils.deleteQuietly(new File(jsonPath));
			return new Result(true, Constant.Message.推送成功);
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, Constant.Message.推送失败);
		}
	}

	@Override
	public Result serverPushBatch(String appUri, String transFlag) {
		try {
			String pullDataFlagId = IdGen.nextId();
			String tempPath = Global.getUserfilesBaseDir(Constant.TemplDir.WEIT_FOR_PULL_TEMP);
			String busTypeTempPath = tempPath + File.separator + appUri + transFlag;
			String jsonPath = busTypeTempPath + File.separator + "json";
			String jsonFileName = jsonPath + File.separator + transFlag + ".json";
			String zipName = busTypeTempPath + File.separator + pullDataFlagId + "_" + transFlag + ".zip";
			System.out.println(this.pushTables);
			FileUtils.createDirectory(tempPath);
			buildZip(this.pushExtraFileList, busTypeTempPath, jsonPath, jsonFileName, zipName, this.pushFileList, this.pushTables.toJSONString());
			// 记录待拉取的标识
			PullDataFlag entity = new PullDataFlag(pullDataFlagId, appUri, transFlag, this.pushTables.toJSONString());
			entity.setIsNewRecord(true);
			pullDataFlagService.save(entity);
			FileUtils.deleteQuietly(new File(jsonPath));
			return new Result(true, Constant.Message.推送成功);
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, Constant.Message.推送失败);
		} finally {
			// 情况临时变量
			cleanPushBatch();
		}
	}

	/*
	 * 重新传输数据
	 */
	private <T extends DataEntity<?>> Result sendData(TransEntity<T> transEntity, Client client) {
		String busType = transEntity.getBusType();
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
			JSONObject json = jsonTableBuilder(transEntity, fileList, transEntity.getList());
			System.out.println(json);
			buildZip(transEntity.getExtraFileList(), tempPath, jsonPath, jsonFileName, zipName, fileList, json.toJSONString());
			// 将zip文件拆分成若干小块
			List<TempFile> tempFileList = fileHandler.splitFile(zipName, busType);
			// 发送文件
			transData(client, tempPath, tempFileList);
			// 调用对方的解析文件接口
			result = analysisData(busType, client, transEntity.getTriggerName());
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
//		ZipParameters zipParameters = new ZipParameters();
//		zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
//		zipParameters.setCompressionLevel(CompressionLevel.NORMAL);
//		zipParameters.setEncryptFiles(true);
//		zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

//		ZipFile zipFile = new ZipFile(zipName, "tq26556570".toCharArray());
//		File jsonPathFile = new File(jsonPath);
//		try {
//			zipFile.addFolder(jsonPathFile, zipParameters);
//		} catch (ZipException e) {
//			e.printStackTrace();
//		}
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
				FileUtils.copyFile(Global.getUserfilesBaseDir(extraFile.getPath()) + File.separator + extraFile.getFileName(), extraFilesTempDir + File.separator + extraFile.getPath() + File.separator + extraFile.getFileName());
			});
		}
	}

	/*
	 * 断点续传
	 */
	private <T extends DataEntity<?>> Result renewal(Client client, String busType, String triggerName, boolean isMulti) {
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
			if (isMulti) {// 是否批量传输
				if (analysisMultiData(busType, client, triggerName).isSuccess()) {
					return new Result(true, Constant.Message.传输成功);
				}
			} else {
				if (analysisData(busType, client, triggerName).isSuccess()) {
					return new Result(true, Constant.Message.传输成功);
				}
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
	private <T extends DataEntity<?>> JSONObject jsonTableBuilder(TransEntity<T> transEntity, List<File> fileList, List<T> list) throws Exception {
		JSONObject table = new JSONObject();
		if (list != null) {
			JSONArray rows = new JSONArray();
			String tableName = "";
			if (StringUtils.isNotBlank(transEntity.getToTableName())) {
				tableName = transEntity.getToTableName();
			} else if (transEntity.getEntityType().isAnnotationPresent(SendTable.class)) {
				tableName = transEntity.getEntityType().getAnnotation(SendTable.class).to();
			} else {
				tableName = transEntity.getEntityType().getAnnotation(Table.class).name();
			}
			table.put("table", tableName);
			for (DataEntity<?> entity : list) {
				rows.add(jsonRowBuilder(transEntity, fileList, entity));
			}
			table.put("rows", rows);
		}

		// 额外传输字符串
		if (StringUtils.isNotBlank(transEntity.getExtraStr())) {
			table.put("extraStr", transEntity.getExtraStr());
		}

		return table;
	}

	/*
	 * 解析集合，生成推送json
	 */
	private <T extends DataEntity<?>> JSONObject jsonTableBuilder4Push(TransEntity<T> transEntity, List<File> fileList, List<T> list) throws Exception {
		JSONObject table = new JSONObject();
		if (list != null) {
			JSONArray rows = new JSONArray();
			String tableName = "";
			if (StringUtils.isNotBlank(transEntity.getToTableName())) {
				tableName = transEntity.getToTableName();
			} else if (transEntity.getEntityType().isAnnotationPresent(PushTable.class)) {
				tableName = transEntity.getEntityType().getAnnotation(PushTable.class).to();
			} else {
				tableName = transEntity.getEntityType().getAnnotation(Table.class).name();
			}
			table.put("table", tableName);
			for (DataEntity<?> entity : list) {
				rows.add(jsonRowBuilder4Push(transEntity, fileList, entity));
			}
			table.put("rows", rows);
		}

		// 额外传输字符串
		if (StringUtils.isNotBlank(transEntity.getExtraStr())) {
			table.put("extraStr", transEntity.getExtraStr());
		}

		return table;
	}

	/*
	 * 解析实体，生成报送json
	 */
	private <T extends DataEntity<?>> JSONObject jsonRowBuilder(TransEntity<T> transEntity, List<File> fileList, DataEntity<?> entity) throws Exception {
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
			buildSendRow(entity, rowData, css, pkList, field);
		}

		// 拼接主键信息
		JSONObject idJson = buildIdJson(entity, rowData, css, pkList);

		// 系统五个默认字段
		sysColumnHandle(entity, transEntity.isRequireSysColumn(), transEntity.getRequireSysColumnArr(), rowData);

		// 系统的树结构字段，继承TreeEntity才有
		if (transEntity.isRequireTreeColumn()) {
			sysTreeColumnHandle(entity, rowData);
		}

		row.put("id", idJson);
		row.put("rowData", rowData);

		// 获取附件信息
		// 联合主键的情况意味着id为空，框架的附件机制并不支持联合主键，所以如果id为空则不考虑附件的处理
		if (transEntity.isRequireAttachment() && entity.getId() != null) {
			fileUploadHandle(entity, fileList, row);
		}

		return row;
	}

	/*
	 * 解析实体，生成报送json
	 */
	private <T extends DataEntity<?>> JSONObject jsonRowBuilder4Push(TransEntity<T> transEntity, List<File> fileList, DataEntity<?> entity) throws Exception {
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
			buildPushRow(entity, rowData, css, pkList, field);
		}

		// 拼接主键信息
		JSONObject idJson = buildIdJson(entity, rowData, css, pkList);

		// 系统五个默认字段
		sysColumnHandle(entity, transEntity.isRequireSysColumn(), transEntity.getRequireSysColumnArr(), rowData);

		// 系统的树结构字段，继承TreeEntity才有
		if (transEntity.isRequireTreeColumn()) {
			sysTreeColumnHandle(entity, rowData);
		}

		row.put("id", idJson);
		row.put("rowData", rowData);

		// 获取附件信息
		// 联合主键的情况意味着id为空，框架的附件机制并不支持联合主键，所以如果id为空则不考虑附件的处理
		if (transEntity.isRequireAttachment() && entity.getId() != null) {
			fileUploadHandle(entity, fileList, row);
		}

		return row;
	}

	@SuppressWarnings("rawtypes")
	private <T extends DataEntity<?>> void buildSendRow(T entity, JSONArray rowData, Class<? extends DataEntity> css, List<Field> pkList, Field field) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
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

	@SuppressWarnings("rawtypes")
	private <T extends DataEntity<?>> JSONObject buildIdJson(T entity, JSONArray rowData, Class<? extends DataEntity> css, List<Field> pkList) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
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
		return idJson;
	}

	private <T extends DataEntity<?>> void sysColumnHandle(T entity, boolean requireSysColumn, String[] requireSysColumnArr, JSONArray rowData) {
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
	}

	private <T extends DataEntity<?>> void sysTreeColumnHandle(T entity, JSONArray rowData) {
		@SuppressWarnings("rawtypes")
		TreeEntity treeEntity = (TreeEntity) entity;
		String parentCode = treeEntity.getParentCode();
		String parentCodes = treeEntity.getParentCodes();
		Integer treeSort = treeEntity.getTreeSort();
		String treeSorts = treeEntity.getTreeSorts();
		String treeLeaf = treeEntity.getTreeLeaf();
		Integer treeLevel = treeEntity.getTreeLevel();
		String treeNames = treeEntity.getTreeNames();
		if (StringUtils.isNotBlank(parentCode)) {
			JSONObject parentCodeJson = new JSONObject();
			parentCodeJson.put("to", Constant.SysTreeCoumn.PARENT_CODE);
			parentCodeJson.put("value", parentCode);
			parentCodeJson.put("type", "string");
			rowData.add(parentCodeJson);
		}
		if (StringUtils.isNotBlank(parentCodes)) {
			JSONObject parentCodesJson = new JSONObject();
			parentCodesJson.put("to", Constant.SysTreeCoumn.PARENT_CODES);
			parentCodesJson.put("value", parentCodes);
			parentCodesJson.put("type", "string");
			rowData.add(parentCodesJson);
		}
		if (treeSort != null) {
			JSONObject treeSortJson = new JSONObject();
			treeSortJson.put("to", Constant.SysTreeCoumn.TREE_SORT);
			treeSortJson.put("value", treeSort);
			treeSortJson.put("type", "number");
			rowData.add(treeSortJson);
		}
		if (StringUtils.isNotBlank(treeSorts)) {
			JSONObject treeSortsJson = new JSONObject();
			treeSortsJson.put("to", Constant.SysTreeCoumn.TREE_SORTS);
			treeSortsJson.put("value", treeSorts);
			treeSortsJson.put("type", "string");
			rowData.add(treeSortsJson);
		}
		if (StringUtils.isNotBlank(treeLeaf)) {
			JSONObject treeLeafJson = new JSONObject();
			treeLeafJson.put("to", Constant.SysTreeCoumn.TREE_LEAF);
			treeLeafJson.put("value", treeLeaf);
			treeLeafJson.put("type", "string");
			rowData.add(treeLeafJson);
		}
		if (treeLevel != null) {
			JSONObject treeLevelJson = new JSONObject();
			treeLevelJson.put("to", Constant.SysTreeCoumn.TREE_LEVEL);
			treeLevelJson.put("value", treeLevel);
			treeLevelJson.put("type", "number");
			rowData.add(treeLevelJson);
		}
		if (StringUtils.isNotBlank(treeNames)) {
			JSONObject treeNamesJson = new JSONObject();
			treeNamesJson.put("to", Constant.SysTreeCoumn.TREE_NAMES);
			treeNamesJson.put("value", treeNames);
			treeNamesJson.put("type", "string");
			rowData.add(treeNamesJson);
		}
	}

	private <T extends DataEntity<?>> void fileUploadHandle(T entity, List<File> fileList, JSONObject row) {
		FileUpload fileUpload = new FileUpload();
		fileUpload.setBizKey(entity.getId());
		List<FileUpload> fileUploadList = fileUploadService.findList(fileUpload);
		if (fileUploadList.size() > 0) {
			JSONArray fileJsonArr = new JSONArray();
			for (FileUpload fileUploadEntity : fileUploadList) {
				JSONObject json = JSON.parseObject(JsonMapper.toJson(fileUploadEntity));
				fileList.add(new File(Global.getUserfilesBaseDir("fileupload") + File.separator + fileUploadEntity.getFileEntity().getFilePath() + fileUploadEntity.getFileEntity().getFileId() + "." + fileUploadEntity.getFileEntity().getFileExtension()));
				SendFile sendFile = new SendFile();
				sendFile.setPath(fileUploadEntity.getFileEntity().getFilePath());
				sendFile.setFileName(fileUploadEntity.getFileEntity().getFileId() + "." + fileUploadEntity.getFileEntity().getFileExtension());
				json.put("fileInfo", JSON.parseObject(JsonMapper.toJson(sendFile)));
				fileJsonArr.add(json);
			}
			row.put("fileList", fileJsonArr);
		}
	}

	@SuppressWarnings("rawtypes")
	private <T extends DataEntity<?>> void buildPushRow(T entity, JSONArray rowData, Class<? extends DataEntity> css, List<Field> pkList, Field field) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
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
		return client.hasPullData(busType).isSuccess();
	}

	/*
	 * 清除推送的临时文件
	 */
	private boolean cleanPushTempFile(String busType, String triggerName, Client client) {
		return client.cleanPushTempFile(busType, triggerName).isSuccess();
	}

	/*
	 * 解析数据
	 */
	private JSONArray doAnalysis(String unZipDir, String jsonFileName) throws Exception {
		// 读取json数据
		String aesStr = FileUtils.readFileToString(new File(unZipDir + File.separator + jsonFileName), "UTF-8");
		String jsonStr = AesUtils.decode(aesStr, Constant.FILE_KEY);
		System.out.println(jsonStr);
		JSONObject table = JSON.parseObject(jsonStr);
		// 数据库表名
		String tableName = table.getString("table");
		if (StringUtils.isNotBlank(tableName)) {
			// 行数据json
			JSONArray rows = table.getJSONArray("rows");
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

		// 返回表json，为了与批量处理统一，所以用JSONArray
		JSONArray tables = new JSONArray();
		tables.add(table);
		return tables;
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
		JSONArray tables = JSON.parseArray(jsonStr);
		for (Object object : tables) {
			JSONObject jsonObj = JSON.parseObject(object.toString());
			// 数据库表名
			String tableName = jsonObj.getString("table");
			if (StringUtils.isNotBlank(tableName)) {
				// 行数据json
				JSONArray rows = jsonObj.getJSONArray("rows");
				// 数据库数据和附件文件
				this.dataBaseHandler.setData(tableName, rows, unZipDir);
			}
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

		return tables;
	}

	/*
	 * 清除临时变量
	 */
	private void cleanSendBatch() {
		this.sendFileList = null;
		this.sendExtraFileList = null;
		this.sendTables = null;
	}

	private void cleanPushBatch() {
		this.pushFileList = null;
		this.pushExtraFileList = null;
		this.pushTables = null;
	}

	/*
	 * 执行触发器
	 */
	private void excuteTrigger(String busType, String triggerName, JSONArray tables) {
		if (StringUtils.isNotBlank(triggerName) && !triggerName.equals(Constant.HAS_NO_TRIGGER)) {
			// 执行触发器
			@SuppressWarnings("static-access")
			ReceiveTrigger trigger = (ReceiveTrigger) springContextsUtil.getBean(triggerName);
			trigger.run(tables, busType);
		}
	}

	/*
	 * 添加数据到批量处理中
	 */
	private <T extends DataEntity<?>> void addTransBatch(TransEntity<T> transEntity, boolean isPush) throws Exception {
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

		// 生成json数据
		JSONObject table = null;
		// 判断是否推送
		if (isPush) {// 推送
			// 初始化
			if (this.pushFileList == null) {
				this.pushFileList = ListUtils.newArrayList();
			}
			if (this.pushTables == null) {
				this.pushTables = new JSONArray();
			}
			if (this.pushExtraFileList == null) {
				this.pushExtraFileList = ListUtils.newArrayList();
			}
			table = jsonTableBuilder4Push(transEntity, this.pushFileList, list);
			// 加入批处理列表中
			this.pushTables.add(table);
			if (extraFileList != null) {
				this.pushExtraFileList.addAll(extraFileList);
			}
		} else {// 发送
			// 初始化
			if (this.sendFileList == null) {
				this.sendFileList = ListUtils.newArrayList();
			}
			if (this.sendTables == null) {
				this.sendTables = new JSONArray();
			}
			if (this.sendExtraFileList == null) {
				this.sendExtraFileList = ListUtils.newArrayList();
			}
			table = jsonTableBuilder(transEntity, this.sendFileList, list);
			// 加入批处理列表中
			this.sendTables.add(table);
			if (extraFileList != null) {
				this.sendExtraFileList.addAll(extraFileList);
			}
		}

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
		 * @param client   请求配置对象
		 * @param tempFile 临时碎片文件信息对象
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

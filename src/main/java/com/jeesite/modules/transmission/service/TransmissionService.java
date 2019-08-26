package com.jeesite.modules.transmission.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.multipart.MultipartFile;

import com.jeesite.common.entity.DataEntity;
import com.jeesite.modules.transmission.entity.ExtraFile;
import com.jeesite.modules.transmission.entity.Result;
import com.jeesite.modules.transmission.entity.TransEntity;

/**
 * 数据传输接口
 * 
 * @author 彭嘉辉
 *
 */
public interface TransmissionService {
	/**
	 * 发送多个对象数据，地址默认读取参数设置中的send.url参数的值
	 * 
	 * @param list
	 *            需要报送的实体对象列表
	 * @param entityType
	 *            实体类
	 * @param busType
	 *            业务类型，用于记录这次数据传输是哪个业务，随意定义
	 * @param renewal
	 *            是否断点续传
	 * @param requireSysColumn
	 *            是否报送系统默认的5列(create_date.....)
	 * @return 结果
	 */
	<T extends DataEntity<?>> Result clientSend(List<T> list, Class<T> entityType, String busType, boolean renewal, boolean requireSysColumn);

	/**
	 * 发送单个对象数据，地址默认读取参数设置中的send.url参数的值
	 * 
	 * @param entity
	 *            需要报送的实体对象
	 * @param entityType
	 *            实体类
	 * @param busType
	 *            业务类型，用于记录这次数据传输是哪个业务，随意定义
	 * @param renewal
	 *            是否断点续传
	 * @param requireSysColumn
	 *            是否报送系统默认的5列(create_date.....)
	 * @return 结果
	 */
	<T extends DataEntity<?>> Result clientSend(T entity, Class<T> entityType, String busType, boolean renewal, boolean requireSysColumn);

	/**
	 * 发送多个对象数据，地址为传入参数url的值
	 * 
	 * @param list
	 *            需要报送的实体对象列表
	 * @param entityType
	 *            实体类
	 * @param url
	 *            发送目标地址
	 * @param busType
	 *            业务类型，用于记录这次数据传输是哪个业务，随意定义
	 * @param renewal
	 *            是否断点续传
	 * @param requireSysColumn
	 *            是否报送系统默认的5列(create_date.....)
	 * @return 结果
	 */
	<T extends DataEntity<?>> Result clientSend(List<T> list, Class<T> entityType, String url, String busType, boolean renewal, boolean requireSysColumn);

	/**
	 * 发送单个对象数据，地址为传入参数url的值
	 * 
	 * @param entity
	 *            需要报送的实体对象
	 * @param entityType
	 *            实体类
	 * @param url
	 *            发送目标地址
	 * @param busType
	 *            业务类型，用于记录这次数据传输是哪个业务，随意定义
	 * @param renewal
	 *            是否断点续传
	 * @param requireSysColumn
	 *            是否报送系统默认的5列(create_date.....)
	 * @return 响应结果
	 */
	<T extends DataEntity<?>> Result clientSend(T entity, Class<T> entityType, String url, String busType, boolean renewal, boolean requireSysColumn);

	/**
	 * 发送多个对象数据，有需要额外传输的文件，这些文件不存在于附件中，比如跳过系统上传组件自动生成的文件，地址默认读取参数设置中的send.url参数的值
	 * 
	 * @param list
	 *            需要报送的实体对象列表
	 * @param entityType
	 *            实体类
	 * @param busType
	 *            业务类型，用于记录这次数据传输是哪个业务，随意定义
	 * @param renewal
	 *            是否断点续传
	 * @param requireSysColumn
	 *            是否报送系统默认的5列(create_date.....)
	 * @param extraFileList
	 *            额外要传输的文件列表
	 * @return 结果
	 */
	<T extends DataEntity<?>> Result clientSend(List<T> list, Class<T> entityType, String busType, boolean renewal, boolean requireSysColumn, List<ExtraFile> extraFileList);

	/**
	 * 发送单个对象数据，有需要额外传输的文件，这些文件不存在于附件中，比如跳过系统上传组件自动生成的文件，地址默认读取参数设置中的send.url参数的值
	 * 
	 * @param entity
	 *            需要报送的实体对象
	 * @param entityType
	 *            实体类
	 * @param busType
	 *            业务类型，用于记录这次数据传输是哪个业务，随意定义
	 * @param renewal
	 *            是否断点续传
	 * @param requireSysColumn
	 *            是否报送系统默认的5列(create_date.....)
	 * @param extraFileList
	 *            额外要传输的文件列表
	 * @return 结果
	 */
	<T extends DataEntity<?>> Result clientSend(T entity, Class<T> entityType, String busType, boolean renewal, boolean requireSysColumn, List<ExtraFile> extraFileList);

	/**
	 * 发送多个对象数据，有需要额外传输的文件，这些文件不存在于附件中，比如跳过系统上传组件自动生成的文件，地址为传入参数url的值
	 * 
	 * @param list
	 *            需要报送的实体对象列表
	 * @param entityType
	 *            实体类
	 * @param url
	 *            发送目标地址
	 * @param busType
	 *            业务类型，用于记录这次数据传输是哪个业务，随意定义
	 * @param renewal
	 *            是否断点续传
	 * @param requireSysColumn
	 *            是否报送系统默认的5列(create_date.....)
	 * @param extraFileList
	 *            额外要传输的文件列表
	 * @return 结果
	 */
	<T extends DataEntity<?>> Result clientSend(List<T> list, Class<T> entityType, String url, String busType, boolean renewal, boolean requireSysColumn, List<ExtraFile> extraFileList);

	/**
	 * 发送单个对象数据，有需要额外传输的文件，这些文件不存在于附件中，比如跳过系统上传组件自动生成的文件，地址为传入参数url的值
	 * 
	 * @param entity
	 *            需要报送的实体对象
	 * @param entityType
	 *            实体类
	 * @param url
	 *            发送目标地址
	 * @param busType
	 *            业务类型，用于记录这次数据传输是哪个业务，随意定义
	 * @param renewal
	 *            是否断点续传
	 * @param requireSysColumn
	 *            是否报送系统默认的5列(create_date.....)
	 * @param extraFileList
	 *            额外要传输的文件列表
	 * @return 响应结果
	 */
	<T extends DataEntity<?>> Result clientSend(T entity, Class<T> entityType, String url, String busType, boolean renewal, boolean requireSysColumn, List<ExtraFile> extraFileList);

	/**
	 * 数据传输
	 * 
	 * @param transEntity
	 *            传输接口参数对象
	 * @return 结果
	 */
	<T extends DataEntity<?>> Result clientSend(TransEntity<T> transEntity);

	/**
	 * 添加传输数据到批处理列表中，准备批量传输，在线传输和离线传输都用这个接口做批量处理
	 * 
	 * @param transEntity
	 *            传输接口参数对象
	 * @throws Exception
	 */
	<T extends DataEntity<?>> void addTransBatch(TransEntity<T> transEntity) throws Exception;

	/**
	 * 执行批量传输
	 * 
	 * @param transFlag
	 *            传输业务的标识，用于标记一组批量传输的操作，作用类似于busType
	 * @param url
	 *            接收方的地址，如192.168.6.1:8080/sbos，如果为空默认读取参数配的send.url的值
	 * @param triggerName
	 *            触发器注入名称，一般为类名首字母小写后的字符串，用于数据传输完成后，在接收端需要执行的一些业务逻辑，触发器类需要在接收端写好，实现ReceiveTrigger接口
	 * 
	 * @return 结果
	 */
	Result clientSendBatch(String transFlag, String url, String triggerName);

	/**
	 * 检测当前业务类型是否存在可断点续传的数据
	 * 
	 * @param busType
	 *            业务类型
	 * @return 是否
	 */
	boolean clientHasRenewal(String busType);

	/**
	 * 向服务器拉取数据
	 * 
	 * @param busType
	 *            应用类型
	 * @param triggerName
	 *            拉取数据成功后要执行的触发器注入名称
	 * @return 结果
	 */
	Result clientPull(String busType, String triggerName);

	/**
	 * 离线导出数据
	 * 
	 * @param transEntity
	 *            传输参数对象
	 * @param exportFileName
	 *            导出的文件名，如果空的话会以传入的业务类型作为文件名
	 * @param request
	 *            请求对象
	 * @param response
	 *            响应对象
	 */
	<T extends DataEntity<?>> void export(TransEntity<T> transEntity, String exportFileName, HttpServletRequest request, HttpServletResponse response);

	/**
	 * 导出批量传输数据
	 * 
	 * @param transFlag
	 *            传输业务的标识，用于标记一组批量传输的操作，作用类似于busType
	 * @param exportFileName
	 *            导出的文件名，如果空的话会以传入的transFlag作为文件名
	 * @param request
	 *            请求对象
	 * @param response
	 *            响应对象
	 */
	void exportBatch(String transFlag, String exportFileName, HttpServletRequest request, HttpServletResponse response);

	/**
	 * 导入离线传输数据
	 * 
	 * @param file
	 *            上传的文件
	 * @param busType
	 *            业务类型，要与导出端保持一直
	 * @return 结果
	 */
	Result importData(MultipartFile file, String busType);

	/**
	 * 导入批量传输数据
	 * 
	 * @param file
	 *            上传的文件
	 * @param transFlag
	 *            传输业务的标识，用于标记一组批量传输的操作，作用类似于busType
	 * @return 结果
	 */
	Result importDataBatch(MultipartFile file, String transFlag);

	/**
	 * 推送数据
	 * 
	 * @param appUri
	 *            接收方应用唯一标识
	 * @param transEntity
	 *            传输接口参数对象
	 * @return 结果
	 */
	<T extends DataEntity<?>> Result serverPush(String appUri, TransEntity<T> transEntity);
}

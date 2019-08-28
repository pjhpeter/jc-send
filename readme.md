# 前言  
这个接口是基于jeesite的，不懂jeesite的就不要往下看了，先劝退一波  
设计场景是这样的，一个中心端，多个单位端，还可以有一些特殊端，比如像军工项目有个叫服务窗口端的，单位端和特殊端可以向中心端发送数据。如果中心端的数据要到单位端或特殊端，是由单位端或特殊端来从中心端拉取数据，总之中心端永远不会主动向单位端和特殊端推送数据的，因为他最牛的┗|｀O′|┛ 嗷~~  
# 1.导包  
```  
<!-- 报送模块 -->
<dependency>
    <groupId>com.jeesite</groupId>
    <artifactId>jc-send</artifactId>
    <version>${project.parent.version}</version>
</dependency>
```  
# 2.建表  
```
-- 记录数据传输的碎片文件信息，用于断点续传
CREATE TABLE trans_temp_file (
	id VARCHAR(64) PRIMARY KEY,
	bus_type varchar(100),-- 业务类型，用于记录这次数据传输是哪个业务，随意定义
	path VARCHAR(100),-- 临时碎片文件存放绝对路径
	file_name VARCHAR(100),-- 拆分前的文件名
	pice_file_name VARCHAR(100),-- 临时碎片文件名
	point BIGINT-- 文件写入起始偏移量
);

-- 有可被拉取数据的标识
CREATE TABLE trans_pull_data_flag(
	id VARCHAR(64) PRIMARY KEY,
	app_uri VARCHAR(50),-- 应用唯一标识
	bus_type VARCHAR(100),-- 业务类型
	rows_json_str text-- 待拉取的数据
);
```  
也可以在jc-send.jar里面找到tables.sql执行  

# 3.参数设置
在系统管理的参数设置里面配置两个参数：  
## send.url  
发送数据接收放的地址，如192.168.6.1:8080/sbos  
## appUri
该系统的唯一标识，随便一个字符串，但是不能跟其他系统重复，这个参数除了在参数配置外，还需要在中心端和单位端的单位信息表添加这个字段，单位端要报给中心端保存好。如果是特殊端的话，在中心端的参数配置里面也要添加配置特殊段的appUri，参数名随便起，找得到就行  

# 4.写代码
## 报送的话
### a.在entity那里加注解  
```
@SendTable(to="sbos_unit_secpost_check_haha")//当这边的表名和那边的表名不同的时候添加这个注解，如果一样就不用了
@Table(name="sbos_unit_secpost_check", alias="a", columns={
		@Column(name="id", attrName="id", label="id", isPK=true),
		@Column(name="unit_code", attrName="unitCode", label="单位id", comment="单位id（unit）"),
		@Column(name="year", attrName="year", label="年份", comment="年份"),
		@Column(name="writing_date", attrName="writingDate", label="填报日期", comment="填报日期(申请日期)"),
		@Column(name="confirm_date", attrName="confirmDate", label="确认日期"),
		@Column(name="writing_user", attrName="writingUser", label="填报人",queryType=QueryType.LIKE),
		@Column(name="is_sure", attrName="isSure", label="确认状态", comment="确认状态（0未报送，1审核中，2已通过 3不通过  ）"),
		@Column(name="nopass_reason", attrName="nopassReason", label="不通过的理由")
},joinTable={
		@JoinTable(type=Type.LEFT_JOIN, entity=Unit.class, alias="b",
				on="b.unit_code = a.unit_code",
				columns={
						@Column(name="unit_name", label="区域名称"),
						@Column(name="unit_cellphone", label="联系电话")
			}),
}, extColumnKeys="extColumn",orderBy="a.year DESC"
)
public class UnitSecpostCheck extends DataEntity<UnitSecpostCheck> {
	private static final long serialVersionUID = 1L;
	@SendField(isPK = true)//在要报送的成员变量上面添加这个注解，主键的话要加上isPK参数，支持联合主键，如果没有一个变量有isPK，默认把id作为主键
	private String unitCode;		
	@SendField(isPK = true)
	private Date writingDate;		
	@SendField(isPK = true)
	private Date confirmDate;
	@SendField(to = "current_user")//当接收端和推送端对应的数据库字段命名不一致时可以加上这个to
	private String writingUser;		
	@SendField
	private Integer isSure;		
	private String nopassReason;		
	private Integer year;   
	private Unit unit;   
	private String secPostCount;
}
```  
### b.写报送的代码
```
List<SbosMemberSecbook> list = secbookService.findList(new SbosMemberSecbook());
// 基本参数
TransEntity<SbosMemberSecbook> transEntity = new TransEntity<>();
transEntity.setList(list);
transEntity.setEntityType(SbosMemberSecbook.class);
transEntity.setBusType("SbosMemberSecbook");

// 有额外要传输的文件，比如自己生成的通知书等，可以这样加
List<ExtraFile> extraFileList = ListUtils.newArrayList();
extraFileList.add(new ExtraFile("temp", "11-27疫苗全称追溯平台方案(1).pdf"));
extraFileList.add(new ExtraFile("temp", "20180105金立手机官网设设计提案.pdf"));
extraFileList.add(new ExtraFile("temp", "20180727福特STA数字化平台-视觉风格提案.pdf"));
transEntity.setExtraFileList(extraFileList);

// 如果报送完数据之后，需要在接收端执行一些业务代码的话可以这样，当然前提是接收端那边有一个叫TestTrigger（这个名字随便起的，不要到时满大街的TestTrigger哟(°ー°〃)）的触发器哦，不然就搞笑咧^_^
transEntity.setTriggerName("testTrigger");//这里给的是触发器的spring容器里的id，注解注入的话一般默认类名首字母小写
		
if(transmissionService.clientHasRenewal(busType)) {// 判断是否可以断点续传
	transEntity.setRenewal(true);
}

// 调用报送接口
Result result = transmissionService.clientSend(transEntity);
System.out.println(result);
```  
触发器TestTrigger的写法  
![image](/uploads/eb0f0d9e60d60b2350484cda75848804/image.png)
```
@Component
public class TestTrigger implements ReceiveTrigger {

	/**
	 * rows 报送过来的所有数据
	 * busType 业务类型
	 */
	@Override
	public void run(JSONArray rows, String busType) {
		System.out.println("这里是业务代码，哈哈哈");
		System.out.println(rows);
		System.out.println(busType);
	}

}
```  
报送就这样写完  
-----------------------------------------------------------我是华丽的分割线------------------------------------------------------------------------  
## 推送的话  
### a.推送端的entity添加注解  
```
@PushTable(to = "sbos_unit_secpost_determine_book_haha") // 当这边的表名和那边的表名不同的时候添加这个注解，如果一样就不用了
@Table(name = "sbos_unit_secpost_determine_book", alias = "a", columns = { @Column(name = "id", attrName = "id", label = "id", isPK = true),
		@Column(name = "unit_secpost_check_id", attrName = "unitSecpostCheckId", label = "岗位审核表id"), @Column(name = "unit_secpost_item_id", attrName = "unitSecpostItemId", label = "涉密岗位备案id"),
		@Column(name = "unit_code", attrName = "unitCode", label = "单位编号"), @Column(name = "year", attrName = "year", label = "年份"),
		@Column(name = "designation", attrName = "designation", label = "文号"), @Column(name = "post_sure_date", attrName = "postSureDate", label = "涉密岗位确定时间"),
		@Column(name = "post_surebook_url", attrName = "postSurebookUrl", label = "电子版涉密岗位确认书路径"), @Column(name = "template_content", attrName = "templateContent", label = "模板内容"), }, joinTable = {
				@JoinTable(type = Type.LEFT_JOIN, entity = Unit.class, alias = "b", on = "b.unit_code = a.unit_code", columns = { @Column(includeEntity = Unit.class) }), }, orderBy = "a.id DESC")
public class UnitSecpostDetermineBook extends DataEntity<UnitSecpostDetermineBook> {
	private static final long serialVersionUID = 1L;
	@PushField(isPK = true)//在要推送的成员变量上面添加这个注解，主键的话要加上isPK参数，支持联合主键，如果没有一个变量有isPK，默认把id作为主键
	private String unitSecpostCheckId;
	@PushField(isPK = true)
	private String unitSecpostItemId;
	@PushField
	private Integer year;
	@PushField(to = "unit_id")//当接收端和推送端对应的数据库字段命名不一致时可以加上这个to
	private String unitCode;
	@PushField
	private String designation;
	@PushField
	private Date postSureDate;
	@PushField
	private String postSurebookUrl;
	@PushField
	private String templateContent;
	private Unit unit;
}
```  
### b.推送端写推送代码  
```
UnitSecpostDetermineBook unitSecpostDetermineBook = new UnitSecpostDetermineBook("1164508036276912128");
UnitSecpostDetermineBook unitSecpostDetermineBook2 = unitSecpostDetermineBookService.get(unitSecpostDetermineBook);

//基本参数
TransEntity<UnitSecpostDetermineBook> transEntity = new TransEntity<>();
transEntity.setBusType("UnitSecpostDetermineBook");
transEntity.setEntity(unitSecpostDetermineBook2);
transEntity.setEntityType(UnitSecpostDetermineBook.class);

//有额外要传输的文件，比如自己生成的通知书等，可以这样加
ExtraFile extraFile = new ExtraFile("template", "中化广东有限公司涉密岗位确认书.docx");//这里第一个参数是额外文件存放的目录，例子中的意思是指向/userfiles/template
List<ExtraFile> extralFileList = ListUtils.newArrayList();
extralFileList.add(extraFile);
transEntity.setExtraFileList(extralFileList);

//推送数据，这时数据还没有真正到达接收方，只是临时存放在待拉取的区域
Result result = transmissionService.serverPush("test_unit", transEntity);
System.out.println(result);
```  
### c.接收端写拉取代码  
```
//一句话，第一个参数是busType，要跟推送端的保持一致，第二个参数就是拉取成功后要在推送端执行触发器，原理跟报送一样哦
Result result = transmissionService.clientPull("UnitSecpostDetermineBook", "testTrigger");
System.out.println(result.toString());//我不是一句话
```  
拉取就这么写完了耶^_^  
----------------------------------------------------------朕是威严的分割线------------------------------------------------------------  
# 还有离线的喔
由于我懒得测试，所以不一定稳定，所以说明也写的很偷懒 ≡ω≡  
## 导出
```
//这是导出后的压缩包文件名，不传默认会用busType做文件名
String exportFileName = "某某某单位报送的数据";
//这里会下载文件哦
transmissionService.export(transEntity, exportFileName, request, response);
```
## 导入
```
//第一个参数是页面上传的文件MultipartFile，第二个是业务类型busType
transmissionService.importData(file, "SbosMemberSecbook");
```
--------------------------------------------------------我是谁？我在哪？在干嘛？-------------------------------------------------------
# 还可以批量传输呢
## 在线的
```
// 添加要批处理对象
transmissionService.addTransBatch(transEntity1);
transmissionService.addTransBatch(transEntity2);

boolean renewal = false;
//transFlag是传输业务的标识，用于标记一组批量传输的操作，作用类似于busType
//url是接收方的地址，如192.168.6.1:8080/sbos，如果为空默认读取参数配的send.url的值，其他应该都懂啦，我就不说了
if (transmissionService.clientHasRenewal(transFlag)) {// 看看有没有断点续传
	renewal = true;
}
Result result = transmissionService.clientSendBatch(transFlag, renewal, null, "testTrigger");
System.out.println(result);
```
## 离线的
```
// 添加要批处理对象
transmissionService.addTransBatch(transEntity1);
transmissionService.addTransBatch(transEntity2);

//transFlag是传输业务的标识，用于标记一组批量传输的操作，作用类似于busType
//exportFileName是导出后的压缩包文件名，不传默认会用busType做文件名
//这里会下载文件哦
transmissionService.exportBatch(transFlag, exportFileName, request, response);
```
# API说明  
想看就看吧 (￣_,￣ )  
## TransEntity
```
/**
 * 传输接口参数实体 参数说明： 
 * 
 * list 需要传输的对象集合，传输多个对象时使用（不能与entity并用）
 * 
 * entity 需要传输的单个对象，传输单个对象时使用（不能与list并用）
 * 
 * entityType 需要传输的对象的实体类型
 * 
 * url 传输接收方的地址，如：192.168.1.1:8080/temp busType 业务类型，该传输业务的唯一标识，自己定义 renewal
 * 断点续传的标识
 * 
 * renewal 是否断点续传，默认false
 * 
 * requireSysColumn 是否需要传输系统的5个字段（status,create_date.....），默认false
 * 
 * requireSysColumnArr 如果系统的5个字段只传输其中一部分的话，在这里设置，如只用了create_date和update_date，{create_date,update_date}
 * 
 * extraFileList 额外要传输的文件列表，有需要额外传输的文件，这些文件不存在于附件中，比如跳过系统上传组件自动生成的文件，需要传如此参数
 * 
 * extraFile 单个额外要传输的文件，有需要额外传输的文件，这些文件不存在于附件中，比如跳过系统上传组件自动生成的文件，需要传如此参数
 * 
 * triggerName 触发器注入名称，一般为类名首字母小写后的字符串，用于数据传输完成后，在接收端需要执行的一些业务逻辑，触发器类需要在接收端写好，实现ReceiveTrigger接口
 * 
 * @author 彭嘉辉
 *
 */
public class TransEntity<T extends DataEntity<?>> implements Serializable {}
```  
## TransmissionService  
```
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
	 * @param renewal
	 *            是否断点续传
	 * @param url
	 *            接收方的地址，如192.168.6.1:8080/sbos，如果为空默认读取参数配的send.url的值
	 * @param triggerName
	 *            触发器注入名称，一般为类名首字母小写后的字符串，用于数据传输完成后，在接收端需要执行的一些业务逻辑，触发器类需要在接收端写好，实现ReceiveTrigger接口
	 * @return 结果
	 */
	Result clientSendBatch(String transFlag, boolean renewal, String url, String triggerName);

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
```  
##### 觉得这个接口还可以的小伙伴点手关注啊，有兴趣了解的小伙伴就把代码check下来看一下吧 ヾ(￣▽￣)Bye~Bye~
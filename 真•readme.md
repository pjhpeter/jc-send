# 前言
写这个接口算是经验累积吧，所以写一份文档分享一下，讲讲解接口是怎么一步一步做出来的，主要聊聊思路方面的东西。里面相关的技术都是很简单的，谁都学得会，但是思路就需要思考和总结了 

# 需求分析
+ 需求分析很重要！需求分析很重要！需求分析很重要！
+ 这个接口的功能是为了在多个不同的项目之间建立一套数据传输的机制，而所有项目运行的环境都是在jeesite框架上，所以接口也使用jeesite框架作为基础，一来可以便利使用jeesite的API，二来跟各个项目可以无缝连接  
+ 一次传输的数据包括数据库的数据，附件等，数据库的数据是序列化的，网络传输序列化数据最通用的就是JSON，而附件等文件传输可以有多种方式，常用的是文件流的方式，或者转成base64字符串后添加到JSON当中传输。如果附件文件较小，添加到JSON当中传是非常便利的，但是文件巨大的情况下，生成JSON的过程会很缓慢，解析的时候需要把整个JSON读到内存中，如果有个附件中有部高清电影，内存直接爆炸哦(⊙﹏⊙)，由于无法确定传输的是什么数据，所以不知道大小，选择文件流的方式传是比较好的  
+ 选用上面结论的方式传输，意味着每次传输可能都是有多个文件的，一个JSON文件，多个附件文件，不确定文件的个数，接收端的代码写起来会比较麻烦，所有就想到把所有文件压缩成一个zip再传，jeesite的FileUtils类也有非常方便的压缩文件方法，这样就永远都是一个文件在传输啦♪(^∇^*)  
+ 由于是涉密系统，传输的数据是需要保护的，不然被人抓包拿去卖就不好了(ˉ▽￣～) 切~~，所以JSON文件内容要加密，压缩包也要加密码。经树根介绍，jeesite有自己的加密方法，就是AesUtils类，真的很方便哦。但是！！！FileUtils类的压缩文件方法居然没有看到可以加密码的参数，我又不想自己再写加密压缩了，所以就这样偷偷不加密压缩包也没有关系吧ㄟ( ▔, ▔ )ㄏ
+ 所有WebAPI都会遇到同样的问题，安全认证，如果别人随便就可以调用你的接口，随便发数据，这样就太恐怖了(☍﹏⁰)，所以要从合法的客户端发过来的请求我们才接，树根推荐的安全认证思路是用户名和密码，网上确实也很多人用，他用的就是system这个用户，但是按照保密规范，应用系统不能存在超级管理员，所以真实环境这个用户是会被禁用的，我还得自己建个隐藏用户，好麻烦，不要！最后选择了令牌方式认证（token），自己随便搞了个tq26556570加密之后认证，虽然很low，但是好用，所以所有请求发过来的时候首先就会进一个拦截器认证一下，通过后再有后续处理，完美(‾◡◝　)
+ 传输的文件如果很大的话，过程很漫长，中间万一网线被老鼠咬断了怎么办o( =•ω•= )m，断点续传当然是常规操作啦  
+ 刘老师说如果单位端没有开机，中心端有审批数据需要下发的时候，下发不了就搞笑了，所以必须要有单位端来拉取数据，中心端永远都不主动给单位端发送东西了，行吧，姜还是老的辣  
+ 树根一直在吐槽杨龙写的socket方式传输很恶心，不稳定，一直让我不要用。我以往的经验是用WebService的，但是又要部署多好WebService服务，所以这两种方式都不用了，看了网上的资料，发现Apache的WebClient好像很厉害的样子，于是数据传输就选择了WebClient，其实WebClient就是模仿浏览器发请求而已，接收请求的时候用普通的controller就行  
+ 说好了做接口，当然是要通用的啦，如果又要做得好用，还得让调用的小伙伴少写代码，不然被举报就惨了。那么数据组装和解析都要做得很智能，这样的话泛型和注解就必不可少了  
经过上面一番思考人生，总算是把接口的雏形想明白了，这样就成功了一半啦！其实一行代码都还没有写ヽ(✿ﾟ▽ﾟ)ノ 

# 开始写代码啦
## 首先从发送数据开始吧
### 先拼装数据
这里让小伙伴把数据先查出来传给我，我帮忙转换成JSON，顺便把附件读出来放一起就可以啦。但是我不知道大家会给我什么对象，也不知道大家要传输的是那些字段的数据，所以这里就需要小伙伴们把要传输的字段给我标出来，然后我用反射来读那些字段的值，这里需要声明注解，还要考虑字段名不一致的问题  
```
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
```
万一接收那边的表名也不一致就坑了，所以又有一个注解  
```
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
```
想象中被调用的代码是这样的
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
那么就开始吧，主要的处理都在**TransmissionService**接口的实现类**TransmissionServiceImpl**里面。

这里用到了一些Java的API，有了思路后，这些API都可以网上查的
```
// 判断类头上是否标注了注解
Class.isAnnotationPresent(注解的class);

// 读取类的注解
Class.getAnnotation(注解的class).属性;

// 获取实体的所有成员变量
entity.getClass().getDeclaredFields();

// 判断成员变量头上是否标注了注解
field.isAnnotationPresent(注解的class);

// 读取成员变量头上的注解
field.getAnnotation(注解的class).属性;
```
考虑到jeesite的数据库操作API都是有每个业务自己写dao来实现的，所以要做通用的还不能用jeesite的API来插数据，只能自己拼SQL，用原生jdbc的API来执行了，插入数据时，字符串、数字、日期、布尔值等，写法是不一样的，所以要根据不同的数据类型来处理，拼成自己能解析的JSON，发过去之后自己解析再拼SQL，通过多番测试发现主键还要特殊处理，有可能还有联合主键，最后就成了下面的代码了
```
// 反射执行get方法
PropertyDescriptor pd = new PropertyDescriptor(field.getName(), Class);
Method readMethod = pd.getReadMethod();// 拿到成员变量的get方法
Class<?> type = field.getType();// 成员变量的数据类型
Object obj = readMethod.invoke(entity);// 执行，返回是个Object需要根据不同的类型转换
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
```
每一行的数据都有可能有自己的附件，jeesite通过bizKey来关联，所以......
```
// 获取附件信息
// 联合主键的情况意味着id为空，框架的附件机制并不支持联合主键，所以如果id为空则不考虑附件的处理
if (entity.getId() != null) {
	FileUpload fileUpload = new FileUpload();
	fileUpload.setBizKey(entity.getId());
	List<FileUpload> fileUploadList = fileUploadService.findList(fileUpload);
	if (fileUploadList.size() > 0) {
		JSONArray fileJsonArr = new JSONArray();
		for (FileUpload fileUploadEntity : fileUploadList) {
			JSONObject json = JSON.parseObject(JsonMapper.toJson(fileUploadEntity));
			fileList.add(new File(Global.getUserfilesBaseDir("fileupload") + File.separator + fileUploadEntity.getFileEntity().getFilePath() + fileUploadEntity.getFileEntity().getFileId()
					+ "." + fileUploadEntity.getFileEntity().getFileExtension()));
			SendFile sendFile = new SendFile();
			sendFile.setPath(fileUploadEntity.getFileEntity().getFilePath());
			sendFile.setFileName(fileUploadEntity.getFileEntity().getFileId() + "." + fileUploadEntity.getFileEntity().getFileExtension());
			json.put("fileInfo", JSON.parseObject(JsonMapper.toJson(sendFile)));
			fileJsonArr.add(json);
		}
		row.put("fileList", fileJsonArr);
	}
}
```
后来有发现系统的五个自带字段**status、create_by、create_date、update_by、update_date**有的要传，有的不要传，又要处理，搞来搞去最后就成就了这个接口最难的方法**jsonTableBuilder**和**jsonRowBuilder**，代码就补贴了，自己去源码那里看吧。

### 打包数据
将数据打成压缩包这里没什么好说的，建各种临时目录，然后将上面生成的JSON字符串用aes加密后写到文件中，最后和一堆附件一起打包就好了，那就是**buildZip**方法了
```
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
```
### 发送数据
这个功能也是很核心的部分，采取的传输思路是这样的：  
1. 将上面打包好的zip用刀切开很多块临时文件
2. 再把每块的基本信息存在**tran_temp_file**表中用于断点续传
3. 每块用一个线来程传，这样速度快，传完一块就删除一块临时文件和表里面的临时数据
4. 接收那边接收到文件碎片之后按照起始位置写入到大文件中就可以了，由于文件处理也有比较多的逻辑，所以分工一下，弄多了一个**FileHandler**类出来
```
/**
 * 文件处理器
 * 
 * @author 彭嘉辉
 *
 */
@Component
public class FileHandler {
	/*
	 * 碎片文件大小，1M
	 */
	private static final long BLOCK_SIZE = 1024 * 1024;

	@Autowired
	private TempFileService tempFileService;

	/**
	 * 拆分文件
	 * 
	 * @param targetFile
	 *            需要拆分的文件全路径
	 * @param busType
	 *            业务类型，用于记录这次数据传输是哪个业务，随意定义
	 * @return 碎片文件个数
	 * @throws Exception
	 */
	public List<TempFile> splitFile(String targetFile, String busType) {
		File target = new File(targetFile);
		RandomAccessFile randomAccessFile = null;
		// DataSourceTransactionManager dataSourceTransactionManager = null;
		// TransactionStatus status = null;
		long count = -1L;
		List<TempFile> returnList = null;
		try {
			randomAccessFile = new RandomAccessFile(targetFile, "r");
			long fileSize = randomAccessFile.length();
			// 拆分后的碎片文件个数
			count = fileSize / BLOCK_SIZE;
			// 处理最后多出一小份的情况
			if (fileSize % BLOCK_SIZE != 0) {
				count++;
			}
			// 创建存放碎片文件的目录
			String piceDir = target.getParent() + File.separator + "pice";
			FileUtils.createDirectory(piceDir);
			// 将文件拆分成若干临时碎片文件
			List<TempFile> tempFileList = ListUtils.newArrayList();
			for (long i = 0; i < count; i++) {
				String piceFileName = piceDir + File.separator + target.getName() + "_" + i + ".tmp";
				FileUtils.createFile(piceFileName);
				byte[] b = new byte[(int) BLOCK_SIZE];
				randomAccessFile.seek(i * BLOCK_SIZE);
				int len = randomAccessFile.read(b);
				File piceFile = new File(piceFileName);
				FileUtils.writeByteArrayToFile(piceFile, b, 0, len);
				// 记录临时碎片文件信息
				TempFile tempFile = new TempFile();
				tempFile.setBusType(busType);
				tempFile.setPath(piceFile.getParent());
				tempFile.setFileName(target.getName());
				tempFile.setPiceFileName(piceFile.getName());
				tempFile.setPoint(i);
				tempFileList.add(tempFile);
			}
			if (tempFileList.size() > 0) {
				returnList = tempFileService.insertBatch(tempFileList);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				randomAccessFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return returnList;
	}

	/**
	 * 合并文件
	 * 
	 * @param targetFile
	 *            合并后大文件全名
	 * @param piceFile
	 *            碎片文件字节流
	 * @param point
	 *            起始位置
	 * @return 是否成功
	 */
	public boolean mergeFile(String targetFile, byte[] piceFile, long point) {
		RandomAccessFile randomAccessFile = null;
		try {
			FileUtils.createFile(targetFile);
			System.out.println("第" + point + "碎片从" + point * BLOCK_SIZE + "开始写入" + piceFile.length);
			randomAccessFile = new RandomAccessFile(targetFile, "rw");
			randomAccessFile.seek(point * BLOCK_SIZE);
			randomAccessFile.write(piceFile);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				randomAccessFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

}
```
**TransmissionServiceImpl**的**transData**方法实现多线程传输
```
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
```
线程内部类
```
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
```
本来把发送请求的WebClient也是写在**TransmissionServiceImpl**里面的，但是后来觉得代码会很多，所以整出了**Client**类专门帮忙负责发请求的
```
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
```
**TransmissionServiceImpl**的**sendData**方法统一调用上面的逻辑
```
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
		JSONObject json = jsonTableBuilder(transEntity.getList(), transEntity.getEntityType(), fileList, transEntity.isRequireSysColumn(), transEntity.getRequireSysColumnArr(),
				transEntity.getExtraStr());
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
```
本来**sendData**方法有七八个参数的，我觉得传起来太麻烦了，小伙们本调用接口也不方便，代码也不好看，所以弄出了**TransEntity**类专门存放所有需要用到的参数的
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
 * extraStr 需要外传输的信息，随便任何格式的字符串，但是自动解析并不会处理这个字符串，需要在接收端用触发器处理
 * 
 * triggerName 触发器注入名称，一般为类名首字母小写后的字符串，用于数据传输完成后，在接收端需要执行的一些业务逻辑，触发器类需要在接收端写好，实现ReceiveTrigger接口
 * 
 * @author 彭嘉辉
 *
 */
public class TransEntity<T extends DataEntity<?>> implements Serializable {

	private static final long serialVersionUID = 3455698535999176893L;

	private List<T> list;
	private T entity;
	private Class<T> entityType;
	private String url;
	private String busType;
	private boolean renewal = false;
	private boolean requireSysColumn = false;
	private String[] requireSysColumnArr;
	private List<ExtraFile> extraFileList;
	private ExtraFile extraFile;
	private String extraStr;
	private String triggerName = Constant.HAS_NO_TRIGGER;

	public List<T> getList() {
		return list;
	}

	public void setList(List<T> list) {
		this.list = list;
	}

	public T getEntity() {
		return entity;
	}

	public void setEntity(T entity) {
		this.entity = entity;
	}

	public Class<T> getEntityType() {
		return entityType;
	}

	public void setEntityType(Class<T> entityType) {
		this.entityType = entityType;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getBusType() {
		return busType;
	}

	public void setBusType(String busType) {
		this.busType = busType;
	}

	public boolean isRenewal() {
		return renewal;
	}

	public void setRenewal(boolean renewal) {
		this.renewal = renewal;
	}

	public boolean isRequireSysColumn() {
		return requireSysColumn;
	}

	public void setRequireSysColumn(boolean requireSysColumn) {
		this.requireSysColumn = requireSysColumn;
	}

	public String[] getRequireSysColumnArr() {
		return requireSysColumnArr;
	}

	public void setRequireSysColumnArr(String[] requireSysColumnArr) {
		this.requireSysColumnArr = requireSysColumnArr;
	}

	public List<ExtraFile> getExtraFileList() {
		return extraFileList;
	}

	public void setExtraFileList(List<ExtraFile> extraFileList) {
		this.extraFileList = extraFileList;
	}

	public ExtraFile getExtraFile() {
		return extraFile;
	}

	public void setExtraFile(ExtraFile extraFile) {
		this.extraFile = extraFile;
	}

	public String getExtraStr() {
		return extraStr;
	}

	public void setExtraStr(String extraStr) {
		this.extraStr = extraStr;
	}

	public String getTriggerName() {
		return triggerName;
	}

	public void setTriggerName(String triggerName) {
		this.triggerName = triggerName;
	}

}
```
如果是断点续传的话就用**TransmissionServiceImpl**的**renewal**方法来处理
```
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
```
最后给到大家可接口就是这样了
```
/**
	 * 数据传输
	 * 
	 * @param transEntity
	 *            传输接口参数对象
	 * @return 结果
	 */
	<T extends DataEntity<?>> Result clientSend(TransEntity<T> transEntity);
```
实现类方法
```
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
```
专门负责接收WebClient请求的是**TransmissionController**  
接收文件的方法是下面这个
```
/**
 * 接收传输的文件
 * 
 * @param file
 *            文件对象
 * @param point
 *            开始写入的位置
 * @param busType
 *            业务类型
 * @return 响应结果
 * @throws Exception
 */
@PostMapping("receive/{token}/{appUri}")
@ResponseBody
public Result receive(MultipartFile file, long point, String busType, @PathVariable("appUri") String appUri) throws Exception {
	return this.transmissionServiceImpl.serverReceive(file, point, busType, appUri);
}
```
我客户端和服务端的业务代码都写在了**TransmissionServiceImpl**里面的了，只是用client或server来开头区分了一下  
接收的实现代码在**TransmissionServiceImpl**的**serverReceive**里面
```
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
```

### 解析数据
通过上面一系列操作，文件就接收成功了，接下来就是解析传过来的一堆东西了
客户端发送时用的是**TransExecutor**线程类，它实现的是阻塞线程接口**Callable**，所以当所有文件碎片传完之后主线程才会继续执行
上面讲到**TransmissionServiceImpl**的**sendData**方法来发送数据，里面会调用**transData**方法进行多线程传输，当所有线程跑完后，就会开始解析数据
客户端先发送解析数据的请求
```
// 发送文件
transData(client, tempPath, tempFileList);
// 调用对方的解析文件接口
result = analysisData(busType, client, transEntity.getTriggerName());
```
```
/*
 * 调用远端解析数据接口
 */
private Result analysisData(String busType, Client client, String triggerName) {
	return client.analysis(busType, triggerName);
}	
```
**Client**类发请求的方法
```
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
```
服务端接收请求
```
/**
 * 解析传输过来的数据
 * 
 * @param busType
 *            业务类型
 * @param appUri
 *            应用唯一标识
 * @param triggerName
 *            解析数据成功后需要执行的触发器名称
 * @return 响应结果
 */
@PostMapping("analysis/{busType}/{token}/{appUri}/{triggerName}")
@ResponseBody
public Result analysis(@PathVariable("busType") String busType, @PathVariable("appUri") String appUri, @PathVariable("triggerName") String triggerName) {
	return this.transmissionServiceImpl.serverAnalysis(appUri, busType, triggerName);
}
```
实现方法是**TransmissionServiceImpl**的**serverAnalysis**方法
```
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
		JSONArray tables = doAnalysis(unZipDir, jsonFileName);
		if (!triggerName.equals(Constant.HAS_NO_TRIGGER)) {
			// 执行触发器
			@SuppressWarnings("static-access")
			ReceiveTrigger trigger = (ReceiveTrigger) springContextsUtil.getBean(triggerName);
			trigger.run(tables, busType);
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
```
真正解析的地方是**TransmissionServiceImpl**的**doAnalysis**方法，先讲讲思路：  
1. 解密JSON
2. 解析JSON，生成insert语句或者update语句并执行
3. 处理附件，将附件的信息插入jeesite的两个附件系统表数据，再将附件文件复制到配置文件指向的fileupload目录中
4. 删除临时文件
5. 后来想到接收完数据之后，接收端可能还要做一些操作，比如改状态之类的，所以多了个触发器机制
6. 又有一些业务需要额外传文件的，所以又要处理，要求真多╮(╯_╰)╭  
下面是实现的代码
```
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
	// 行数据json
	JSONArray rows = table.getJSONArray("rows");
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

	// 返回表json，为了与批量处理统一，所以用JSONArray
	JSONArray tables = new JSONArray();
	tables.add(table);
	return tables;
}
```
本来数据库操作也是写在一起的，后来觉得那么多代码写在一起也不好，太热闹了，我喜欢安静，所以把他们赶到**DataBaseHandler**类去了
```
/**
 * 数据库操作处理类
 * 
 * @author 彭嘉辉
 *
 */
@Component
public class DataBaseHandler {
	@Autowired
	private RoutingDataSource routingDataSource;
	@Autowired
	private FileUploadService fileUploadService;
	@Autowired
	private FileEntityService fileEntityService;

	private Connection connection;
	private Statement statement;

	/*
	 * 初始化
	 */
	private void init() throws SQLException {
		DataSource dataSource = routingDataSource.getTargetDataSource("default");
		this.connection = dataSource.getConnection();
		this.connection.setAutoCommit(false);
		this.statement = this.connection.createStatement();
	}

	/*
	 * 判断是否新的记录
	 * 
	 * @param tableName 数据库表名
	 * 
	 * @param idMap 主键map，用map是因为可能是联合主键
	 */
	private boolean isNewRecord(String tableName, Map<String, Object> idMap) {
		ResultSet resultSet = null;
		try {
			List<String> queryCondition = ListUtils.newArrayList();
			idMap.keySet().forEach(key -> {
				queryCondition.add(key + " = '" + idMap.get(key) + "'");
			});
			String querySQL = "SELECT COUNT(0) FROM " + tableName + " WHERE " + StringUtils.join(queryCondition.toArray(), " AND ");
			System.out.println("查询语句：" + querySQL);
			resultSet = this.statement.executeQuery(querySQL);
			if (resultSet.next() && resultSet.getInt(1) > 0) {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	/**
	 * 处理数据表数据
	 * 
	 * @param tableName
	 *            表名
	 * @param rows
	 *            所有行数据
	 * @param unZipDir
	 *            解压后的目录
	 * @throws SQLException
	 */
	public void setData(String tableName, JSONArray rows, String unZipDir) throws Exception {
		if (this.connection == null) {
			init();
		}
		for (Object object : rows) {

			JSONObject row = JSON.parseObject(object.toString());
			// 主键
			JSONObject id = row.getJSONObject("id");
			try {
				if (isNewRecord(tableName, id.getInnerMap())) {
					// 插入数据
					System.out.println("新增");
					this.statement.executeUpdate(insertSQLBuilder(tableName, row));
				} else {
					// 更新数据
					System.out.println("更新");
					this.statement.executeUpdate(updateSQLBuilder(tableName, row));
				}

				// 处理附件
				JSONArray fileListJson = row.getJSONArray("fileList");
				if (fileListJson != null) {
					fileListJson.forEach(obj -> {
						JSONObject fileJson = JSON.parseObject(obj.toString());
						FileUpload fileUpload = JSON.toJavaObject(fileJson, FileUpload.class);
						FileEntity fileEntity = fileUpload.getFileEntity();

						// 处理系统附件表
						try {// 新增
							fileUpload.setIsNewRecord(true);
							fileEntity.setIsNewRecord(true);
							fileUploadService.save(fileUpload);
							fileEntityService.save(fileEntity);
						} catch (Exception e) {// 更新
							fileUpload.setIsNewRecord(false);
							fileEntity.setIsNewRecord(false);
							fileUploadService.save(fileUpload);
							fileEntityService.save(fileEntity);
						}

						// 处理文件
						String fileId = fileEntity.getFileId();
						String filePath = fileEntity.getFilePath();
						String fileExtension = fileEntity.getFileExtension();
						String attachmentName = fileId + "." + fileExtension;
						String fileUploadPath = Global.getUserfilesBaseDir("fileupload");
						FileUtils.copyFileCover(unZipDir + File.separator + attachmentName, fileUploadPath + File.separator + filePath + File.separator + attachmentName, true);
					});
				}

			} catch (Exception e) {
				try {
					connection.rollback();
				} catch (Exception e1) {
					e1.printStackTrace();
					throw new Exception(e.getMessage());
				}
				throw new Exception(e.getMessage());
			}

		}
	}

	/**
	 * 提交数据
	 */
	public void commit() {
		try {
			if (this.connection != null) {
				this.connection.commit();
			}
		} catch (SQLException e) {
			try {
				this.connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			try {
				if (this.statement != null) {
					this.statement.close();
					this.statement = null;
				}
				if (this.connection != null) {
					this.connection.close();
					this.connection = null;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * 生成insert语句
	 */
	private String insertSQLBuilder(String tableName, JSONObject row) {
		String insertSQL = "INSERT INTO " + tableName;
		List<String> columnList = ListUtils.newArrayList();
		List<String> valueList = ListUtils.newArrayList();
		// 解析各列的值
		JSONArray rowData = row.getJSONArray("rowData");
		rowData.forEach(rowDataObj -> {
			JSONObject column = JSON.parseObject(rowDataObj.toString());
			columnList.add(column.getString("to"));
			if (column.getString("type").equals("number")) {// 数字
				valueList.add(column.get("value").toString());
			} else if (column.getString("type").equals("date")) {// 日期
				valueList.add("'" + DateUtils.formatDate(column.getLong("value"), "yyyy-MM-dd HH:mm:ss") + "'");
			} else {// 布尔值和字符串
				valueList.add("'" + column.get("value") + "'");
			}
		});
		insertSQL = insertSQL + " (" + StringUtils.join(columnList.toArray(), ",") + ") VALUES (" + StringUtils.join(valueList.toArray(), ",") + ")";
		System.out.println("插入语句：" + insertSQL);
		return insertSQL;
	}

	/*
	 * 生成update语句
	 */
	private String updateSQLBuilder(String tableName, JSONObject row) {
		String updateSQL = "UPDATE " + tableName + " SET";
		List<String> valueList = ListUtils.newArrayList();
		List<String> idList = ListUtils.newArrayList();
		// 解析各列的值
		JSONArray rowData = row.getJSONArray("rowData");
		rowData.forEach(rowDataObj -> {
			JSONObject column = JSON.parseObject(rowDataObj.toString());
			if (column.get("isPK") != null && column.getBoolean("isPK")) {
				idList.add(column.getString("to") + " = '" + column.get("value") + "'");
			} else if (column.getString("type").equals("number")) {// 数字
				valueList.add(column.getString("to") + " = " + column.get("value").toString());
			} else if (column.getString("type").equals("date")) {// 日期
				valueList.add(column.getString("to") + " = '" + DateUtils.formatDate(column.getLong("value"), "yyyy-MM-dd HH:mm:ss") + "'");
			} else {// 布尔值和字符串
				valueList.add(column.getString("to") + " = '" + column.get("value") + "'");
			}
		});
		updateSQL = updateSQL + " " + StringUtils.join(valueList.toArray(), ",") + " WHERE " + StringUtils.join(idList.toArray(), " AND ");
		System.out.println("更新语句：" + updateSQL);
		return updateSQL;
	}

}
```
还有触发器接口  
```
/**
 * 接收数据完成后触发的触发器，接收数据结束后如果想做一些操作请写一个触发器类，实现此接口，发送数据时把触发器的注入名传过来就行
 * 
 * @author 彭嘉辉
 *
 */
public interface ReceiveTrigger {
	void run(JSONArray rows, String busType);
}
```
触发器的调用实现
```
if (!triggerName.equals(Constant.HAS_NO_TRIGGER)) {
	// 执行触发器
	@SuppressWarnings("static-access")
	ReceiveTrigger trigger = (ReceiveTrigger) springContextsUtil.getBean(triggerName);
	trigger.run(tables, busType);
}
```
-----------------------------------------------------------终于都写完发送数据的接口说明啦-----------------------------------------------------------------

## 然后讲推送数据吧



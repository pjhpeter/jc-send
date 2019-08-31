# 前言
写这个接口算是经验累积吧，所以写一份文档分享一下，讲讲解接口是怎么一步一步做出来的，主要聊聊思路方面的东西。里面相关的技术都是很简单的，谁都学得会，但是思路就需要思考和总结了 

# 需求分析
+ 需求分析很重要！需求分析很重要！需求分析很重要！
+ 这个接口的功能是为了在多个不同的项目之间建立一套数据传输的机制，而所有项目运行的环境都是在jeesite框架上，所以接口也使用jeesite框架作为基础，一来可以便利使用jeesite的API，二来跟各个项目可以无缝连接  
+ 一次传输的数据包括数据库的数据，附件等，数据库的数据是序列化的，网络传输序列化数据最通用的就是JSON，而附件等文件传输可以有多种方式，常用的是文件流的方式，或者转成base64字符串后添加到JSON当中传输。如果附件文件较小，添加到JSON当中传是非常便利的，但是文件巨大的情况下，生成JSON的过程会很缓慢，解析的时候需要把整个JSON读到内存中，如果有个附件中有部高清电影，内存直接爆炸哦(⊙﹏⊙)，由于无法确定传输的是什么数据，所以不知道大小，选择文件流的方式传是比较好的  
+ 选用上面结论的方式传输，意味着每次传输可能都是有多个文件的，一个JSON文件，多个附件文件，不确定文件的个数，接收端的代码写起来会比较麻烦，所有就想到把所有文件压缩成一个zip再传，jeesite的FileUtils类也有非常方便的压缩文件方法，这样就永远都是一个文件在传输啦♪(^∇^*)  
+ 由于是涉密系统，传输的数据是需要保护的，不然被人抓包拿去卖就不好了(ˉ▽￣～) 切~~，所以JSON文件内容要加密，压缩包也要加密码。经树根介绍，jeesite有自己的加密方法，就是AesUtils类，真的很方便哦。但是！！！FileUtils类的压缩文件方法居然没有看到可以加密码的参数，我又不想自己再写加密压缩了，所以就这样偷偷不加密压缩包也没有关系吧ㄟ( ▔, ▔ )ㄏ
+ 传输的文件如果很大的话，过程很漫长，中间万一网线被老鼠咬断了怎么办o( =•ω•= )m，断点续传当然是常规操作啦  
+ 刘老师说如果单位端没有开机，中心端有审批数据需要下发的时候，下发不了就搞笑了，所以必须要有单位端来拉取数据，中心端永远都不主动给单位端发送东西了，行吧，姜还是老的辣  
+ 树根一直在吐槽杨龙写的socket方式传输很恶心，不稳定，一直让我不要用。我以往的经验是用WebService的，但是又要部署多好多服务，所以这两种方式都不用了，看了网上的资料，发现Apache的WebClient好像很厉害的样子，于是数据传输就选择了WebClient  
+ 说好了做接口，当然是要通用的啦，如果又要做得好用，还得让调用的小伙伴少写代码，不然被举报就惨了。那么数据组装和解析都要做得很智能，这样的话泛型和注解就必不可少了  
经过上面一番思考人生，总算是把接口的雏形想明白了，这样就成功了一半啦！其实一行代码都还没有写ヽ(✿ﾟ▽ﾟ)ノ 

# 开始写代码啦
## 首先把数据拼装出来吧
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
那么就开始吧，主要的处理都在TransmissionService接口的实现类TransmissionServiceImpl里面。

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
后来有发现系统的五个自带字段status、create_by、create_date、update_by、update_date有的要传，有的不要传，又要处理，搞来搞去最后就成就了这个接口最难的方法**jsonTableBuilder**和**jsonRowBuilder**
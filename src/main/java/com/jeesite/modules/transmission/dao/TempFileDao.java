package com.jeesite.modules.transmission.dao;

import com.jeesite.common.dao.CrudDao;
import com.jeesite.common.mybatis.annotation.MyBatisDao;
import com.jeesite.modules.transmission.entity.TempFile;

/**
 * 临时碎片文件信息dao层接口，用于断点续传
 * 
 * @author 彭嘉辉
 *
 */
@MyBatisDao
public interface TempFileDao extends CrudDao<TempFile> {

}

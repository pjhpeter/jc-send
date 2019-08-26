package com.jeesite.modules.transmission.dao;

import com.jeesite.common.dao.CrudDao;
import com.jeesite.common.mybatis.annotation.MyBatisDao;
import com.jeesite.modules.transmission.entity.PullDataFlag;

/**
 * 有可被拉取数据的标识dao层接口
 * 
 * @author 彭嘉辉
 *
 */
@MyBatisDao
public interface PullDataFlagDao extends CrudDao<PullDataFlag> {

}

package com.jeesite.modules.transmission.service;

import org.springframework.stereotype.Service;

import com.jeesite.common.service.CrudService;
import com.jeesite.modules.transmission.dao.PullDataFlagDao;
import com.jeesite.modules.transmission.entity.PullDataFlag;

/**
 * 有可被拉取数据的标识service层接口
 * 
 * @author 彭嘉辉
 *
 */
@Service
public class PullDataFlagService extends CrudService<PullDataFlagDao, PullDataFlag> {

}

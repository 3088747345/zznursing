package com.zzyl.nursing.service.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzyl.common.constant.CacheConstants;
import com.zzyl.nursing.domain.NursingLevel;
import com.zzyl.nursing.mapper.NursingLevelMapper;
import com.zzyl.nursing.service.INursingLevelService;
import com.zzyl.nursing.vo.NursingLevelVo;

import cn.hutool.core.util.ObjectUtil;

/**
 * 护理等级Service业务层处理
 * 
 * @author alexis
 * @date 2025-06-02
 */
@Service
public class NursingLevelServiceImpl extends ServiceImpl<NursingLevelMapper, NursingLevel>
        implements INursingLevelService {
    @Autowired
    private NursingLevelMapper nursingLevelMapper;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 查询护理等级
     * 
     * @param id 护理等级主键
     * @return 护理等级
     */
    @Override
    public NursingLevel selectNursingLevelById(Long id) {
        return getById(id);
    }

    /**
     * 查询护理等级列表
     * 
     * @param nursingLevel 护理等级
     * @return 护理等级
     */
    @Override
    public List<NursingLevel> selectNursingLevelList(NursingLevel nursingLevel) {
        return nursingLevelMapper.selectNursingLevelList(nursingLevel);
    }

    /**
     * 新增护理等级
     * 
     * @param nursingLevel 护理等级
     * @return 结果
     */
    @Override
    public int insertNursingLevel(NursingLevel nursingLevel) {
        int flag = save(nursingLevel) ? 1 : 0;
        if (flag == 1) {
            redisTemplate.delete(CacheConstants.NURSING_LEVEL_ALL_KEY);
        }
        return flag;
    }

    /**
     * 修改护理等级
     * 
     * @param nursingLevel 护理等级
     * @return 结果
     */
    @Override
    public int updateNursingLevel(NursingLevel nursingLevel) {
        int flag = updateById(nursingLevel) ? 1 : 0;
        if (flag == 1) {
            redisTemplate.delete(CacheConstants.NURSING_LEVEL_ALL_KEY);
        }
        return flag;
    }

    /**
     * 批量删除护理等级
     * 
     * @param ids 需要删除的护理等级主键
     * @return 结果
     */
    @Override
    public int deleteNursingLevelByIds(Long[] ids) {
        int flag = removeByIds(Arrays.asList(ids)) ? 1 : 0;
        if (flag == 1) {
            redisTemplate.delete(CacheConstants.NURSING_LEVEL_ALL_KEY);
        }
        return flag;
    }

    /**
     * 删除护理等级信息
     * 
     * @param id 护理等级主键
     * @return 结果
     */
    @Override
    public int deleteNursingLevelById(Long id) {
        int flag = removeById(id) ? 1 : 0;
        if (flag == 1) {
            redisTemplate.delete(CacheConstants.NURSING_LEVEL_ALL_KEY);
        }
        return flag;
    }

    /**
     * 查询护理等级Vo列表
     *
     * @param nursingLevel 条件
     * @return 结果
     */
    @Override
    public List<NursingLevelVo> selectNursingLevelVoList(NursingLevel nursingLevel) {
        return nursingLevelMapper.selectNursingLevelVoList(nursingLevel);
    }

    /**
     * 查询所有护理等级
     * 
     * 先查询Redis中是否有数据，有数据直接返回，没有数据从数据库查询并存入Redis
     * 
     * @return 结果
     */
    @Override
    public List<NursingLevel> listAll() {

        List<NursingLevel> list = (List<NursingLevel>) redisTemplate.opsForValue()
                .get(CacheConstants.NURSING_LEVEL_ALL_KEY);

        if (ObjectUtil.isNotEmpty(list)) {
            return list;
        }
        LambdaQueryWrapper<NursingLevel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NursingLevel::getStatus, 1);
        list = list(queryWrapper);
        redisTemplate.opsForValue().set(CacheConstants.NURSING_LEVEL_ALL_KEY, list, 60 * 60 * 24 * 7);
        return list;
    }
}

package com.zzyl.nursing.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzyl.nursing.domain.CheckIn;
import com.zzyl.nursing.dto.CheckInApplyDto;
import com.zzyl.nursing.vo.CheckInDetailVo;

/**
 * 入住Service接口
 * 
 * @author chen
 * @date 2026-09-17
 */
public interface ICheckInService extends IService<CheckIn> {
    /**
     * 查询入住
     * 
     * @param id 入住主键
     * @return 入住
     */
    public CheckIn selectCheckInById(Long id);

    /**
     * 查询入住列表
     * 
     * @param checkIn 入住
     * @return 入住集合
     */
    public List<CheckIn> selectCheckInList(CheckIn checkIn);

    /**
     * 新增入住
     * 
     * @param checkIn 入住
     * @return 结果
     */
    public int insertCheckIn(CheckIn checkIn);

    /**
     * 修改入住
     * 
     * @param checkIn 入住
     * @return 结果
     */
    public int updateCheckIn(CheckIn checkIn);

    /**
     * 批量删除入住
     * 
     * @param ids 需要删除的入住主键集合
     * @return 结果
     */
    public int deleteCheckInByIds(Long[] ids);

    /**
     * 删除入住信息
     * 
     * @param id 入住主键
     * @return 结果
     */
    public int deleteCheckInById(Long id);

    /**
     * 申请入住
     * 
     * @param dto 申请的入住对象
     */
    public void apply(CheckInApplyDto dto);

    /**
     * 查询入住详情
     * 
     * @param id 入住ID
     * @return 入住详情
     */
    public CheckInDetailVo detail(Long id);
}

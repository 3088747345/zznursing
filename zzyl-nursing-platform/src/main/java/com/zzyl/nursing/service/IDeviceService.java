package com.zzyl.nursing.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.nursing.domain.Device;
import com.zzyl.nursing.dto.DeviceDto;
import com.zzyl.nursing.vo.DeviceDetailVo;
import com.zzyl.nursing.vo.ProductVo;

/**
 * 设备Service接口
 * 
 * @author chen
 * @date 2026-09-30
 */
public interface IDeviceService extends IService<Device> {
    /**
     * 查询设备
     * 
     * @param id 设备主键
     * @return 设备
     */
    public Device selectDeviceById(Long id);

    /**
     * 查询设备列表
     * 
     * @param device 设备
     * @return 设备集合
     */
    public List<Device> selectDeviceList(Device device);

    /**
     * 新增设备
     * 
     * @param device 设备
     * @return 结果
     */
    public int insertDevice(Device device);

    /**
     * 修改设备
     * 
     * @param device 设备
     * @return 结果
     */
    public int updateDevice(Device device);

    /**
     * 批量删除设备
     * 
     * @param ids 需要删除的设备主键集合
     * @return 结果
     */
    public int deleteDeviceByIds(Long[] ids);

    /**
     * 删除设备信息
     * 
     * @param id 设备主键
     * @return 结果
     */
    public int deleteDeviceById(Long id);

    /**
     * 同步物联网平台产品列表
     */
    public void syncProductList();

    /**
     * 查询所有产品列表
     *
     * @return 产品列表
     */
    List<ProductVo> allProduct();

    /*
     * 注册设备
     */
    public void registerDevice(DeviceDto deviceDto);

    /*
     * 查询设备详细信息
     */
    public DeviceDetailVo queryDeviceDetail(String iotId);

    /*
     * 查询设备上报数据
     */
    public AjaxResult queryServiceProperties(String iotId);

    /**
     * 修改设备
     */
    public void updateDevice1(DeviceDto device);

    /**
     * 删除设备
     */
    public void deleteDeviceByIotId(String iotId);
}

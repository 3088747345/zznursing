package com.zzyl.nursing.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzyl.nursing.domain.Device;

/**
 * 设备Mapper接口
 * 
 * @author chen
 * @date 2026-09-30
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
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
     * 删除设备
     * 
     * @param id 设备主键
     * @return 结果
     */
    public int deleteDeviceById(Long id);

    /**
     * 批量删除设备
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDeviceByIds(Long[] ids);

    /**
     * 根据随身设备id查询老人关联的护理人员id列表
     * 
     * @param iotId 设备id
     * @return 护理人员列表
     */
    List<Long> selectNursingIdsByIotIdWithElder(@Param("iotId") String iotId);

    /**
     * 根据固定设备id查询老人关联的护理人员id列表
     * 
     * @param iotId 设备id
     * @return 护理人员列表
     */
    List<Long> selectNursingIdsByIotIdWithBed(@Param("iotId") String iotId);
}

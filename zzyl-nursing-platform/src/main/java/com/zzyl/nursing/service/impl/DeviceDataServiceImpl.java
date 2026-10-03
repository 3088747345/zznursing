package com.zzyl.nursing.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.ObjectUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzyl.common.constant.HttpStatus;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.common.utils.DateTimeZoneConverter;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.nursing.domain.Device;
import com.zzyl.nursing.domain.DeviceData;
import com.zzyl.nursing.dto.DeviceDataPageReqDto;
import com.zzyl.nursing.mapper.DeviceDataMapper;
import com.zzyl.nursing.mapper.DeviceMapper;
import com.zzyl.nursing.service.IDeviceDataService;
import com.zzyl.nursing.task.vo.IotMsgNotifyData;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;

/**
 * 设备数据Service业务层处理
 * 
 * @author chen
 * @date 2026-10-02
 */
@Service
public class DeviceDataServiceImpl extends ServiceImpl<DeviceDataMapper, DeviceData> implements IDeviceDataService {
    @Autowired
    private DeviceDataMapper deviceDataMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    /**
     * 查询设备数据
     * 
     * @param id 设备数据主键
     * @return 设备数据
     */
    @Override
    public DeviceData selectDeviceDataById(Long id) {
        return getById(id);
    }

    /**
     * 查询设备数据列表
     * 
     * @param dto 设备数据
     * @return 设备数据
     */
    @Override
    public TableDataInfo selectDeviceDataList(DeviceDataPageReqDto dto) {

        LambdaQueryWrapper<DeviceData> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        Page<DeviceData> page = new Page(dto.getPageNum(), dto.getPageSize());
        // 模糊查询设备名称
        if (StringUtils.isNotEmpty(dto.getDeviceName())) {
            lambdaQueryWrapper.eq(DeviceData::getDeviceName, dto.getDeviceName());
        }
        // 精确查询功能名称
        if (StringUtils.isNotEmpty(dto.getFunctionId())) {
            lambdaQueryWrapper.eq(DeviceData::getFunctionId, dto.getFunctionId());
        }
        // 时间范围查询
        if (ObjectUtils.isNotEmpty(dto.getStartTime()) && ObjectUtils.isNotEmpty(dto.getEndTime())) {
            lambdaQueryWrapper.between(DeviceData::getAlarmTime, dto.getStartTime(), dto.getEndTime());
        }

        // 分页查询
        page = page(page, lambdaQueryWrapper);

        // 封装分页对象
        return getTableDataInfo(page);

    }

    /**
     * 封装分页对象
     * 
     * @param page
     * @return
     */
    @NotNull
    private static TableDataInfo getTableDataInfo(Page<DeviceData> page) {
        TableDataInfo tableData = new TableDataInfo();
        tableData.setCode(HttpStatus.SUCCESS);
        tableData.setMsg("查询成功");
        tableData.setRows(page.getRecords());
        tableData.setTotal(page.getTotal());
        return tableData;
    }

    /**
     * 新增设备数据
     * 
     * @param deviceData 设备数据
     * @return 结果
     */
    @Override
    public int insertDeviceData(DeviceData deviceData) {
        return save(deviceData) ? 1 : 0;
    }

    /**
     * 修改设备数据
     * 
     * @param deviceData 设备数据
     * @return 结果
     */
    @Override
    public int updateDeviceData(DeviceData deviceData) {
        return updateById(deviceData) ? 1 : 0;
    }

    /**
     * 批量删除设备数据
     * 
     * @param ids 需要删除的设备数据主键
     * @return 结果
     */
    @Override
    public int deleteDeviceDataByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除设备数据信息
     * 
     * @param id 设备数据主键
     * @return 结果
     */
    @Override
    public int deleteDeviceDataById(Long id) {
        return removeById(id) ? 1 : 0;
    }

    /**
     * 批量保存设备数据
     * 
     * @param iotMsgNotifyData
     */
    @Override
    public void batchInsertDeviceData(IotMsgNotifyData iotMsgNotifyData) {
        String iotId = iotMsgNotifyData.getHeader().getDeviceId();
        // 查询设备信息
        Device device = deviceMapper.selectOne(Wrappers.<Device>lambdaQuery().eq(Device::getIotId, iotId));
        if (ObjectUtil.isEmpty(device)) {
            log.error("设备不存在");
            return;
        }
        // 批量保存设备数据
        iotMsgNotifyData.getBody().getServices().forEach(s -> {
            // 判断属性是否为空
            Map<String, Object> properties = s.getProperties();
            if (CollUtil.isEmpty(properties)) {
                return;
            }

            // 上报时间处理
            String eventTimeStr = s.getEventTime();
            LocalDateTime localDateTime = LocalDateTimeUtil.parse(eventTimeStr, "yyyyMMdd'T'HHmmss'Z'");
            LocalDateTime eventTime = DateTimeZoneConverter.utcToShanghai(localDateTime);

            List<DeviceData> list = new ArrayList<>();

            // key:属性id，value:属性值
            properties.forEach((k, v) -> {
                DeviceData deviceData = BeanUtil.toBean(device, DeviceData.class);
                deviceData.setId(null);
                deviceData.setAlarmTime(eventTime);
                deviceData.setFunctionId(k);
                deviceData.setDataValue(v + "");
                list.add(deviceData);
            });
            // 批量保存设备数据
            saveBatch(list);
        });
    }
}

package com.zzyl.nursing.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huaweicloud.sdk.iotda.v5.IoTDAClient;
import com.huaweicloud.sdk.iotda.v5.model.AddDevice;
import com.huaweicloud.sdk.iotda.v5.model.AddDeviceRequest;
import com.huaweicloud.sdk.iotda.v5.model.AddDeviceResponse;
import com.huaweicloud.sdk.iotda.v5.model.AuthInfo;
import com.huaweicloud.sdk.iotda.v5.model.DeleteDeviceRequest;
import com.huaweicloud.sdk.iotda.v5.model.DeviceShadowData;
import com.huaweicloud.sdk.iotda.v5.model.DeviceShadowProperties;
import com.huaweicloud.sdk.iotda.v5.model.ListProductsRequest;
import com.huaweicloud.sdk.iotda.v5.model.ListProductsResponse;
import com.huaweicloud.sdk.iotda.v5.model.ServiceCapability;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceRequest;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceResponse;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceShadowRequest;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceShadowResponse;
import com.huaweicloud.sdk.iotda.v5.model.ShowProductRequest;
import com.huaweicloud.sdk.iotda.v5.model.ShowProductResponse;
import com.huaweicloud.sdk.iotda.v5.model.UpdateDevice;
import com.huaweicloud.sdk.iotda.v5.model.UpdateDeviceRequest;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.common.exception.base.BaseException;
import com.zzyl.common.utils.DateTimeZoneConverter;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.nursing.domain.Device;
import com.zzyl.nursing.dto.DeviceDto;
import com.zzyl.nursing.mapper.DeviceMapper;
import com.zzyl.nursing.service.IDeviceService;
import com.zzyl.nursing.vo.DeviceDetailVo;
import com.zzyl.nursing.vo.ProductVo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 设备Service业务层处理
 * 
 * @author chen
 * @date 2026-09-30
 */
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements IDeviceService {
    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private IoTDAClient client;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 查询设备
     * 
     * @param id 设备主键
     * @return 设备
     */
    @Override
    public Device selectDeviceById(Long id) {
        return getById(id);
    }

    /**
     * 查询设备列表
     * 
     * @param device 设备
     * @return 设备
     */
    @Override
    public List<Device> selectDeviceList(Device device) {
        return deviceMapper.selectDeviceList(device);
    }

    /**
     * 新增设备
     * 
     * @param device 设备
     * @return 结果
     */
    @Override
    public int insertDevice(Device device) {
        return save(device) ? 1 : 0;
    }

    /**
     * 修改设备
     * 
     * @param device 设备
     * @return 结果
     */
    @Override
    public int updateDevice(Device device) {
        return updateById(device) ? 1 : 0;
    }

    /**
     * 批量删除设备
     * 
     * @param ids 需要删除的设备主键
     * @return 结果
     */
    @Override
    public int deleteDeviceByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除设备信息
     * 
     * @param id 设备主键
     * @return 结果
     */
    @Override
    public int deleteDeviceById(Long id) {
        return removeById(id) ? 1 : 0;
    }

    @Override
    public void syncProductList() {
        // 请求参数
        ListProductsRequest listProductsRequest = new ListProductsRequest();
        // 设置条数
        listProductsRequest.setLimit(50);
        // 发起请求
        ListProductsResponse response = client.listProducts(listProductsRequest);
        if (response.getHttpStatusCode() != 200) {
            throw new BaseException("物联网接口 - 查询产品，同步失败");
        }
        // 存储到redis
        redisTemplate.opsForValue().set("prouductList", JSONUtil.toJsonStr(response.getProducts()));

    }

    /**
     * 查询所有产品列表
     */
    @Override
    public List<ProductVo> allProduct() {
        // 从redis中查询数据
        String jsonStr = redisTemplate.opsForValue().get("prouductList");
        // 如果数据为空，则返回一个空集合
        if (StringUtils.isEmpty(jsonStr)) {
            return Collections.emptyList();
        }
        // 解析数据，并返回
        return JSONUtil.toList(jsonStr, ProductVo.class);
    }

    /**
     * 注册设备
     */
    @Override
    public void registerDevice(DeviceDto dto) {
        // 判断设备名称是否存在
        long count = count(Wrappers.<Device>lambdaQuery().eq(Device::getDeviceName, dto.getDeviceName()));
        if (count > 0) {
            throw new BaseException("设备名称已存在，请重新输入");
        }

        // 判断设备标识是否存在
        count = count(Wrappers.<Device>lambdaQuery().eq(Device::getNodeId, dto.getNodeId()));
        if (count > 0) {
            throw new BaseException("设备标识码已存在，请重新输入");
        }

        // 判断同一位置是否绑定了相同的产品
        count = count(Wrappers.<Device>lambdaQuery()
                .eq(Device::getProductKey, dto.getProductKey())
                .eq(Device::getBindingLocation, dto.getBindingLocation())
                .eq(Device::getLocationType, dto.getLocationType())
                .eq(dto.getPhysicalLocationType() != null, Device::getPhysicalLocationType,
                        dto.getPhysicalLocationType()));
        if (count > 0) {
            throw new BaseException("该老人/位置已绑定该产品，请重新选择");
        }

        // 注册设备--->IoT平台
        AddDeviceRequest request = new AddDeviceRequest();
        AddDevice body = new AddDevice();
        body.withProductId(dto.getProductKey());
        body.withDeviceName(dto.getDeviceName());
        body.withNodeId(dto.getNodeId());

        // 秘钥设置
        AuthInfo authInfo = new AuthInfo();
        String secret = UUID.randomUUID().toString().replaceAll("-", "");
        authInfo.withSecret(secret);
        body.setAuthInfo(authInfo);
        request.setBody(body);

        AddDeviceResponse response;
        try {
            response = client.addDevice(request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BaseException("物联网接口 - 注册设备，调用失败");
        }
        // 本地保存设备
        // 属性拷贝
        Device device = BeanUtil.toBean(dto, Device.class);
        // 秘钥
        device.setSecret(secret);
        // 设备id、设备绑定状态
        device.setIotId(response.getDeviceId());
        save(device);

    }

    /**
     * 查询设备详细信息
     */
    @Override
    public DeviceDetailVo queryDeviceDetail(String iotId) {
        // 查询本地设备数据
        Device device = getOne(Wrappers.<Device>lambdaQuery().eq(Device::getIotId, iotId));
        if (ObjectUtil.isEmpty(device)) {
            return null;
        }
        // 调用华为云接口查询设备详情
        ShowDeviceRequest request = new ShowDeviceRequest();
        request.setDeviceId(iotId);
        ShowDeviceResponse response;
        try {
            response = client.showDevice(request);
        } catch (Exception e) {
            throw new BaseException("物联网接口 - 查询设备详情，调用失败");
        }
        // 属性拷贝
        DeviceDetailVo deviceVo = BeanUtil.toBean(device, DeviceDetailVo.class);
        deviceVo.setDeviceStatus(response.getStatus());
        String activeTimeStr = response.getActiveTime();
        // 日期转换
        if (StringUtils.isNotEmpty(activeTimeStr)) {
            // 把字符串转换为LocalDateTime
            LocalDateTime activeTime = LocalDateTimeUtil.parse(activeTimeStr, DatePattern.UTC_MS_PATTERN);
            // 日期时区转换
            deviceVo.setActiveTime(DateTimeZoneConverter.utcToShanghai(activeTime));
        }

        return deviceVo;
    }

    /**
     * 查询设备上报数据
     */
    @Override
    public AjaxResult queryServiceProperties(String iotId) {
        ShowDeviceShadowRequest request = new ShowDeviceShadowRequest();
        request.setDeviceId(iotId);
        ShowDeviceShadowResponse response = client.showDeviceShadow(request);
        if (response.getHttpStatusCode() != 200) {
            throw new BaseException("物联网接口 - 查询设备影子，调用失败");
        }
        List<DeviceShadowData> shadow = response.getShadow();
        if (CollUtil.isEmpty(shadow)) {
            List<Object> emptyList = Collections.emptyList();
            return AjaxResult.success(emptyList);
        }
        // 获取上报数据的reported （参考返回的json数据）
        DeviceShadowProperties reported = shadow.get(0).getReported();
        // 把数据转换为JSONObject(map)，方便处理
        JSONObject jsonObject = JSONUtil.parseObj(reported.getProperties());
        // 遍历数据，封装到list中
        List<Map<String, Object>> list = new ArrayList<>();
        // 事件上报时间
        String eventTimeStr = reported.getEventTime();
        // 把字符串转换为LocalDateTime
        LocalDateTime eventTimeLocalDateTime = LocalDateTimeUtil.parse(eventTimeStr, "yyyyMMdd'T'HHmmss'Z'");
        // 时区转换
        LocalDateTime eventTime = DateTimeZoneConverter.utcToShanghai(eventTimeLocalDateTime);

        // k:属性标识，v:属性值
        jsonObject.forEach((k, v) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("functionId", k);
            map.put("value", v);
            map.put("eventTime", eventTime);
            list.add(map);
        });

        // 数据返回
        return AjaxResult.success(list);
    }

    /**
     * 修改设备
     */
    @Override
    public void updateDevice1(DeviceDto device) {
        // 1. 判断设备名称是否存在（排除自身）
        long count = count(Wrappers.<Device>lambdaQuery()
                .eq(Device::getDeviceName, device.getDeviceName())
                .ne(Device::getId, device.getId()));
        if (count > 0) {
            throw new BaseException("设备名称已存在，请重新输入");
        }

        // 判断设备标识是否存在（排除自身）
        count = count(Wrappers.<Device>lambdaQuery().eq(Device::getNodeId, device.getNodeId())
                .ne(Device::getId, device.getId()));
        if (count > 0) {
            throw new BaseException("设备标识码已存在，请重新输入");
        }

        // 2. 判断同一位置是否绑定了相同的产品（排除自身）
        count = count(Wrappers.<Device>lambdaQuery()
                .eq(Device::getProductKey, device.getProductKey())
                .eq(Device::getBindingLocation, device.getBindingLocation())
                .eq(Device::getLocationType, device.getLocationType())
                .eq(device.getPhysicalLocationType() != null, Device::getPhysicalLocationType,
                        device.getPhysicalLocationType())
                .ne(Device::getId, device.getId()));
        if (count > 0) {
            throw new BaseException("该老人/位置已绑定该产品，请重新选择");
        }

        // 3. 同步修改华为云 IoT 平台的设备名称
        UpdateDeviceRequest request = new UpdateDeviceRequest();
        // 必须指定要修改哪个设备（华为云上的 deviceId / iotId）
        request.setDeviceId(device.getIotId());
        UpdateDevice body = new UpdateDevice();
        body.withDeviceName(device.getDeviceName());
        request.setBody(body);

        try {
            client.updateDevice(request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BaseException("物联网接口 - 修改设备名称，调用失败");
        }

        // 4. 属性拷贝为 Device 实体类，并更新本地数据库喵！
        Device deviceEntity = BeanUtil.toBean(device, Device.class);
        // 如果是老人位置类型，物理位置类型强制设置为-1
        if (device.getLocationType() == 0) {
            deviceEntity.setPhysicalLocationType(-1);
        }
        updateById(deviceEntity);
    }

    /**
     * 删除设备
     */
    @Override
    public void deleteDeviceByIotId(String iotId) {
        // 1. 先删除物联网平台上的设备
        DeleteDeviceRequest request = new DeleteDeviceRequest();
        request.setDeviceId(iotId);
        try {
            client.deleteDevice(request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BaseException("物联网接口 - 删除设备，调用失败");
        }
        // 2. 再删除本地数据库中的设备信息
        remove(Wrappers.<Device>lambdaQuery().eq(Device::getIotId, iotId));
    }

    /**
     * 查询产品详情
     * 
     * @param productKey
     * @return
     */
    @Override
    public AjaxResult queryProduct(String productKey) {
        // 参数校验
        if (StringUtils.isEmpty(productKey)) {
            throw new BaseException("请输入正确的参数");
        }
        // 调用华为云IOT平台接口
        ShowProductRequest showProductRequest = new ShowProductRequest();
        showProductRequest.setProductId(productKey);
        ShowProductResponse response;

        try {
            response = client.showProduct(showProductRequest);
        } catch (Exception e) {
            throw new BaseException("查询产品详情失败");
        }
        // 判断是否存在服务数据
        List<ServiceCapability> serviceCapabilities = response.getServiceCapabilities();
        if (CollUtil.isEmpty(serviceCapabilities)) {
            return AjaxResult.success(Collections.emptyList());
        }
        return AjaxResult.success(serviceCapabilities);
    }

}

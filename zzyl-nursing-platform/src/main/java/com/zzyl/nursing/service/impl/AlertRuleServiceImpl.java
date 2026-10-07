package com.zzyl.nursing.service.impl;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.nursing.config.WebSocketServer;
import com.zzyl.nursing.domain.AlertData;
import com.zzyl.nursing.domain.AlertRule;
import com.zzyl.nursing.domain.DeviceData;
import com.zzyl.nursing.mapper.AlertRuleMapper;
import com.zzyl.nursing.mapper.DeviceMapper;
import com.zzyl.nursing.service.IAlertDataService;
import com.zzyl.nursing.service.IAlertRuleService;
import com.zzyl.nursing.vo.AlertNotifyVo;
import com.zzyl.system.mapper.SysUserRoleMapper;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 报警规则Service业务层处理
 * 
 * @author chen
 * @date 2026-10-05
 */
@Service
@Slf4j
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleMapper, AlertRule> implements IAlertRuleService {
    @Autowired
    private AlertRuleMapper alertRuleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private IAlertDataService alertDataService;

    @Value("${alert.deviceMaintainerRole}")
    private String deviceMaintainerRole;

    @Value("${alert.managerRole}")
    private String managerRole;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 查询报警规则
     * 
     * @param id 报警规则主键
     * @return 报警规则
     */
    @Override
    public AlertRule selectAlertRuleById(Long id) {
        return getById(id);
    }

    /**
     * 查询报警规则列表
     * 
     * @param alertRule 报警规则
     * @return 报警规则
     */
    @Override
    public List<AlertRule> selectAlertRuleList(AlertRule alertRule) {
        return alertRuleMapper.selectAlertRuleList(alertRule);
    }

    /**
     * 新增报警规则
     * 
     * @param alertRule 报警规则
     * @return 结果
     */
    @Override
    public int insertAlertRule(AlertRule alertRule) {
        return save(alertRule) ? 1 : 0;
    }

    /**
     * 修改报警规则
     * 
     * @param alertRule 报警规则
     * @return 结果
     */
    @Override
    public int updateAlertRule(AlertRule alertRule) {
        return updateById(alertRule) ? 1 : 0;
    }

    /**
     * 批量删除报警规则
     * 
     * @param ids 需要删除的报警规则主键
     * @return 结果
     */
    @Override
    public int deleteAlertRuleByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除报警规则信息
     * 
     * @param id 报警规则主键
     * @return 结果
     */
    @Override
    public int deleteAlertRuleById(Long id) {
        return removeById(id) ? 1 : 0;
    }

    /**
     * 报警过滤
     */
    @Override
    public void alertFilter() {
        // 查询所有规则，遍历规则
        long count = count(Wrappers.<AlertRule>lambdaQuery().eq(AlertRule::getStatus, 1));
        if (count <= 0) {
            return;
        }
        // 查询所有上报的数据
        List<Object> values = redisTemplate.opsForHash().values("iot:device_last_data");
        if (CollUtil.isEmpty(values)) {
            return;
        }
        // 解析上报的数据
        List<DeviceData> deviceDatas = new ArrayList<>();
        values.forEach(v -> deviceDatas.addAll(JSONUtil.toList(v.toString(), DeviceData.class)));

        // 遍历报警数据，逐条处理
        deviceDatas.forEach(d -> alertFilter(d));
    }

    /**
     * 逐条过滤报警数据
     *
     * @param deviceData 设备数据
     */
    private void alertFilter(DeviceData deviceData) {
        // 判断当前上报的数据是否超过了1分钟
        LocalDateTime alarmTime = deviceData.getAlarmTime();
        long between = LocalDateTimeUtil.between(alarmTime, LocalDateTime.now(), ChronoUnit.SECONDS);
        if (between > 60) {
            return;
        }
        // 查询所有的该产品规则和该物模型的规则
        List<AlertRule> allRules = list(Wrappers.<AlertRule>lambdaQuery()
                .eq(AlertRule::getProductKey, deviceData.getProductKey())
                .eq(AlertRule::getIotId, "-1")
                .eq(AlertRule::getFunctionId, deviceData.getFunctionId())
                .eq(AlertRule::getStatus, 1));

        List<AlertRule> iotIdRules = list(Wrappers.<AlertRule>lambdaQuery()
                .eq(AlertRule::getProductKey, deviceData.getProductKey())
                .eq(AlertRule::getIotId, deviceData.getIotId())
                .eq(AlertRule::getFunctionId, deviceData.getFunctionId())
                .eq(AlertRule::getStatus, 1));
        // 合并
        Collection<AlertRule> allArertRules = CollUtil.addAll(allRules, iotIdRules);
        // 如果为空，则中断
        if (CollUtil.isEmpty(allArertRules)) {
            return;
        }
        // 按照过滤规则和上报的数据进行匹配
        allArertRules.forEach(alertRule -> deviceDataAlarmHandler(alertRule, deviceData));
    }

    /**
     * 过滤数据是否触发报警规则
     *
     * @param rule
     * @param deviceData
     */
    private void deviceDataAlarmHandler(AlertRule rule, DeviceData deviceData) {
        // 判断上报时间是否在规则的生效时段内 00:00:00~23:59:59
        String[] split = rule.getAlertEffectivePeriod().split("~");
        LocalTime startTime = LocalTime.parse(split[0]);
        LocalTime endTime = LocalTime.parse(split[1]);
        // 获取上报时间
        LocalTime time = LocalDateTimeUtil.of(deviceData.getAlarmTime()).toLocalTime();
        // 不在上报时间内，则结束请求
        if (time.isBefore(startTime) || time.isAfter(endTime)) {
            return;
        }
        // 获取IOTID
        String iotId = deviceData.getIotId();
        // 统计次数的key
        String aggCountKey = "iot:alert_trigger_count:" + iotId + ":" + deviceData.getFunctionId() + ":" + rule.getId();

        // 数据对比，上报的数据与规则中的阈值进行对比
        // 两个参数x,y(参数有顺序要求，左边是上报的数据，后边是规则的数据) x==y 返回0 x>y 返回大于0 x<y 返回小于0的数值
        int compare = NumberUtil.compare(Double.valueOf(deviceData.getDataValue()), rule.getValue());
        if ((rule.getOperator().equals(">=") && compare >= 0) || (rule.getOperator().equals("<") && compare < 0)) {
            log.info("当前上报的数据符合规则异常");
        } else {
            // 正常的数据
            redisTemplate.delete(aggCountKey);
            return;
        }
        // 异常的数据会走到这里
        // 判断是否在沉默周期内
        String silentKey = "iot:alert_silent:" + iotId + ":" + deviceData.getFunctionId() + ":" + rule.getId();
        String silentData = redisTemplate.opsForValue().get(silentKey);
        if (StringUtils.isNotEmpty(silentData)) {
            return;
        }
        // 持续周期的逻辑
        String aggData = redisTemplate.opsForValue().get(aggCountKey);
        int count = StringUtils.isEmpty(aggData) ? 1 : Integer.parseInt(aggData) + 1;
        // 如果count与持续周期的值相等，则触发报警
        if (ObjectUtil.notEqual(count, rule.getDuration())) {
            // 不相等
            redisTemplate.opsForValue().set(aggCountKey, count + "");
            return;
        }
        // 删除redis的报警数据
        redisTemplate.delete(aggCountKey);
        // 存储数据到沉默周期，设置一个过期时间，规则中的沉默周期
        redisTemplate.opsForValue().set(silentKey, "1", rule.getAlertSilentPeriod(), TimeUnit.MINUTES);

        // 报警数据，需要找到对应的人
        List<Long> userIds = new ArrayList<>();
        if (rule.getAlertDataType().equals(0)) {
            // 老人异常数据
            if (deviceData.getLocationType().equals(0)) {
                // 说明是报警手表，直接可以找到老人的id,通过老人id,找到对应的护理员
                userIds = deviceMapper.selectNursingIdsByIotIdWithElder(iotId);
            } else if (deviceData.getLocationType().equals(1) && deviceData.getPhysicalLocationType().equals(2)) {
                // 说明是床位设备，可以通过床位id找到老人，通过老人id,找到对应的护理员
                userIds = deviceMapper.selectNursingIdsByIotIdWithBed(iotId);
            }
        } else {
            // 设备异常数据，找维修工，或者是行政人员
            userIds = userRoleMapper.selectUserIdByRoleName(deviceMaintainerRole);
        }
        // 不论是哪种情况，都要通知超级管理员
        List<Long> managerIds = userRoleMapper.selectUserIdByRoleName(managerRole);
        Collection<Long> allUserIds = CollUtil.addAll(userIds, managerIds);
        // 去重
        allUserIds = CollUtil.distinct(allUserIds);

        // 保存报警数据
        List<AlertData> alertDataList = insertAlertData(allUserIds, rule, deviceData);

        // websocket推送消息
        webSocketNotity(alertDataList.get(0), rule, allUserIds);
    }

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * websocket推送消息
     * 
     * @param alertData
     * @param rule
     * @param allUserIds
     */
    private void webSocketNotity(AlertData alertData, AlertRule rule, Collection<Long> allUserIds) {

        // 属性拷贝
        AlertNotifyVo alertNotifyVo = BeanUtil.toBean(alertData, AlertNotifyVo.class);
        alertNotifyVo.setAccessLocation(alertData.getRemark());
        alertNotifyVo.setFunctionName(rule.getFunctionName());
        alertNotifyVo.setAlertDataType(rule.getAlertDataType());
        alertNotifyVo.setNotifyType(1);
        // 向指定的人推送消息
        webSocketServer.sendMessageToConsumer(alertNotifyVo, allUserIds);

    }

    /**
     * 保存报警数据
     *
     * @param allUserIds
     * @param rule
     * @param deviceData
     */
    private List<AlertData> insertAlertData(Collection<Long> allUserIds, AlertRule rule, DeviceData deviceData) {
        // 对象拷贝
        AlertData alertData = BeanUtil.toBean(deviceData, AlertData.class);
        alertData.setAlertRuleId(rule.getId());
        // 心率<60,持续3个周期就报警
        String alertReason = CharSequenceUtil.format("{}{}{},持续{}个周期就报警", rule.getFunctionName(), rule.getOperator(),
                rule.getValue(), rule.getDuration());
        alertData.setAlertReason(alertReason);
        alertData.setStatus(0);
        alertData.setType(rule.getAlertDataType());
        // 遍历allUserIds
        List<AlertData> list = allUserIds.stream().map(userId -> {
            AlertData dbAlertData = BeanUtil.toBean(alertData, AlertData.class);
            dbAlertData.setUserId(userId);
            dbAlertData.setId(null);
            return dbAlertData;
        }).collect(Collectors.toList());

        // 批量保存
        alertDataService.saveBatch(list);

        return list;

    }
}

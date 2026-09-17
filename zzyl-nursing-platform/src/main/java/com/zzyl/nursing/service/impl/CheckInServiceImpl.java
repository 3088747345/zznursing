package com.zzyl.nursing.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzyl.common.exception.base.BaseException;
import com.zzyl.common.utils.CodeGenerator;
import com.zzyl.common.utils.bean.BeanUtils;
import com.zzyl.nursing.domain.Bed;
import com.zzyl.nursing.domain.CheckIn;
import com.zzyl.nursing.domain.CheckInConfig;
import com.zzyl.nursing.domain.Contract;
import com.zzyl.nursing.domain.Elder;
import com.zzyl.nursing.dto.CheckInApplyDto;
import com.zzyl.nursing.dto.CheckInElderDto;
import com.zzyl.nursing.mapper.BedMapper;
import com.zzyl.nursing.mapper.CheckInConfigMapper;
import com.zzyl.nursing.mapper.CheckInMapper;
import com.zzyl.nursing.mapper.ContractMapper;
import com.zzyl.nursing.mapper.ElderMapper;
import com.zzyl.nursing.service.ICheckInService;
import com.zzyl.nursing.vo.CheckInConfigVo;
import com.zzyl.nursing.vo.CheckInDetailVo;
import com.zzyl.nursing.vo.CheckInElderVo;
import com.zzyl.nursing.vo.ElderFamilyVo;

import cn.hutool.core.util.ObjectUtil;

/**
 * 入住Service业务层处理
 * 
 * @author chen
 * @date 2026-09-17
 */
@Service
public class CheckInServiceImpl extends ServiceImpl<CheckInMapper, CheckIn> implements ICheckInService {
    @Autowired
    private CheckInMapper checkInMapper;

    @Autowired
    private ElderMapper elderMapper;

    @Autowired
    private BedMapper bedMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private CheckInConfigMapper checkInConfigMapper;

    /**
     * 查询入住
     * 
     * @param id 入住主键
     * @return 入住
     */
    @Override
    public CheckIn selectCheckInById(Long id) {
        return getById(id);
    }

    /**
     * 查询入住列表
     * 
     * @param checkIn 入住
     * @return 入住
     */
    @Override
    public List<CheckIn> selectCheckInList(CheckIn checkIn) {
        return checkInMapper.selectCheckInList(checkIn);
    }

    /**
     * 新增入住
     * 
     * @param checkIn 入住
     * @return 结果
     */
    @Override
    public int insertCheckIn(CheckIn checkIn) {
        return save(checkIn) ? 1 : 0;
    }

    /**
     * 修改入住
     * 
     * @param checkIn 入住
     * @return 结果
     */
    @Override
    public int updateCheckIn(CheckIn checkIn) {
        return updateById(checkIn) ? 1 : 0;
    }

    /**
     * 批量删除入住
     * 
     * @param ids 需要删除的入住主键
     * @return 结果
     */
    @Override
    public int deleteCheckInByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除入住信息
     * 
     * @param id 入住主键
     * @return 结果
     */
    @Override
    public int deleteCheckInById(Long id) {
        return removeById(id) ? 1 : 0;
    }

    /**
     * 申请入住
     *
     * @param checkInApplyDto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(CheckInApplyDto checkInApplyDto) {
        // 判断老人是否已经入住
        // 通过身份证号查询老人
        LambdaQueryWrapper<Elder> elderQueryWrapper = new LambdaQueryWrapper<>();
        elderQueryWrapper.eq(Elder::getIdCardNo, checkInApplyDto.getCheckInElderDto().getIdCardNo());
        elderQueryWrapper.eq(Elder::getStatus, 1);
        Elder elder = elderMapper.selectOne(elderQueryWrapper);
        if (ObjectUtil.isNotEmpty(elder)) {
            throw new BaseException("老人已入住");
        }

        // 更新床位的状态 已入住
        Bed bed = bedMapper.selectById(checkInApplyDto.getCheckInConfigDto().getBedId());
        bed.setBedStatus(1);
        bedMapper.updateById(bed);

        // 保存或更新老人数据
        elder = insertOrUpdateElder(bed, checkInApplyDto.getCheckInElderDto());

        // 生成合同编号
        String contractNo = "HT" + CodeGenerator.generateContractNumber();

        // 新增签约办理
        insertContract(contractNo, elder, checkInApplyDto);

        // 新增入住信息
        CheckIn checkIn = insertCheckIn(elder, checkInApplyDto);

        // 新增入住配置信息
        insertCheckInConfig(checkIn.getId(), checkInApplyDto);
    }

    /**
     * 新增入住配置
     * 
     * @param checkInApplyDto
     */
    private void insertCheckInConfig(Long checkInId, CheckInApplyDto checkInApplyDto) {
        CheckInConfig checkInConfig = new CheckInConfig();
        BeanUtils.copyProperties(checkInApplyDto.getCheckInConfigDto(), checkInConfig);
        checkInConfig.setCheckInId(checkInId);
        checkInConfigMapper.insert(checkInConfig);
    }

    /**
     * 新增入住信息
     * 
     * @param elder
     * @param checkInApplyDto
     */
    private CheckIn insertCheckIn(Elder elder, CheckInApplyDto checkInApplyDto) {
        CheckIn checkIn = new CheckIn();
        checkIn.setElderId(elder.getId());
        checkIn.setElderName(elder.getName());
        checkIn.setIdCardNo(elder.getIdCardNo());
        checkIn.setNursingLevelName(checkInApplyDto.getCheckInConfigDto().getNursingLevelName());
        checkIn.setStartDate(checkInApplyDto.getCheckInConfigDto().getStartDate());
        checkIn.setEndDate(checkInApplyDto.getCheckInConfigDto().getEndDate());
        checkIn.setBedNumber(elder.getBedNumber());
        checkIn.setRemark(JSON.toJSONString(checkInApplyDto.getElderFamilyDtoList()));
        checkIn.setStatus(0);
        checkInMapper.insert(checkIn);
        return checkIn;
    }

    /**
     * 新增合同
     * 
     * @param contractNo
     * @param elder
     * @param checkInApplyDto
     */
    private void insertContract(String contractNo, Elder elder, CheckInApplyDto checkInApplyDto) {

        Contract contract = new Contract();
        // 属性拷贝
        BeanUtils.copyProperties(checkInApplyDto.getCheckInContractDto(), contract);
        contract.setContractNumber(contractNo);
        contract.setElderId(elder.getId());
        contract.setElderName(elder.getName());
        // 状态、开始时间、结束时间
        // 签约时间小于等于当前时间，合同生效中
        LocalDateTime checkInStartTime = checkInApplyDto.getCheckInConfigDto().getStartDate();
        LocalDateTime checkInEndTime = checkInApplyDto.getCheckInConfigDto().getEndDate();
        Integer status = checkInStartTime.isAfter(LocalDateTime.now()) ? 0 : 1;
        contract.setStatus(status);
        contract.setStartDate(checkInStartTime);
        contract.setEndDate(checkInEndTime);
        contractMapper.insert(contract);
    }

    /**
     * 新增或更新老人
     * 
     * @param bed
     * @param checkInElderDto
     * @return
     */
    private Elder insertOrUpdateElder(Bed bed, CheckInElderDto checkInElderDto) {
        // 准备一个Elder对象
        Elder elder = new Elder();
        // 属性拷贝
        BeanUtils.copyProperties(checkInElderDto, elder);
        elder.setBedId(bed.getId());
        elder.setBedNumber(bed.getBedNumber());
        elder.setStatus(1);
        // 查询老人信息
        LambdaQueryWrapper<Elder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Elder::getIdCardNo, elder.getIdCardNo());
        lambdaQueryWrapper.notIn(Elder::getStatus, 1, 4);
        Elder elderInDb = elderMapper.selectOne(lambdaQueryWrapper);
        if (ObjectUtil.isNotEmpty(elderInDb)) {
            // 修改
            elder.setId(elderInDb.getId());
            elderMapper.updateById(elder);
        } else {
            // 新增
            elderMapper.insert(elder);
        }
        return elder;
    }

    /**
     * 查询入住详情
     */
    @Override
    public CheckInDetailVo detail(Long id) {

        CheckInDetailVo checkInDetailVo = new CheckInDetailVo();
        // 1.查询老人信息
        // 根据入住id查询老人id
        LambdaQueryWrapper<CheckIn> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(CheckIn::getId, id);
        CheckIn checkIn = checkInMapper.selectOne(lambdaQueryWrapper);
        Long elderId = checkIn.getElderId();
        Elder elder = elderMapper.selectById(elderId);
        CheckInElderVo checkInElderVo = new CheckInElderVo();
        BeanUtils.copyProperties(elder, checkInElderVo);
        // 根据elder表的出生日期设置年龄
        String birthdayStr = elder.getBirthday();
        if (birthdayStr != null && !birthdayStr.isEmpty()) {
            LocalDate birthday = LocalDate.parse(birthdayStr);
            int age = Period.between(birthday, LocalDate.now()).getYears();
            checkInElderVo.setAge(age);
        }
        checkInDetailVo.setCheckInElderVo(checkInElderVo);

        // 2.查询家属信息
        // 使用checkIn的remark字段
        String remark = checkIn.getRemark();
        if (remark != null && !remark.isEmpty()) {
            List<ElderFamilyVo> elderFamilies = JSON.parseArray(remark, ElderFamilyVo.class);
            checkInDetailVo.setElderFamilyVoList(elderFamilies);
        }

        // 3.查询入住配置
        CheckInConfigVo checkInConfigVo = new CheckInConfigVo();
        checkInConfigVo.setStartDate(checkIn.getStartDate());
        checkInConfigVo.setEndDate(checkIn.getEndDate());
        checkInConfigVo.setBedNumber(elder.getBedNumber());
        checkInDetailVo.setCheckInConfigVo(checkInConfigVo);

        // 4.查询合同信息
        LambdaQueryWrapper<Contract> contractLambdaQueryWrapper = new LambdaQueryWrapper<>();
        contractLambdaQueryWrapper.eq(Contract::getElderId, elderId);
        Contract contract = contractMapper.selectOne(contractLambdaQueryWrapper);
        checkInDetailVo.setContract(contract);

        // 5.拼接返回对象
        return checkInDetailVo;
    }
}

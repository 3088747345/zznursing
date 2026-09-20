package com.zzyl.nursing.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zzyl.nursing.service.IContractService;

import lombok.extern.slf4j.Slf4j;

@Component("contractTask")
@Slf4j
public class ContractTask {

    @Autowired
    private IContractService contractService;

    public void updateContractStatusTask() {
        contractService.updateContractStatus();
        log.info("定时更新合同状态成功！");
    }
}

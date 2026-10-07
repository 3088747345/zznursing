package com.zzyl.nursing.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zzyl.nursing.service.IAlertRuleService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AlertTask {

    @Autowired
    private IAlertRuleService alertRuleService;

    public void deviceDataAlertFilter() {
        alertRuleService.alertFilter();
    }
}
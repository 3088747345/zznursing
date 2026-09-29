package com.zzyl.framework.interceptor;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.zzyl.common.core.domain.model.LoginUser;
import com.zzyl.common.utils.SecurityUtils;

import lombok.SneakyThrows;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Autowired
    private HttpServletRequest request;

    @SneakyThrows
    public boolean isExclude() {
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/member")) {
            return true;
        }
        return false;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        if (!isExclude()) {
            this.strictInsertFill(metaObject, "createBy", String.class, loadUserId() + "");
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updateTime", new Date(), metaObject);
        if (!isExclude()) {
            this.setFieldValByName("updateBy", loadUserId() + "", metaObject);
        }
        // this.strictInsertFill(metaObject, "updateBy", String.class,
        // String.valueOf(getLoginUserId()));
        // this.strictUpdateFill(metaObject, "updateTime", Date.class,
        // DateUtils.getNowDate());
    }

    public Long loadUserId() {
        // 获取到当前登录人的信息
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (ObjectUtils.isNotEmpty(loginUser)) {
            return loginUser.getUserId();
        }
        return 1L;
    }

}

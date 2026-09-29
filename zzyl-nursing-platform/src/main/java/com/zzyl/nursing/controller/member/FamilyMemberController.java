package com.zzyl.nursing.controller.member;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zzyl.common.core.controller.BaseController;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.nursing.dto.UserLoginRequestDto;
import com.zzyl.nursing.service.IFamilyMemberService;
import com.zzyl.nursing.vo.LoginVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 老人家属Controller
 * 
 * @author chen
 * @date 2026-09-29
 */
@Api("老人家属管理")
@RestController
@RequestMapping("/member/user")
public class FamilyMemberController extends BaseController {
    @Autowired
    private IFamilyMemberService familyMemberService;

    @PostMapping("/login")
    @ApiOperation("小程序登录")
    public AjaxResult login(@RequestBody UserLoginRequestDto userLoginRequestDto) {
        LoginVo loginVo = familyMemberService.login(userLoginRequestDto);
        return success(loginVo);
    }
}

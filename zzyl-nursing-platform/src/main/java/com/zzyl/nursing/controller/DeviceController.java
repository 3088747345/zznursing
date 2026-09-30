package com.zzyl.nursing.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zzyl.common.annotation.Log;
import com.zzyl.common.core.controller.BaseController;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.common.enums.BusinessType;
import com.zzyl.common.utils.poi.ExcelUtil;
import com.zzyl.nursing.domain.Device;
import com.zzyl.nursing.dto.DeviceDto;
import com.zzyl.nursing.service.IDeviceService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 设备Controller
 * 
 * @author chen
 * @date 2026-09-30
 */
@Api("设备管理")
@RestController
@RequestMapping("/nursing/device")
public class DeviceController extends BaseController {
    @Autowired
    private IDeviceService deviceService;

    /**
     * 查询设备列表
     */
    @ApiOperation("查询设备列表")
    @PreAuthorize("@ss.hasPermi('nursing:device:list')")
    @GetMapping("/list")
    public TableDataInfo<List<Device>> list(@ApiParam("查询条件对象") Device device) {
        startPage();
        List<Device> list = deviceService.selectDeviceList(device);
        return getDataTable(list);
    }

    /**
     * 导出设备列表
     */
    @ApiOperation("导出设备列表")
    @PreAuthorize("@ss.hasPermi('nursing:device:export')")
    @Log(title = "设备", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@ApiParam("导出的查询条件") HttpServletResponse response, Device device) {
        List<Device> list = deviceService.selectDeviceList(device);
        ExcelUtil<Device> util = new ExcelUtil<Device>(Device.class);
        util.exportExcel(response, list, "设备数据");
    }

    // /**
    // * 获取设备详细信息
    // */
    // @ApiOperation("获取设备详细信息")
    // @PreAuthorize("@ss.hasPermi('nursing:device:query')")
    // @GetMapping(value = "/{id}")
    // public R<Device> getInfo(@PathVariable("id") @ApiParam("设备ID") Long id) {
    // return R.ok(deviceService.selectDeviceById(id));
    // }

    /**
     * 新增设备
     */
    @ApiOperation("新增设备")
    @PreAuthorize("@ss.hasPermi('nursing:device:add')")
    @Log(title = "设备", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody @ApiParam("新增的设备对象") Device device) {
        return toAjax(deviceService.insertDevice(device));
    }

    // /**
    // * 修改设备
    // */
    // @ApiOperation("修改设备")
    // @PreAuthorize("@ss.hasPermi('nursing:device:edit')")
    // @Log(title = "设备", businessType = BusinessType.UPDATE)
    // @PutMapping
    // public AjaxResult edit(@RequestBody @ApiParam("修改的设备对象") Device device) {
    // return toAjax(deviceService.updateDevice(device));
    // }

    // /**
    // * 删除设备
    // */
    // @ApiOperation("删除设备")
    // @PreAuthorize("@ss.hasPermi('nursing:device:remove')")
    // @Log(title = "设备", businessType = BusinessType.DELETE)
    // @DeleteMapping("/{ids}")
    // public AjaxResult remove(@PathVariable @ApiParam("要删除的设备ID") Long[] ids) {
    // return toAjax(deviceService.deleteDeviceByIds(ids));
    // }

    /**
     * 从物联网平台同步产品列表
     * 
     * @return
     */
    @PostMapping("/syncProductList")
    @ApiOperation(value = "从物联网平台同步产品列表")
    public AjaxResult syncProductList() {
        deviceService.syncProductList();
        return success();
    }

    /**
     * 查询所有产品列表
     */
    @GetMapping("/allProduct")
    @ApiOperation(value = "查询所有产品列表")
    public AjaxResult allProduct() {
        return success(deviceService.allProduct());
    }

    /*
     * 注册设备
     */
    @PostMapping("/register")
    @ApiOperation(value = "注册设备")
    public AjaxResult registerDevice(@RequestBody DeviceDto deviceDto) {
        deviceService.registerDevice(deviceDto);
        return success();
    }

    /**
     * 获取设备详细信息
     */
    @GetMapping("/{iotId}")
    @ApiOperation("获取设备详细信息")
    public AjaxResult getInfo(@PathVariable("iotId") String iotId) {
        return success(deviceService.queryDeviceDetail(iotId));
    }

    /**
     * 查询设备上报数据
     */
    @GetMapping("/queryServiceProperties/{iotId}")
    @ApiOperation("查询设备上报数据")
    public AjaxResult queryServiceProperties(@PathVariable("iotId") String iotId) {
        AjaxResult ajaxResult = deviceService.queryServiceProperties(iotId);
        return ajaxResult;
    }

    /**
     * 修改设备
     */
    @ApiOperation("修改设备")
    @PreAuthorize("@ss.hasPermi('nursing:device:edit')")
    @Log(title = "设备", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody @ApiParam("修改的设备对象") DeviceDto device) {
        deviceService.updateDevice1(device);
        return success();
    }

    /**
     * 删除设备
     */
    @ApiOperation("删除设备")
    @PreAuthorize("@ss.hasPermi('nursing:device:remove')")
    @Log(title = "设备", businessType = BusinessType.DELETE)
    @DeleteMapping("/{iotId}")
    public AjaxResult remove(@PathVariable @ApiParam("要删除的设备ID") String iotId) {
        deviceService.deleteDeviceByIotId(iotId);
        return success();
    }
}

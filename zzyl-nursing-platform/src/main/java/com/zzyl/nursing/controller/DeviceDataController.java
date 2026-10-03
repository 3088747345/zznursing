package com.zzyl.nursing.controller;

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
import com.zzyl.common.core.domain.R;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.common.enums.BusinessType;
import com.zzyl.nursing.domain.DeviceData;
import com.zzyl.nursing.dto.DeviceDataPageReqDto;
import com.zzyl.nursing.service.IDeviceDataService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 设备数据Controller
 * 
 * @author chen
 * @date 2026-10-02
 */
@Api("设备数据管理")
@RestController
@RequestMapping("/nursing/data")
public class DeviceDataController extends BaseController {
    @Autowired
    private IDeviceDataService deviceDataService;

    /**
     * 查询设备数据列表
     */
    @PreAuthorize("@ss.hasPermi('elder:data:list')")
    @GetMapping("/list")
    @ApiOperation("查询设备数据列表")
    public TableDataInfo list(DeviceDataPageReqDto deviceDataPageReqDto) {
        return deviceDataService.selectDeviceDataList(deviceDataPageReqDto);
    }

    // /**
    // * 导出设备数据列表
    // */
    // @ApiOperation("导出设备数据列表")
    // @PreAuthorize("@ss.hasPermi('nursing:data:export')")
    // @Log(title = "设备数据", businessType = BusinessType.EXPORT)
    // @PostMapping("/export")
    // public void export(@ApiParam("导出的查询条件") HttpServletResponse response,
    // DeviceData deviceData)
    // {
    // List<DeviceData> list = deviceDataService.selectDeviceDataList(deviceData);
    // ExcelUtil<DeviceData> util = new ExcelUtil<DeviceData>(DeviceData.class);
    // util.exportExcel(response, list, "设备数据数据");
    // }

    /**
     * 获取设备数据详细信息
     */
    @ApiOperation("获取设备数据详细信息")
    @PreAuthorize("@ss.hasPermi('nursing:data:query')")
    @GetMapping(value = "/{id}")
    public R<DeviceData> getInfo(@PathVariable("id") @ApiParam("设备数据ID") Long id) {
        return R.ok(deviceDataService.selectDeviceDataById(id));
    }

    /**
     * 新增设备数据
     */
    @ApiOperation("新增设备数据")
    @PreAuthorize("@ss.hasPermi('nursing:data:add')")
    @Log(title = "设备数据", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody @ApiParam("新增的设备数据对象") DeviceData deviceData) {
        return toAjax(deviceDataService.insertDeviceData(deviceData));
    }

    /**
     * 修改设备数据
     */
    @ApiOperation("修改设备数据")
    @PreAuthorize("@ss.hasPermi('nursing:data:edit')")
    @Log(title = "设备数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody @ApiParam("修改的设备数据对象") DeviceData deviceData) {
        return toAjax(deviceDataService.updateDeviceData(deviceData));
    }

    /**
     * 删除设备数据
     */
    @ApiOperation("删除设备数据")
    @PreAuthorize("@ss.hasPermi('nursing:data:remove')")
    @Log(title = "设备数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable @ApiParam("要删除的设备数据ID") Long[] ids) {
        return toAjax(deviceDataService.deleteDeviceDataByIds(ids));
    }
}

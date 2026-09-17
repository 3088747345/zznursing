package com.zzyl.nursing.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzyl.nursing.domain.Room;
import com.zzyl.nursing.vo.RoomVo;

/**
 * 房间Service接口
 *
 * @author ruoyi
 * @date 2024-04-26
 */
public interface IRoomService extends IService<Room> {
    /**
     * 查询房间
     *
     * @param id 房间主键
     * @return 房间
     */
    public Room selectRoomById(Long id);

    /**
     * 查询房间列表
     *
     * @param room 房间
     * @return 房间集合
     */
    public List<Room> selectRoomList(Room room);

    /**
     * 新增房间
     *
     * @param room 房间
     * @return 结果
     */
    public int insertRoom(Room room);

    /**
     * 修改房间
     *
     * @param room 房间
     * @return 结果
     */
    public int updateRoom(Room room);

    /**
     * 批量删除房间
     *
     * @param ids 需要删除的房间主键集合
     * @return 结果
     */
    public int deleteRoomByIds(Long[] ids);

    /**
     * 根据楼层 id 获取房间视图对象列表
     * 
     * @param floorId
     * @return
     */
    List<RoomVo> getRoomsByFloorId(Long floorId);

    /**
     * 获取所有房间（负责老人）
     * 
     * @param floorId
     * @return
     */
    List<RoomVo> getRoomsWithNurByFloorId(Long floorId);

    /**
     * 按照房间id查询楼层、房间、价格
     * 
     * @param id
     * @return
     */
    public RoomVo getRoomById(Long id);
}

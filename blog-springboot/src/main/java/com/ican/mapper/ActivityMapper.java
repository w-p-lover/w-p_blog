package com.ican.mapper;

import com.ican.entity.Activity;
import com.ican.entity.ActivityRegistration;
import com.ican.model.dto.ActivitySaveDTO;
import com.ican.model.vo.ActivityVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityMapper {
    int insertActivity(@Param("activity") Activity activity);
    int updateBeforeStart(@Param("id") Integer id, @Param("dto") ActivitySaveDTO dto);
    int publish(@Param("id") Integer id);
    Activity selectActivity(@Param("id") Integer id);
    List<ActivityVO> listActivities(@Param("admin") boolean admin, @Param("offset") int offset, @Param("size") int size);
    Long countActivities(@Param("admin") boolean admin);
    int insertRegistration(@Param("requestId") String requestId, @Param("activityId") Integer activityId,
                           @Param("userId") Integer userId);
    ActivityRegistration lockRequest(@Param("requestId") String requestId);
    ActivityRegistration lockUserRequest(@Param("activityId") Integer activityId, @Param("userId") Integer userId);
    ActivityRegistration findMine(@Param("activityId") Integer activityId, @Param("userId") Integer userId);
    ActivityRegistration lockDispatched(@Param("requestId") String requestId, @Param("token") String token);
    List<com.ican.model.vo.AdminActivityRegistrationVO> listRegistrations(@Param("activityId") Integer activityId,
            @Param("status") String status, @Param("offset") int offset, @Param("size") int size);
    Long countRegistrations(@Param("activityId") Integer activityId, @Param("status") String status);
    int reserveAdmission(@Param("id") Integer id);
    int releasePending(@Param("id") Integer id);
    int consumeCapacity(@Param("id") Integer id);
    int restoreCapacity(@Param("id") Integer id);
    int changeStatus(@Param("requestId") String requestId, @Param("from") String from, @Param("to") String to);
    List<String> selectDue(@Param("size") int size);
    int reserveDispatch(@Param("requestId") String requestId, @Param("token") String token);
    int failDispatch(@Param("requestId") String requestId, @Param("token") String token,
                     @Param("terminal") boolean terminal);
    int reserveRetry(@Param("id") Integer id);
    int retryFailed(@Param("requestId") String requestId);
}

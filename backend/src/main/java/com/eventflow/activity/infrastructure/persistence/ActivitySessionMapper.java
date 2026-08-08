package com.eventflow.activity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivitySessionMapper extends BaseMapper<ActivitySession> {
    @Update(
            """
            UPDATE ef_activity_session
            SET available_quota = available_quota - 1,
                confirmed_quota = confirmed_quota + 1
            WHERE id = #{sessionId}
              AND activity_id = #{activityId}
              AND status = 'ACTIVE'
              AND available_quota > 0
            """)
    int confirmQuota(@Param("activityId") Long activityId, @Param("sessionId") Long sessionId);

    @Update(
            """
            UPDATE ef_activity_session
            SET available_quota = available_quota + 1,
                confirmed_quota = confirmed_quota - 1
            WHERE id = #{sessionId}
              AND activity_id = #{activityId}
              AND confirmed_quota > 0
              AND available_quota < total_quota
            """)
    int releaseConfirmedQuota(@Param("activityId") Long activityId, @Param("sessionId") Long sessionId);
}

package com.eventflow.registration.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityRegistrationMapper extends BaseMapper<ActivityRegistration> {
    @Update(
            """
            UPDATE ef_registration
            SET session_id = #{sessionId},
                status = 'CONFIRMED',
                update_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{registrationId}
              AND status = 'CANCELLED'
            """)
    int reactivate(@Param("registrationId") Long registrationId, @Param("sessionId") Long sessionId);

    @Update(
            """
            UPDATE ef_registration
            SET status = 'CANCELLED',
                update_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{registrationId}
              AND user_id = #{userId}
              AND status = 'CONFIRMED'
            """)
    int cancel(@Param("registrationId") Long registrationId, @Param("userId") Long userId);
}

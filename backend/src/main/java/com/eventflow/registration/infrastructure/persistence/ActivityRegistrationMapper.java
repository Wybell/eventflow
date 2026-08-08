package com.eventflow.registration.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eventflow.registration.domain.RegistrationStatus;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityRegistrationMapper extends BaseMapper<ActivityRegistration> {

    @Select(
            """
            <script>
            SELECT r.id,
                   r.activity_id,
                   r.session_id,
                   r.status,
                   u.display_name,
                   u.username,
                   u.mobile,
                   u.email,
                   s.title AS session_title,
                   r.create_time AS registration_time,
                   r.update_time
            FROM ef_registration r
            JOIN ef_user u ON u.id = r.user_id
            JOIN ef_activity_session s ON s.id = r.session_id
            WHERE r.activity_id = #{activityId}
              <if test="sessionId != null">AND r.session_id = #{sessionId}</if>
              <if test="status != null">AND r.status = #{status}</if>
              <if test="keyword != null and keyword != ''">
                AND (u.display_name LIKE CONCAT('%', #{keyword}, '%')
                     OR u.username LIKE CONCAT('%', #{keyword}, '%')
                     OR u.mobile LIKE CONCAT('%', #{keyword}, '%')
                     OR u.email LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            ORDER BY r.create_time DESC, r.id DESC
            LIMIT #{offset}, #{limit}
            </script>
            """)
    List<OrganizerRegistrationRow> findForOrganizer(
            @Param("activityId") Long activityId,
            @Param("sessionId") Long sessionId,
            @Param("status") RegistrationStatus status,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit);

    @Select(
            """
            <script>
            SELECT COUNT(*)
            FROM ef_registration r
            JOIN ef_user u ON u.id = r.user_id
            WHERE r.activity_id = #{activityId}
              <if test="sessionId != null">AND r.session_id = #{sessionId}</if>
              <if test="status != null">AND r.status = #{status}</if>
              <if test="keyword != null and keyword != ''">
                AND (u.display_name LIKE CONCAT('%', #{keyword}, '%')
                     OR u.username LIKE CONCAT('%', #{keyword}, '%')
                     OR u.mobile LIKE CONCAT('%', #{keyword}, '%')
                     OR u.email LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </script>
            """)
    long countForOrganizer(
            @Param("activityId") Long activityId,
            @Param("sessionId") Long sessionId,
            @Param("status") RegistrationStatus status,
            @Param("keyword") String keyword);

    @Select(
            """
            SELECT COUNT(*)
            FROM ef_registration
            WHERE activity_id = #{activityId}
              AND status = #{status}
            """)
    long countByActivityAndStatus(@Param("activityId") Long activityId, @Param("status") RegistrationStatus status);

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

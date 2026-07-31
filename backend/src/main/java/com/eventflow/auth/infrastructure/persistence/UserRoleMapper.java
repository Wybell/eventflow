package com.eventflow.auth.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserRoleMapper {

    @Select(
            """
            SELECT r.role_code
            FROM ef_user_role_rel user_role
            INNER JOIN ef_role r ON r.id = user_role.role_id
            WHERE user_role.user_id = #{userId}
              AND r.status = 'ACTIVE'
            ORDER BY r.role_code
            """)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);
}

package com.eventflow.auth.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    @Select(
            """
            SELECT id, username, password_hash, display_name, mobile, email, organization_id, status,
                   last_login_time, create_time, update_time
            FROM ef_user
            WHERE username = #{username}
            LIMIT 1
            """)
    UserAccount findByUsername(@Param("username") String username);

    @Update("UPDATE ef_user SET last_login_time = #{loginTime} WHERE id = #{id}")
    int updateLastLoginTime(@Param("id") Long id, @Param("loginTime") LocalDateTime loginTime);
}

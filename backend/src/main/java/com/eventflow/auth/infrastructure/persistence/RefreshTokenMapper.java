package com.eventflow.auth.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {

    @Select(
            """
            SELECT id, user_id, token_hash, expires_time, revoked_time, create_time, update_time
            FROM ef_refresh_token
            WHERE token_hash = #{tokenHash}
              AND revoked_time IS NULL
              AND expires_time > CURRENT_TIMESTAMP(3)
            LIMIT 1
            """)
    RefreshToken findActiveByTokenHash(@Param("tokenHash") String tokenHash);

    @Update(
            """
            UPDATE ef_refresh_token
            SET revoked_time = #{revokedTime}
            WHERE id = #{id}
              AND revoked_time IS NULL
            """)
    int revokeById(@Param("id") Long id, @Param("revokedTime") LocalDateTime revokedTime);

    @Update(
            """
            UPDATE ef_refresh_token
            SET revoked_time = #{revokedTime}
            WHERE user_id = #{userId}
              AND revoked_time IS NULL
            """)
    int revokeAllByUserId(@Param("userId") Long userId, @Param("revokedTime") LocalDateTime revokedTime);
}

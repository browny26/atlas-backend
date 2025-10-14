package com.back.atlas.repository;

import com.back.atlas.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUserIdAndExpiryDateAfter(Long userId, LocalDateTime date);

    List<PasswordResetToken> findByUserIdAndExpiryDateBefore(Long userId, LocalDateTime date);
}

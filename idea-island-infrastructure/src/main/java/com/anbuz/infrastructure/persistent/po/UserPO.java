package com.anbuz.infrastructure.persistent.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPO {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String passwordHash;
    private String nickname;
    private String avatarKey;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}

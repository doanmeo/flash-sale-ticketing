package com.DTPM.FlashSale.dto.response;

import com.DTPM.FlashSale.domain.enums.UserRole;
import com.DTPM.FlashSale.domain.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private UserRole role;
    private UserStatus status;
}

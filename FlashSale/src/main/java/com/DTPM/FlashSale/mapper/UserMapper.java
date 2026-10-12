package com.DTPM.FlashSale.mapper;

import com.DTPM.FlashSale.dto.request.RegisterRequest;
import com.DTPM.FlashSale.dto.response.UserResponse;
import com.DTPM.FlashSale.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", expression = "java(com.DTPM.FlashSale.domain.enums.UserStatus.ACTIVE)")
    @Mapping(target = "deletedAt", ignore = true)
    User toEntity(RegisterRequest request);

    UserResponse toUserResponse(User user);
}

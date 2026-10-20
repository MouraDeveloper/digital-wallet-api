package com.mouradeveloper.digitalwallet.mapper;

import com.mouradeveloper.digitalwallet.models.dto.AppUserDto;
import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AppUserMapper {

    AppUserDto.Response toResponse(AppUser appUser);
}

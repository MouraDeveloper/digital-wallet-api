package com.mouradeveloper.digitalwallet.mapper;

import com.mouradeveloper.digitalwallet.models.dto.WalletDto;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {
    @Mapping(source = "user.id", target = "userId")
    WalletDto.Response toResponse(Wallet wallet);
}

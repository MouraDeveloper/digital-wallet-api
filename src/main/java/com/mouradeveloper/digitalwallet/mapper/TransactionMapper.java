package com.mouradeveloper.digitalwallet.mapper;

import com.mouradeveloper.digitalwallet.models.dto.TransactionDto;
import com.mouradeveloper.digitalwallet.models.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "sourceWallet.id", target = "sourceWalletId")
    @Mapping(source = "destinationWallet.id", target = "destinationWalletId")
    TransactionDto.Response toResponse(Transaction transaction);

}

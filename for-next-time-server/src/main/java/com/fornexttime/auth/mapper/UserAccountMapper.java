package com.fornexttime.auth.mapper;

import com.fornexttime.auth.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAccountMapper {

    UserAccount findByUsername(String username);

    int insert(UserAccount userAccount);
}

/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 *  org.apache.ibatis.annotations.Param
 *  org.apache.ibatis.annotations.Select
 *  org.apache.ibatis.annotations.Update
 */
package com.example.waterfactoryceshi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.waterfactoryceshi.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper
extends BaseMapper<User> {
    @Select(value={"SELECT * FROM user WHERE username = #{username}"})
    public User findByUsername(String var1);

    @Update(value={"UPDATE user SET password = #{password} WHERE username = #{username}"})
    public void updatePassword(@Param(value="username") String var1, @Param(value="password") String var2);
}

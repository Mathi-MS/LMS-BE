package com.live.locationtracker.config;

import com.live.locationtracker.responseDto.UserContextDTO;

public class UserContextHolder {
    private UserContextHolder() {
        super();
    }

    private static final ThreadLocal<UserContextDTO> USER_CONTEXT = new ThreadLocal<>();

    public static void setUserDto(UserContextDTO userContextDTO) {
        USER_CONTEXT.set(userContextDTO);
    }

    public static UserContextDTO getUserDto() {
        return USER_CONTEXT.get();
    }

    public static void clear() {
        USER_CONTEXT.remove();
    }
}

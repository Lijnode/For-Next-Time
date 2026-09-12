package com.fornexttime.auth.res;

public record LoginRes(String token, String tokenType, long expiresIn, UserSimpleRes user) {
}

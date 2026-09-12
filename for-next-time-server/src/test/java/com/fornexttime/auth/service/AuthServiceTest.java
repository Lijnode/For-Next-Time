package com.fornexttime.auth.service;

import com.fornexttime.auth.entity.UserAccount;
import com.fornexttime.auth.mapper.UserAccountMapper;
import com.fornexttime.auth.req.LoginReq;
import com.fornexttime.auth.req.RegisterReq;
import com.fornexttime.auth.res.LoginRes;
import com.fornexttime.common.config.JwtProperties;
import com.fornexttime.common.exception.BusinessException;
import com.fornexttime.common.exception.ErrorCode;
import com.fornexttime.common.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountMapper userAccountMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private JwtProperties jwtProperties;
    @InjectMocks
    private AuthService authService;

    @Test
    void registersTrimmedUsernameHashesPasswordAndSignsIn() {
        when(passwordEncoder.encode("12345678")).thenReturn("bcrypt-hash");
        when(userAccountMapper.insert(any(UserAccount.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, UserAccount.class).setId(1L);
            return 1;
        });
        when(jwtUtil.createToken(1L, "new user")).thenReturn("jwt-token");
        when(jwtProperties.expirationSeconds()).thenReturn(604800L);

        LoginRes response = authService.register(new RegisterReq("  new user  ", "12345678", "12345678"));

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountMapper).insert(captor.capture());
        assertEquals("new user", captor.getValue().getUsername());
        assertEquals("bcrypt-hash", captor.getValue().getPasswordHash());
        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals("new user", response.user().username());
    }

    @Test
    void rejectsDuplicateUsernameAndMismatchedPasswordConfirmation() {
        when(userAccountMapper.findByUsername("existing")).thenReturn(new UserAccount());

        BusinessException duplicate = assertThrows(BusinessException.class,
                () -> authService.register(new RegisterReq("existing", "12345678", "12345678")));
        BusinessException mismatch = assertThrows(BusinessException.class,
                () -> authService.register(new RegisterReq("new-user", "12345678", "87654321")));

        assertEquals(ErrorCode.USERNAME_EXISTS, duplicate.getErrorCode());
        assertEquals(ErrorCode.PASSWORD_MISMATCH, mismatch.getErrorCode());
    }

    @Test
    void returnsTheSameErrorForUnknownUserAndWrongPassword() {
        when(userAccountMapper.findByUsername("unknown")).thenReturn(null);
        UserAccount userAccount = new UserAccount();
        userAccount.setPasswordHash("bcrypt-hash");
        when(userAccountMapper.findByUsername("known")).thenReturn(userAccount);
        when(passwordEncoder.matches(eq("wrong-password"), eq("bcrypt-hash"))).thenReturn(false);

        BusinessException unknown = assertThrows(BusinessException.class,
                () -> authService.login(new LoginReq("unknown", "wrong-password")));
        BusinessException wrongPassword = assertThrows(BusinessException.class,
                () -> authService.login(new LoginReq("known", "wrong-password")));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, unknown.getErrorCode());
        assertEquals(ErrorCode.INVALID_CREDENTIALS, wrongPassword.getErrorCode());
    }
}

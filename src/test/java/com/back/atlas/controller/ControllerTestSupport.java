package com.back.atlas.controller;

import com.back.atlas.config.CorsConfig;
import com.back.atlas.config.SecurityConfig;
import com.back.atlas.entity.User;
import com.back.atlas.repository.UserRepository;
import com.back.atlas.security.JwtService;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/** Loads the real security configuration, so the tests check the actual access rules. */
@Import({SecurityConfig.class, CorsConfig.class})
abstract class ControllerTestSupport {

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    UserRepository userRepository;

    static RequestPostProcessor loggedInAs(long userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("user" + userId + "@example.com");
        return authentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}

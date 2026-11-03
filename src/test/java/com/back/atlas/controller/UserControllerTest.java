package com.back.atlas.controller;

import com.back.atlas.dto.UserResponse;
import com.back.atlas.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest extends ControllerTestSupport {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService userService;

    @Test
    void readOwnProfile() throws Exception {
        when(userService.findById(7L)).thenReturn(new UserResponse());

        mvc.perform(get("/v1/api/user/7").with(loggedInAs(7))).andExpect(status().isOk());
    }

    @Test
    void cannotReadUpdateOrDeleteAnotherAccount() throws Exception {
        mvc.perform(get("/v1/api/user/8").with(loggedInAs(7))).andExpect(status().isForbidden());
        mvc.perform(put("/v1/api/user/8").with(loggedInAs(7))
                .contentType(MediaType.APPLICATION_JSON).content("{\"bio\": \"hacked\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/v1/api/user/8").with(loggedInAs(7))).andExpect(status().isForbidden());

        verify(userService, never()).updateUser(anyLong(), any());
        verify(userService, never()).deleteUser(anyLong());
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mvc.perform(get("/v1/api/user/7")).andExpect(status().isUnauthorized());
    }
}

package com.gg.server;

import com.gg.server.global.security.cookie.CookieUtil;
import com.gg.server.global.security.jwt.utils.TokenHeaders;
import com.gg.server.global.utils.ApplicationYmlRead;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final ApplicationYmlRead applicationYmlRead;
    @GetMapping("/user")
    public String testUser() {
        return "user check ok";
    }

    @GetMapping("/admin")
    public String testAdmin() {
        return "admin check ok";
    }

    @GetMapping("/test")
    public void testConnectOauth2Kakao(HttpServletResponse response,
            @RequestParam(defaultValue = "hello") String accessToken) throws IOException {
        System.out.println("accessToken = " + accessToken);
        CookieUtil.addCookie(response, TokenHeaders.ACCESS_TOKEN, accessToken, 1000000, "localhost");
        response.sendRedirect("http://localhost:8080/oauth2/authorization/kakao");
        //response.sendRedirect(applicationYmlRead.getDomain() + "/oauth2/authorization/kakao");
//        response.sendRedirect("localhost:8080/pingpong/users/oauth/kakao?accessToken=" +
//                accessToken + "&maxAge=" + 100000);
    }
}

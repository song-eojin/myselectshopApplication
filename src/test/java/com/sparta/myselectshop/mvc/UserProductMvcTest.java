package com.sparta.myselectshop.mvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.myselectshop.config.WebSecurityConfig;
import com.sparta.myselectshop.controller.ProductController;
import com.sparta.myselectshop.controller.UserController;
import com.sparta.myselectshop.dto.ProductRequestDto;
import com.sparta.myselectshop.entity.User;
import com.sparta.myselectshop.entity.UserRoleEnum;
import com.sparta.myselectshop.security.UserDetailsImpl;
import com.sparta.myselectshop.service.FolderService;
import com.sparta.myselectshop.service.KakaoService;
import com.sparta.myselectshop.service.ProductService;
import com.sparta.myselectshop.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import java.security.Principal;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest( //Controller 를 Test 하기 위한 애너테이션
        controllers = {UserController.class, ProductController.class},
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = WebSecurityConfig.class
                )
        }
)
class UserProductMvcTest {
    private MockMvc mvc;

    private Principal mockPrincipal;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean //실제로 필요한 빈들 대신 주입해주기 위해 가짜 빈 생성
    UserService userService;

    @MockBean
    KakaoService kakaoService;

    @MockBean
    ProductService productService;

    @MockBean
    FolderService folderService;

    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity(new MockSpringSecurityFilter()))
                .build();
    }

    //가짜 유저 객체 세팅
    private void mockUserSetup() {
        // Mock 테스트 유져 생성
        String username = "sollertia4351";
        String password = "robbie1234";
        String email = "sollertia@sparta.com";
        UserRoleEnum role = UserRoleEnum.USER;
        User testUser = new User(username, password, email, role);
        UserDetailsImpl testUserDetails = new UserDetailsImpl(testUser);
        mockPrincipal = new UsernamePasswordAuthenticationToken(testUserDetails, "", testUserDetails.getAuthorities());
    }

    @Test
    @DisplayName("로그인 Page")
    void test1() throws Exception {
        // when - then
        mvc.perform(get("/api/user/login-page"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andDo(print());
    }

    @Test
    @DisplayName("회원 가입 요청 처리")
    void test2() throws Exception {
        // given
        MultiValueMap<String, String> signupRequestForm = new LinkedMultiValueMap<>();
        signupRequestForm.add("username", "sollertia4351");
        signupRequestForm.add("password", "robbie1234");
        signupRequestForm.add("email", "sollertia@sparta.com");
        signupRequestForm.add("admin", "false");

        // when - then
        mvc.perform(post("/api/user/signup")
                        .params(signupRequestForm)
                )
                .andExpect(status().is3xxRedirection()) //예측
                .andExpect(view().name("redirect:/api/user/login-page"))
                .andDo(print());
    }

    @Test
    @DisplayName("신규 관심상품 등록")
    void test3() throws Exception {
        // given
        this.mockUserSetup();
        String title = "Apple <b>아이폰</b> 14 프로 256GB [자급제]";
        String imageUrl = "https://shopping-phinf.pstatic.net/main_3456175/34561756621.20220929142551.jpg";
        String linkUrl = "https://search.shopping.naver.com/gate.nhn?id=34561756621";
        int lPrice = 959000;
        ProductRequestDto requestDto = new ProductRequestDto(
                title,
                imageUrl,
                linkUrl,
                lPrice
        );

        String postInfo = objectMapper.writeValueAsString(requestDto);
        //writeValueAsString(): 클래스를 Json 타입 String 으로 변환시킴
        //서버에서는 Json 을 읽을 수 없기 때문에 원래는 클라이언트에서 json.stringfy 같은 것을 사용해서 보내준다. 클라이언트에서 이루어지는 처리를 흉내내기 위해 writeValueAsString() 메서드를 사용

        // when - then
        mvc.perform(post("/api/products")
                        .content(postInfo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .principal(mockPrincipal)
                )
                .andExpect(status().isOk())
                .andDo(print());
    }
}
package com.deepblue.service;

import com.deepblue.domain.Member;
import com.deepblue.repository.MemberRepository;
import com.deepblue.security.JwtTokenProvider;
import com.deepblue.security.TokenInfo;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;


/**
 * 회원과 관련된 로직을 처리해주는 서비스객체입니다.
 * @since : 2023-02-16
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final AuthenticationManagerBuilder managerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    //private int a = 1;

    /**
     * 전달받은 회원명의 회원이 DB에 있는지 확인 후 중복여부를 반환해줍니다.
     * @param username 검사할 회원명
     * @return 중복일 경우 false, 중복이 아닐 경우 true
     * @since : 2023-02-16
     * @lastModified : 2023-02-16
     */
    @Transactional(readOnly = true)
    public boolean isDuplicateUsername(String username){
        Optional<Member> findMember = memberRepository.findByUsername(username);

        if(findMember.isPresent())
            return true;
        else
            return false;
    }

    /**
     * 로그인을 시도하는 정보를 바탕으로 JWT토큰을 반환해줍니다.
     * @param username 로그인 시도 ID
     * @param password 로그인 시도 비밀번호
     * @return 액세스 토큰, 리프레쉬 토큰, 인가 타입이 담긴 DTO
     * @since : 2023-02-17
     * @lastModified : 2023-02-17
     */
    @Transactional(readOnly = true)
    public TokenInfo login(String username, String password){
        UsernamePasswordAuthenticationToken authenticationToken
                = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = managerBuilder.getObject().authenticate(authenticationToken);
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);
        return tokenInfo;
    }

    /**
     * 리프레쉬토큰을 검증 후 액세스 토큰을 재발급해줍니다.
     * @param refreshToken 검증할 리프레쉬토큰
     * @return 액세스 토큰, 리프레쉬 토큰, 인가 타입이 담긴 DTO
     * @since : 2023-02-17
     * @lastModified : 2023-02-17
     */
    public TokenInfo recreateAccessToken(String refreshToken){
        if(jwtTokenProvider.validateToken(refreshToken)){
            Claims claims = jwtTokenProvider.parseClaims(refreshToken);
            if(claims.get("sub") == null){
                throw new RuntimeException("잘못된 토큰입니다");
            }
            String username = claims.get("sub").toString();
            return jwtTokenProvider.recreateAccessToken(username, refreshToken);
        }else{
            throw new RuntimeException("잘못된 토큰입니다");
        }
    }
}

package com.deepblue.api;

import com.deepblue.domain.Member;
import com.deepblue.dto.MemberDataForm;
import com.deepblue.dto.ResponseDto;
import com.deepblue.repository.MemberRepository;
import com.deepblue.repository.RefreshTokenRepository;
import com.deepblue.security.MemberLoginRequestDto;
import com.deepblue.security.RefreshToken;
import com.deepblue.security.TokenInfo;
import com.deepblue.service.MemberService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.nio.charset.Charset;
import java.util.Optional;

/**
 * 회원과 관련된 API요청을 처리해주는 컨트롤러입니다.
 * @since 2023-02-16
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class MemberApiController {

    private final MemberService memberService;
    private final StringEncryptor encryptor;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 전달받은 회원명의 중복여부를 반환해줍니다.
     * @param username 검사할 회원명
     * @return 중복 일 경우 false, 중복이 아닐 경우 true
     * @since 2023-02-16
     * @lastModified 2023-02-16
     */
    @ApiOperation(value="회원명 중복체크", notes = "현재 작성한 회원명의 중복여부를 반환해줍니다.")
    @ApiImplicitParam(name = "username", value = "검사할 회원명")
    @PostMapping("/members/duplicate-check")
    public ResponseEntity<ResponseDto> duplicateCheck(@RequestBody String username){

        boolean isOK = !memberService.isDuplicateUsername(username); //중복여부 검사

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

        ResponseDto dto = new ResponseDto();
        dto.setData(isOK);

        if(isOK){
            dto.setMessage("사용가능한 회원명입니다");
            return new ResponseEntity<>(dto, headers, HttpStatus.OK);
        }else{
            dto.setMessage("이미 존재하는 회원명입니다");
            return new ResponseEntity<>(dto, headers, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 전달받은 회원정보로 회원가입을 진행합니다.
     * @param form 가입할 회원정보 데이터
     * @return 회원가입 성공 시 true, 실패할 경우 false
     * @since 2023-02-17
     * @lastModified 2023-02-17
     */
    @ApiOperation(value="회원가입 요청", notes = "전달받은 회원정보로 회원가입을 진행합니다.")
    @PostMapping("/members")
    public ResponseEntity<ResponseDto> signUp(@RequestBody @Valid MemberDataForm form){
        boolean isOK = !memberService.isDuplicateUsername(form.getUsername());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

        ResponseDto dto = new ResponseDto();
        dto.setData(isOK);

        if(isOK){ //중복이 아닐 경우
            Member member = new Member(form.getUsername(), passwordEncoder.encode(form.getPassword()));
            memberRepository.save(member);
            dto.setMessage("회원가입에 성공했습니다");
            return new ResponseEntity<>(dto, headers, HttpStatus.OK);
        }else{ //중복일 경우
            dto.setMessage("회원가입에 실패했습니다. 이미 존재하는 회원입니다");
            return new ResponseEntity<>(dto, headers, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 전달받은 회원정보로 로그인을 진행합니다.
     * @param form 가입할 회원정보 데이터
     * @return JWT 토큰정보 DTO
     * @since 2023-02-17
     * @lastModified 2023-02-18
     */
    @ApiOperation(value="로그인 요청", notes = "전달받은 회원정보로 로그인을 진행합니다. 로그인에 성공하면 JWT토큰을 발급해줍니다.")
    @PostMapping("/login")
    public ResponseEntity<ResponseDto> login(@RequestBody @Valid MemberDataForm form){

        String username = form.getUsername();
        String password = form.getPassword();

        TokenInfo tokenInfo = memberService.login(username, password); //토큰 정보 생성
        RefreshToken refreshToken = new RefreshToken(tokenInfo.getRefreshToken()); //리프레쉬토큰을 DB에 저장
        refreshTokenRepository.save(refreshToken);

        ResponseDto dto = new ResponseDto();
        dto.setMessage("성공적으로 로그인이 진행됐습니다");
        dto.setData(tokenInfo);

        return new ResponseEntity<>(dto,HttpStatus.OK);
    }

    /**
     * 리프레쉬 토큰을 받고 확인 후 액세스토큰을 재발급해줍니다.
     * @param refreshToken 사용자가 전달한 리프레쉬토큰
     * @return JWT 토큰 정보
     * @since 2023-02-18
     * @lastModified 2023-02-18
     */
    @ApiOperation(value="액세스 토큰 재발급 요청",
            notes = "클라이언트로부터 리프레쉬토큰을 받은 후 액세스토큰을 재발급해줍니다.")
    @PostMapping("/members/recreate-access")
    public ResponseEntity<ResponseDto> recreateAccessToken(@RequestBody String refreshToken){

        //유저한테 받은 리프레쉬토큰이 DB에 있는지 확인
        Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByDigest(refreshToken);

        ResponseDto dto = new ResponseDto();

        //리프레쉬토큰이 DB에 없을 경우
        if(!tokenOptional.isPresent()) {
            dto.setMessage("리프레쉬 토큰을 찾지 못했습니다");
            dto.setData(false);
            return new ResponseEntity<>(dto, HttpStatus.BAD_REQUEST);

        }else{

            try {

                RefreshToken findToken = tokenOptional.get();
                //리프레쉬토큰을 기반으로 새로운 액세스토큰을 생성해서 발급
                TokenInfo tokenInfo = memberService.recreateAccessToken(refreshToken);
                dto.setMessage("정상적으로 토큰이 재발급되었습니다");
                dto.setData(tokenInfo);
                return new ResponseEntity<>(dto, HttpStatus.OK);

            }catch (Exception e){
                dto.setMessage("리프레쉬토큰이 만료되었거나 잘못된 토큰입니다");
                dto.setData(false);
                return new ResponseEntity<>(dto, HttpStatus.BAD_REQUEST);
            }
        }

    }

    @PostMapping("/passtest")
    public String passTest(@RequestBody MemberLoginRequestDto dto){
        Optional<Member> op = memberRepository.findByUsername(dto.getId());
        if(!op.isPresent())
            return "없어용";
        else{
            Member member = op.get();
            if(passwordEncoder.matches(dto.getPassword(), member.getPassword()))
            {
                return "일치함";
            }else{
                return "불일치";
            }
        }
    }

    @PostMapping("/enc")
    public String enc(String plain){
        String resultText = encryptor.encrypt(plain);
        return resultText;
    }

    @PostMapping("/test")
    public String test(){
        return "test";
    }
}

package com.deepblue.api;

import com.deepblue.domain.Member;
import com.deepblue.dto.CreateMemberForm;
import com.deepblue.dto.ResponseDto;
import com.deepblue.repository.MemberRepository;
import com.deepblue.security.MemberLoginRequestDto;
import com.deepblue.security.TokenInfo;
import com.deepblue.service.MemberService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Map;

/**
 * 회원과 관련된 API요청을 처리해주는 컨트롤러입니다.
 * @since : 2023-02-16
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class MemberApiController {

    private final MemberService memberService;
    private final StringEncryptor encryptor;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    /**
     * 전달받은 회원명의 중복여부를 반환해줍니다.
     * @param username 검사할 회원명
     * @return 중복 일 경우 false, 중복이 아닐 경우 true
     * @since : 2023-02-16
     * @lastModified : 2023-02-16
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
     * @since : 2023-02-17
     * @lastModified : 2023-02-17
     */
    @ApiOperation(value="회원가입 요청", notes = "전달받은 회원정보로 회원가입을 진행합니다.")
    //@ApiImplicitParam(name = "form", value = "가입시킬 회원 정보")
    @PostMapping("/members")
    public ResponseEntity<ResponseDto> signUp(@RequestBody @Valid CreateMemberForm form){
        boolean isOK = !memberService.isDuplicateUsername(form.getUsername());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

        ResponseDto dto = new ResponseDto();
        dto.setData(isOK);

        if(isOK){
            Member member = new Member(form.getUsername(), passwordEncoder.encode(form.getPassword()));
            memberRepository.save(member);
            dto.setMessage("회원가입에 성공했습니다");
            return new ResponseEntity<>(dto, headers, HttpStatus.OK);
        }else{
            dto.setMessage("회원가입에 실패했습니다. 이미 존재하는 회원입니다");
            return new ResponseEntity<>(dto, headers, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public TokenInfo login(@RequestBody MemberLoginRequestDto memberLoginRequestDto){
        String id = memberLoginRequestDto.getId();
        String password = memberLoginRequestDto.getPassword();
        TokenInfo tokenInfo = memberService.login(id, password);
        return tokenInfo;
    }

    @PostMapping("/enc")
    public String enc(String plain){
        String resultText = encryptor.encrypt(plain);
        return resultText;
    }
}

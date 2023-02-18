package com.deepblue;


import com.deepblue.domain.Member;
import com.deepblue.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class InitRunner implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder encoder;
    @Override
    public void run(ApplicationArguments args) throws Exception {
        Member member = new Member("user", encoder.encode("1234"));
        memberRepository.save(member);
    }
}

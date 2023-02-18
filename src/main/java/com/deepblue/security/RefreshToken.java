package com.deepblue.security;


import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Entity
@NoArgsConstructor
public class RefreshToken {
    @Id @GeneratedValue
    @Column(name = "token_id")
    private Long id;
    @Column(length = 10000)
    private String digest;

    public RefreshToken(String digest) {
        this.digest = digest;
    }
}

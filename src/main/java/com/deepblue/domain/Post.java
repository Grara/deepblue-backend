package com.deepblue.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
@NoArgsConstructor
public class Post {

    @Id @GeneratedValue
    @Column(name = "post_id")
    private Long id;
    @Column(length = 20000)
    private String content;
    private int likes;

    public Post(String content) {
        this.content = content;
        this.likes = 0;
    }
}

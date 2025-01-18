package com.ll.SimpleDb;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class Article {

    private Long id;
    private String title;
    private String body;

    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    private boolean isBlind = false;

    //setIsBlind인식을 못해서 만듦
    public void setIsBlind(boolean isBlind) {
        this.isBlind = isBlind;
    }
}

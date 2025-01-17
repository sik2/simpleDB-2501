package com.ll.SimpleDb;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Article {
  private Long id;
  private String title;
  private String body;
  private LocalDateTime createdDate;
  private LocalDateTime modifiedDate;
  private boolean isBlind;
}

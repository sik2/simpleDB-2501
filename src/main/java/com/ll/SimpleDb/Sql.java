package com.ll.SimpleDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class Sql {
    private Connection connection;
    private StringBuilder query = new StringBuilder();
    private List<Object> params = new ArrayList<>();

    public Sql(Connection connection) {
        this.connection = connection;
    }

    public Sql append(String part, Object... args) {
        query.append(part).append(" ");
        for (Object arg : args) {
            params.add(arg);
        }
        return this;
    }
    public Sql appendIn(String part, Object... args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("args must not be null or empty");
        }

        // `IN` 절을 위한 부분 쿼리 만들기 (IN (?, ?, ?))
        query.append(part.split("\\?")[0]);

        // `?`를 생성하여 IN 절에 추가
        String placeholders = String.join(", ", Collections.nCopies(args.length, "?"));
        query.append(placeholders).append(")");
        System.out.println(query);
        // SQL 쿼리에서 파라미터로 추가할 값들을 params 리스트에 추가
        for (Object arg : args) {
            params.add(arg); // 각각의 파라미터 값을 params에 추가
        }

        return this;
    }



    public int delete() {
        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute delete query", e);
        }
    }

    public int insert() {
        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute insert query", e);
        }
    }

    public int update() {
        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute update query", e);
        }
    }

    public LocalDateTime selectDatetime() {
        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // 첫 번째 컬럼을 LocalDateTime으로 변환해서 반환
                    LocalDateTime result = rs.getObject(1, LocalDateTime.class);

                    // ZonedDateTime을 LocalDateTime으로 변환하여 반환
                    // DB에서 반환된 시간을 시스템의 로컬 시간대에 맞게 변환
                    ZonedDateTime dbZonedDateTime = result.atZone(ZoneId.of("UTC"));  // DB 서버가 UTC일 경우
                    ZonedDateTime localZonedDateTime = dbZonedDateTime.withZoneSameInstant(ZoneId.systemDefault());

                    // 변환된 ZonedDateTime을 LocalDateTime으로 변환
                    return localZonedDateTime.toLocalDateTime();
                } else {
                    throw new SQLException("No result found for the query.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute select query", e);
        }
    }



    public Long selectLong() {
        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // ResultSet에서 DATETIME 값을 가져와 LocalDateTime으로 변환
                    return rs.getObject(1, Long.class);  // 1은 첫 번째 컬럼을 의미
                } else {
                    throw new SQLException("No result found");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute update query", e);
        }
    }

    public List<Long> selectLongs() {
        List<Long> result = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            // 쿼리 파라미터 설정
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            // 실행 후 결과 처리
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getLong(1));  // 첫 번째 열의 값을 Long으로 가져옴
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute select query", e);
        }
        return result;
    }

    public String selectString() {
        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // ResultSet에서 DATETIME 값을 가져와 LocalDateTime으로 변환
                    return rs.getObject(1, String.class);  // 1은 첫 번째 컬럼을 의미
                } else {
                    throw new SQLException("No result found");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute update query", e);
        }
    }
    public Boolean selectBoolean() {
        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // ResultSet에서 DATETIME 값을 가져와 LocalDateTime으로 변환
                    return rs.getObject(1, Boolean.class);  // 1은 첫 번째 컬럼을 의미
                } else {
                    throw new SQLException("No result found");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute update query", e);
        }
    }

    public Article selectRow(Class<Article> articleClass) {
        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // ResultSet에서 각 컬럼을 가져와 Article 객체를 수동으로 생성
                    Article article = new Article();

                    article.setId(rs.getLong("id"));
                    article.setTitle(rs.getString("title"));
                    article.setBody(rs.getString("body"));
                    article.setCreatedDate(rs.getObject("createdDate", LocalDateTime.class));  // LocalDateTime으로 변환
                    article.setModifiedDate(rs.getObject("modifiedDate", LocalDateTime.class));  // LocalDateTime으로 변환
                    article.setIsBlind(rs.getBoolean("isBlind"));

                    return article;
                } else {
                    throw new SQLException("No result found");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute select query", e);
        }
    }


    public Map<String, Object> selectRow() {
        // 결과를 담을 Map을 생성
        Map<String, Object> result = new HashMap<>();

        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            // 파라미터 설정
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            // 쿼리 실행
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // ResultSet에서 결과를 가져와 Map에 추가
                    result.put("id", rs.getLong("id"));
                    result.put("title", rs.getString("title"));
                    result.put("body", rs.getString("body"));
                    result.put("createdDate", rs.getTimestamp("createdDate").toLocalDateTime());
                    result.put("modifiedDate", rs.getTimestamp("modifiedDate").toLocalDateTime());
                    result.put("isBlind", rs.getBoolean("isBlind"));
                    // 필요한 다른 필드를 Map에 추가...
                } else {
                    throw new SQLException("No result found");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute select query", e);
        }

        return result;  // Map 반환
    }


    public List<Map<String, Object>> selectRows() {
        List<Map<String, Object>> articles = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Map으로 결과를 변환
                    Map<String, Object> article = new HashMap<>();
                    article.put("id", rs.getLong("id"));
                    article.put("title", rs.getString("title"));
                    article.put("body", rs.getString("body"));
                    article.put("createdDate", rs.getTimestamp("createdDate").toLocalDateTime());
                    article.put("modifiedDate", rs.getTimestamp("modifiedDate").toLocalDateTime());
                    article.put("isBlind", rs.getBoolean("isBlind"));


                    articles.add(article);  // Map 객체를 리스트에 추가
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute select query", e);
        }

        return articles;  // Map 리스트 반환
    }

    public List<Article> selectRows(Class<Article> articleClass) {
        List<Article> articles = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Article 객체 생성
                    Article article = new Article();
                    article.setId(rs.getLong("id"));
                    article.setTitle(rs.getString("title"));
                    article.setBody(rs.getString("body"));
                    System.out.println(rs.getTimestamp("createdDate").toLocalDateTime());
                    article.setCreatedDate(rs.getTimestamp("createdDate").toLocalDateTime());
                    article.setModifiedDate(rs.getTimestamp("modifiedDate").toLocalDateTime());
                    article.setIsBlind(rs.getBoolean("isBlind"));

                    articles.add(article);  // Article 객체를 리스트에 추가
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute select query", e);
        }

        return articles;  // Article 리스트 반환
    }




}

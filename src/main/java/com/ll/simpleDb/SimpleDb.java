package com.ll.simpleDb;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Setter
public class SimpleDb {
    private final String host;
    private final String username;
    private final String password;
    private final String dbName;

    private final ConcurrentHashMap<Long, Connection> connections = new ConcurrentHashMap<>();
    private boolean devMode = false;


    private Connection getConnection() {
        Long threadId = Thread.currentThread().getId();

        return connections.computeIfAbsent(threadId, k -> {
            try {
                String url = "jdbc:mysql://" + host + "/" + dbName;
                Connection connection = DriverManager.getConnection(url, username, password);

                if (devMode) {
                    System.out.printf("Thread %d : DB 커넥션 성공!%n", threadId);
                }
                return connection;

            } catch (SQLException e) {
                String errorMsg = String.format("Thread %d : JDBC Connection 객체 준비 중 오류 발생! : %s",
                    threadId, e.getMessage());
                System.err.println(errorMsg);
                throw new RuntimeException(errorMsg, e);
            }
        });
    }

    public void run(String sql, Object... params) {
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();

        } catch (Exception e) {
            System.out.println("run() 메서드 호출 중 오류 발생 !");
        }
    }

    public Sql genSql() {
        return new Sql(getConnection());
    }

    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
            System.out.println("트랜잭션 시작!");

        } catch (SQLException e) {
            System.out.println("트랜잭션 시작 중 오류 발생!");
        }
    }

    public void rollback() {
        try {
            getConnection().rollback();
            System.out.println("트랜잭션 롤백!");

        } catch (SQLException e) {
            System.out.println("트랜잭션 롤백 중 오류 발생!");
        }
    }

    public void commit() {
        try {
            getConnection().commit();
            System.out.println("트랜잭션 커밋!");

        } catch (SQLException e) {
            System.out.println("트랜잭션 커밋 중 오류 발생!");
        }
    }

    public void close() {
        Long threadId = Thread.currentThread().getId();
        Connection connection = connections.remove(threadId);

        if (connection != null) {
            try {
                connection.close();
                if (devMode) {
                    System.out.printf("Thread %d : DB 커넥션 종료 성공%n", threadId);
                }
            } catch (SQLException e) {}
        }
    }
}

package com.ll.SimpleDb;


import com.ll.SimpleDb.Sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleDb {
    private final String host;
    private final String username;
    private final String password;
    private final String dbName;

    private Connection connection;
    private boolean devMode=false;

    public SimpleDb(String host, String username, String password, String dbName) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.dbName = dbName;
        connect();
    }

    private void connect(){
        try{
            String url = "jdbc:mysql://" + host + ":3306/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            connection= DriverManager.getConnection(url,username,password);
            if (devMode){
                System.out.println("Connected to the database"+ dbName);
            }
        }catch (SQLException e){
            throw new RuntimeException("Failed to connect to database");
        }
    }

    public void setDevMode(boolean devMode){
        this.devMode=devMode;
    }

    public void run(String query, Object... params) {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            statement.executeUpdate();
            if (devMode) {
                System.out.println("Executed query: " + query);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query: " + e.getMessage(), e);
        }
    }

    public Sql genSql() {
        return new Sql(connection);  // 데이터베이스 연결을 통해 Sql 객체 생성
    }

    // 트랜잭션 시작
    public void startTransaction() {
        try {
            connection.setAutoCommit(false);  // 자동 커밋 비활성화
        } catch (SQLException e) {
            throw new RuntimeException("Failed to start transaction", e);
        }
    }

    // 트랜잭션 롤백
    public void rollback() {
        try {
            connection.rollback();  // 트랜잭션 롤백
            connection.setAutoCommit(true);  // 자동 커밋 다시 활성화
        } catch (SQLException e) {
            throw new RuntimeException("Failed to rollback transaction", e);
        }
    }

    // 트랜잭션 커밋
    public void commit() {
        try {
            connection.commit();  // 트랜잭션 커밋
            connection.setAutoCommit(true);  // 자동 커밋 다시 활성화
        } catch (SQLException e) {
            throw new RuntimeException("Failed to commit transaction", e);
        }
    }

    public void close() {
        try {
            if (connection != null) {
                connection.commit();  // 트랜잭션 커밋
                connection.setAutoCommit(true);  // 자동 커밋 다시 활성화
                connection.close();  // 커넥션 종료
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to commit transaction and close connection", e);
        }
    }

}

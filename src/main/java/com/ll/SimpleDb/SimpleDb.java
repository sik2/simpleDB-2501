package com.ll.SimpleDb;

import lombok.RequiredArgsConstructor;

public class SimpleDb {
    private String url;
    private String username;
    private String password;
    private Connection con;
    private ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public SimpleDb(String url, String username, String password, String database) {
        this.url = String.format("jdbc:mysql://%s:3306/%s?serverTimezone=Asia/Seoul&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false", url, database);
        this.username = username;
        this.password = password;
    }

    private Connection getConnection() {
        try {
            if (con == null || con.isClosed()) {
                con = DriverManager.getConnection(url, username, password);
                connectionHolder.set(con);
            }
            return con;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setDevMode(boolean setting) {
        try {
            getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("DB 연결 실패");
        }
    }

    public void run(String sql, Object... params) {
        try {
            PreparedStatement stmt = getConnection().prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Sql genSql() {
        return new Sql(getConnection());
    }

    public void close() {
        Connection con = connectionHolder.get();
        if (con != null) {
            try {
                con.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                connectionHolder.remove();  // ThreadLocal에서 제거
            }
        }
    }

    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        try {
            Connection conn = getConnection();
            conn.rollback();
            conn.setAutoCommit(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            Connection conn = getConnection();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

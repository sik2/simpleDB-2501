package com.ll.SimpleDb;

import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleDb {
    private final ThreadLocal<Connection> connection;
    //Connection connection;
    private  boolean isDev;
    private final String url;
    private final String username;
    private final String password;
    private final String dbName;


    //후에는 타임존과 같은 파라미터도 입력 받도록..
    //기존 코드: 여기서 생성자로 생성할 때 한 번만 스레드 로컬에 등록했다.
    //참고 코드: 여기서 생성자로 생성할 때 말고도 스레드 로컬에 커넥션이 비어있거나 끊어져있으면 실행한다.
    public SimpleDb(String url, String username, String password, String dbName) {
        this.url=url;
        this.username=username;
        this.password=password;
        this.dbName=dbName;

        connection=new ThreadLocal<>();
        //System.out.println("디비 생성자 호출됨");
        getDBConnection();
        isDev=false;

    }

    private Connection getDBConnection(){
        String full_url="jdbc:mysql://"+url+":3306/"+dbName+"?serverTimezone=UTC";
        try {

            if(connection.get() == null || connection.get().isClosed()) {
                //System.out.println("스레드 로컬에 커넥션 세팅");
                //테스트 17을 돌려보면 여기가 11번 호출된다.
                //메인 스레드는 생성자를 통해 여기로 접근한다.
                //나머지 스레드도 어쨌든 SimpleDb를 통해서 genSql을 실행하게 된다.
                Connection con = DriverManager.getConnection(full_url, username, password);
                connection.set(con);
            }

            return connection.get();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

//    public void setConnection(){
//        this.connection.set(generateConnection());
//    }
//
//    public Connection getConnection(){
//        return this.connection.get();
//    }
//
//    public void removeConnection(){
//        this.connection.remove();
//    }

    public void setDevMode(boolean b) {
        isDev=b;
    }

    public void run(String query,Object... args) {
        if(query==null || query.isBlank()) return;

        query=query.trim();

        try(PreparedStatement prepstmt=getDBConnection().prepareStatement(query)) {

            if(args.length==0){
                prepstmt.executeUpdate();

            }else{

                for(int i=1;i<=args.length;i++){
                    prepstmt.setObject(i,args[i-1]);
                }

                prepstmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //스레드 로컬에 있는 연결을 그대로 주므로, Sql 객체의 커넥션과 SimpleDb 커넥션은 같다.
    public Sql genSql() {
        //System.out.println("sql을 위한 커넥션을 생성");
        return new Sql(getDBConnection());
    }

    public void startTransaction() {
        try {
            getDBConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        try {
            getDBConnection().rollback();
            getDBConnection().setAutoCommit(true);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            getDBConnection().commit();
            getDBConnection().setAutoCommit(true);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public void close() {
        if(connection.get()!=null) {
            //System.out.println("클로징 호출됨");
            try {
                connection.get().close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }finally {
                connection.remove();
            }
        }
    }


}

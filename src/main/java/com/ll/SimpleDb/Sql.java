package com.ll.SimpleDb;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Sql {
    private final Connection connection;
    //private ThreadLocal<Connection> connection;

    //private final SimpleDb db;
    String finalQuery="";

    static final String[] INJECTIONS={"=","--",";","DROP","drop","alter","ALTER","TABLE","table","OR","or","union","UNION","AND","and","'"};
    public Sql(Connection connection){
        this.connection=connection;
        //this.connection=new ThreadLocal<>();
        //this.connection.set(connection);
    }

//    public Connection getConnection(){
//        return connection;
//    }

    //개선할 수 있는 점: Sql 한 쿼리만 날린 후 다시 쓰이는 경우가 없으므로, 파라미터를 담을 수 있는 리스트를 필드로 만들어 거기에 파라미터를 넣고 나중에 setObject로 하나씩 넣는다.
    public Sql append(String query, Object... args) {
        //인젝션을 막아라!

        //System.out.println("어펜딩 하는 중");

        if(args.length==0){
            finalQuery+=query+" ";
        }else{
            for(String exploit : INJECTIONS){
                for (Object arg : args) {
                    if (arg.toString().contains(exploit)) {
                        return this; //아 몰라 넌 바인딩 안할거야.
                    }
                }
            }

            for(Object arg:args) {
                int idx=query.indexOf('?');
                query=query.substring(0,idx)+addPrefixPostfix(arg.toString())+query.substring(idx+1);
            }
            finalQuery+=query+" ";
        }

        return this;
    }

    private String addPrefixPostfix(String arg){
        return "'"+arg+"'";
    }

    public Sql appendIn(String query, Object... args) {
        if(args.length==0){
            finalQuery+=query+" ";
        }else{
            for(String exploit : INJECTIONS){
                for(int i=0;i<args.length;i++){
                    if(args[0].toString().contains(exploit)) {
                        return this; //아 몰라 넌 바인딩 안할거야.
                    }
                }
            }
            String params = Arrays.stream(args).map(object -> addPrefixPostfix(object.toString()))
                    .collect(Collectors.joining(","));
            finalQuery+=query.replace("?",params)+" ";
        }
        return this;
    }

    public long insert() {
        //db.setConnection();

        //try(Connection connection= this.connection){
        try(PreparedStatement prepstmt=connection.prepareStatement(finalQuery, Statement.RETURN_GENERATED_KEYS);) {
            int affectedRows = prepstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = prepstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1); // 첫 번째 열의 값
                    }
                }
            }
            //}

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            finalQuery="";
            //db.removeConnection();
        }

        return 0;
    }

    public long selectLong() {
        //db.setConnection();
        long value=0;
        //try(Connection connection=this.connection){
        try(PreparedStatement prepstmt=connection.prepareStatement(finalQuery)) {
            try (ResultSet rs = prepstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
            //}
        }catch (SQLException e){
            throw new RuntimeException(e);
        }finally {
            //db.removeConnection();
            finalQuery="";
        }

        return value;
    }

    //리플렉션에 대해 좀 더 공부할 것
    public <T> T selectRow(Class<T> _class) {
        //System.out.println("셀렉트 로우 호출됨");

        T instance = null;

        //db.setConnection();

        //try(Connection connection=this.connection){
        try (PreparedStatement prepstmt = connection.prepareStatement(finalQuery)) {
            try (ResultSet resultSet = prepstmt.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                Field[] fields = _class.getDeclaredFields();
                instance = _class.getDeclaredConstructor().newInstance();
                if (resultSet.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        String fieldName = metaData.getColumnName(i);
                        Object fieldValue = resultSet.getObject(i);
                        for (Field field : fields) {
                            field.setAccessible(true);

                            if (field.getName().equals(fieldName)) {
                                field.set(instance, fieldValue);
                            }
                        }
                    }
                }
                //  }

            } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }finally {
            //  db.removeConnection();
            finalQuery="";
        }
        return instance;
    }

    public Map<String, Object> selectRow() {
        //db.setConnection();
        //try(Connection connection=this.connection){
        try(PreparedStatement prepstmt=connection.prepareStatement(finalQuery)) {
            try (ResultSet resultSet = prepstmt.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                if (resultSet.next()) {
                    Map<String, Object> columnValueMap = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        columnValueMap.put(metaData.getColumnName(i), resultSet.getObject(i));
                    }
                    return columnValueMap;
                }
            }
            //}
        }catch (SQLException e){
            throw new RuntimeException(e);
        }finally {
            //  db.removeConnection();
            finalQuery="";
        }

        return null;
    }

    public LocalDateTime selectDatetime() {
        //db.setConnection();

        //try(Connection connection= this.connection){
        try(PreparedStatement prepstmt=connection.prepareStatement(finalQuery)) {
            try (ResultSet rs = prepstmt.executeQuery()) {
                if (rs.next()) {
                    // NOW()의 결과를 Timestamp 형식으로 가져오기
                    java.sql.Timestamp currentTimestamp = rs.getTimestamp(1);

                    // Timestamp를 LocalDateTime으로 변환
                    return currentTimestamp.toLocalDateTime();

                }
            }
            //}
        }catch (SQLException e){
            throw new RuntimeException(e);
        }finally {
            //db.removeConnection();
            finalQuery="";
        }

        return null;
    }

    public String selectString() {
        //db.setConnection();

        String value="";
        //try(Connection connection=this.connection){
        try(PreparedStatement prepstmt=connection.prepareStatement(finalQuery)) {
            try (ResultSet rs = prepstmt.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
            //}
        }catch (SQLException e){
            throw new RuntimeException(e);
        }finally {
            //db.removeConnection();
            finalQuery="";
        }

        return value;
    }

    public Boolean selectBoolean() {
        Boolean value=false;
        //db.setConnection();

        //try(Connection connection= this.connection){
        try(PreparedStatement prepstmt=connection.prepareStatement(finalQuery)) {
            try (ResultSet rs = prepstmt.executeQuery()) {
                if (rs.next()) return rs.getBoolean(1);
            }
            //}
        }catch (SQLException e){
            throw new RuntimeException(e);
        }finally {
            //db.removeConnection();
            finalQuery="";
        }

        return value;
    }

    public List<Long> selectLongs() {
        List<Long> values=new ArrayList<>();
        //db.setConnection();
        //try(Connection connection= this.connection){
        try(PreparedStatement prepstmt=connection.prepareStatement(finalQuery)) {
            try (ResultSet rs = prepstmt.executeQuery()) {
                while (rs.next()) {
                    values.add(rs.getLong(1));
                }
            }
            //}
        }catch (SQLException e){
            throw new RuntimeException(e);
        }finally {
            //db.removeConnection();
            finalQuery="";
        }

        return values;
    }

    public <T> List<T> selectRows(Class<T> _class) {
        List<T> instances=new ArrayList<>();
        //db.setConnection();

        //try(Connection connection=this.connection){
        try (PreparedStatement prepstmt = connection.prepareStatement(finalQuery)) {
            try (ResultSet resultSet = prepstmt.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                Field[] fields = _class.getDeclaredFields();

                while (resultSet.next()) {
                    T instance = _class.getDeclaredConstructor().newInstance();
                    for (int i = 1; i <= columnCount; i++) {
                        String fieldName = metaData.getColumnName(i);
                        Object fieldValue = resultSet.getObject(i);
                        for (Field field : fields) {
                            field.setAccessible(true);

                            if (field.getName().equals(fieldName)) {
                                field.set(instance, fieldValue);
                            }
                        }
                    }
                    instances.add(instance);
                }
                //}

            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException |
                     InstantiationException e) {
                throw new RuntimeException(e);
            }
        }catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }finally {
            //db.removeConnection();
            finalQuery="";
        }

        return instances;
    }

    public int update() {
        //db.setConnection();

        //try(Connection connection= this.connection){
        try(PreparedStatement prepstmt=connection.prepareStatement(finalQuery)) {
            return prepstmt.executeUpdate();
            //}
        }catch (SQLException e){
            throw new RuntimeException(e);
        }finally {
            //db.removeConnection();
            finalQuery="";
        }
    }

    public int delete() {
        //db.setConnection();

        //try(Connection connection= this.connection){
        try(PreparedStatement prepstmt=connection.prepareStatement(finalQuery)) {
            return prepstmt.executeUpdate();
            //  }
        }catch (SQLException e){
            throw new RuntimeException(e);
        }finally {
            //db.removeConnection();
            finalQuery="";
        }
    }

    public List<Map<String, Object>> selectRows() {
        //db.setConnection();

        List<Map<String,Object>> result=new ArrayList<>();
        //try(Connection connection=this.connection){
        try(PreparedStatement prepstmt=connection.prepareStatement(finalQuery)) {
            try (ResultSet resultSet = prepstmt.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (resultSet.next()) {
                    Map<String, Object> columnValueMap = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        columnValueMap.put(metaData.getColumnName(i), resultSet.getObject(i));
                    }
                    result.add(columnValueMap);
                }
                return result;
            }
            //}
        }catch (SQLException e){
            throw new RuntimeException(e);
        }finally {
            //db.removeConnection();
            finalQuery="";
        }
    }
}

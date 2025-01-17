package com.ll.SimpleDb;

import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@NoArgsConstructor
public class Sql {
  private Connection con;
  private String sql = "";
  private List<Object> params = new ArrayList<>();

  public Sql(Connection con) {
    this.con = con;
  }

  public Sql append(String sql, Object... params) {
    this.sql += " " + sql;
    for (Object obj : params) {
      this.params.add(obj);
    }
    return this;
  }

  public Sql appendIn(String sql, Object... params) {
    StringBuilder finalSql = new StringBuilder();
    int paramCount = 0;

    for (Object param : params) {
      if (param == null) {
        this.params.add(null);
        paramCount++;
        continue;
      }

      if (param.getClass().isArray()) {
        Object[] array = (Object[]) param;
        String questions = String.join(",", Collections.nCopies(array.length, "?"));
        Collections.addAll(this.params, array);
        finalSql.append(questions);
        paramCount += array.length;
      } else {
        this.params.add(param);
        finalSql.append("?");
        paramCount++;
      }

      finalSql.append(",");
    }

    // 마지막 콤마 제거
    if (finalSql.length() > 0) {
      finalSql.setLength(finalSql.length() - 1);
    }

    this.sql += " " + sql.replace("?", finalSql.toString());

    return this;
  }

  public long insert() {
    try (PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      for (int i = 0; i < params.size(); i++) {
          stmt.setObject(i + 1, params.get(i));
      }
      stmt.executeUpdate();
      ResultSet keyRS = stmt.getGeneratedKeys();
      keyRS.next();

      return keyRS.getInt(1);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public int update(){
    try(PreparedStatement stmt = con.prepareStatement(sql)){
      for (int i = 0; i < params.size(); i++) {
        stmt.setObject(i + 1, params.get(i));
      }
      stmt.executeUpdate();
        return stmt.getUpdateCount();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public int delete(){
    try(PreparedStatement stmt = con.prepareStatement(sql)){
      for (int i = 0; i < params.size(); i++) {
        stmt.setObject(i + 1, params.get(i));
      }
      return stmt.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<Map<String, Object>> selectRows() {
    try (PreparedStatement stmt = con.prepareStatement(sql)) {
      ResultSet rs = stmt.executeQuery();
      ResultSetMetaData rsmd = rs.getMetaData();
      int columnCount = rsmd.getColumnCount();

      List<Map<String, Object>> rows = new ArrayList<>();

      while (rs.next()) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < columnCount; i++) {
          row.put(rsmd.getColumnName(i + 1), rs.getObject(i + 1));
        }
        rows.add(row);
      }
      return rows;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public <T> List<T> selectRows(Class<T> clazz) {
    try (PreparedStatement stmt = con.prepareStatement(sql)) {
      ResultSet rs = stmt.executeQuery();
      ResultSetMetaData rsmd = rs.getMetaData();
      int columnCount = rsmd.getColumnCount();
      List<T> rows = new ArrayList<>();

      while (rs.next()) {
        T instance = clazz.getDeclaredConstructor().newInstance();

        for (int i = 0; i < columnCount; i++){
          String columnName = rsmd.getColumnName(i + 1);
          Field field = clazz.getDeclaredField(columnName);
          field.setAccessible(true);
          field.set(instance, rs.getObject(i + 1));
        }
        rows.add(instance);
      }
      return rows;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Object> selectRow() {
    try (PreparedStatement stmt = con.prepareStatement(sql)) {
      ResultSet rs = stmt.executeQuery();
      ResultSetMetaData rsmd = rs.getMetaData();
      int columnCount = rsmd.getColumnCount();

      Map<String, Object> row = new HashMap<>();

      if (rs.next()) {
        for (int i = 0; i < columnCount; i++) {
          row.put(rsmd.getColumnName(i + 1), rs.getObject(i + 1));
        }
      }
      return row;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public <T> T selectRow(Class<T> clazz){
    try(PreparedStatement stmt = con.prepareStatement(sql)){
      ResultSet rs = stmt.executeQuery();
      ResultSetMetaData rsmd = rs.getMetaData();
      int columnCount = rsmd.getColumnCount();
      T instance = clazz.getDeclaredConstructor().newInstance();
      while(rs.next()){
        for (int i = 0; i < columnCount; i++){
          String columnName = rsmd.getColumnName(i+1);
          Field field = clazz.getDeclaredField(columnName);
          field.setAccessible(true);
          field.set(instance, rs.getObject(i+1));
        }
      }
      return instance;
    } catch (Exception e){
      throw new RuntimeException(e);
    }
  }

  public LocalDateTime selectDatetime() {
    try (PreparedStatement stmt = con.prepareStatement(sql)) {
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getObject(1, LocalDateTime.class);
      }
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Long selectLong() {
    try (PreparedStatement stmt = con.prepareStatement(sql)) {
      for (int i = 0; i < params.size(); i++) {
        stmt.setObject(i + 1, params.get(i));
      }
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getLong(1);
      }
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<Long> selectLongs() {
    List<Long> list = new ArrayList<>();
    try (PreparedStatement stmt = con.prepareStatement(sql)) {
      for (int i = 0; i < params.size(); i++) {
        stmt.setObject(i + 1, params.get(i));
      }

      try (ResultSet rs = stmt.executeQuery()) {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
          Object value = rs.getObject(1);
          if (value != null) {
            list.add(value instanceof Long ? (Long) value : Long.valueOf(value.toString()));
          }
        }
      }
      return list;
    } catch (SQLException e) {
      throw new RuntimeException("SQL 실행 중 오류 발생: " + e.getMessage(), e);
    }
  }

  public String selectString() {
    try(PreparedStatement stmt = con.prepareStatement(sql)){
      ResultSet rs = stmt.executeQuery();
      if(rs.next()){
        return rs.getString(1);
      }
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);

    }
  }

  public Boolean selectBoolean() {
    try (PreparedStatement stmt = con.prepareStatement(sql)) {
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getBoolean(1);
      }
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }



}

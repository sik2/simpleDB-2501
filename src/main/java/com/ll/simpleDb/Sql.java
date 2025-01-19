package com.ll.simpleDb;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class Sql {

  private final Connection connection;
  private StringBuilder query = new StringBuilder();
  private List<Object> params = new ArrayList<>();


  public Sql append(String str, Object... args) {
    query.append(str);
    query.append(" ");
    Collections.addAll(params, args);
    return this;
  }

  public Sql appendIn(String str, Object... args) {
    int count = 0;
    if (args.length == 1 && args[0].getClass().isArray()) {
      String[] array = (String[]) args[0];
      count = array.length;
      Collections.addAll(params, array);

    } else {
      count = args.length;
      Collections.addAll(params, args);
    }

    for (Object obj : params) {
      System.out.println("param : " + obj);
    }

    String questionMarks = "?" + ", ?".repeat(count - 1);
    query.append(str.replace("?", questionMarks));
    query.append(" ");
    System.out.println("Query문 : " + query.toString());

    return this;
  }

  public long insert() {
    try (PreparedStatement stmt = connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS)) {
      for (int i = 0; i < params.size(); i++) {
        stmt.setObject(i + 1, params.get(i));
      }
      stmt.executeUpdate();

      ResultSet keyRS = stmt.getGeneratedKeys();
      keyRS.next();
      return keyRS.getLong(1);

    } catch (Exception e) {
      throw new RuntimeException("insert() 오류 발생", e);
    }
  }

  public int update() {
    try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
      for (int i = 0; i < params.size(); i++) {
        stmt.setObject(i + 1, params.get(i));
      }
      return stmt.executeUpdate();

    } catch (Exception e) {
      throw new RuntimeException("update() 오류 발생", e);
    }
  }

  public int delete() {
    try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
      for (int i = 0; i < params.size(); i++) {
        stmt.setObject(i + 1, params.get(i));
      }
      return stmt.executeUpdate();

    } catch (Exception e) {
      throw new RuntimeException("delete() 오류 발생", e);
    }
  }

  public List<Map<String, Object>> selectRows() {
    try (PreparedStatement stmt = connection.prepareStatement(query.toString());
        ResultSet rs = stmt.executeQuery()) {

      List<Map<String, Object>> rows = new ArrayList<>();
      while (rs.next()) {
        Map<String, Object> row = new java.util.HashMap<>();
        row.put("id", rs.getLong("id"));
        row.put("createdDate", rs.getTimestamp("createdDate").toLocalDateTime());
        row.put("modifiedDate", rs.getTimestamp("modifiedDate").toLocalDateTime());
        row.put("title", rs.getString("title"));
        row.put("body", rs.getString("body"));
        row.put("isBlind", rs.getBoolean("isBlind"));
        rows.add(row);
      }

      return rows;
    } catch (Exception e) {
      throw new RuntimeException("selectRows() 오류 발생", e);
    }
  }


  public Map<String, Object> selectRow() {
    try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
      for (int i = 0; i < params.size(); i++) {
        stmt.setObject(i + 1, params.get(i));
      }

      try (ResultSet rs = stmt.executeQuery()) {
        Map<String, Object> row = new java.util.HashMap<>();
        if (rs.next()) {
          row.put("id", rs.getLong("id"));
          row.put("createdDate", rs.getTimestamp("createdDate").toLocalDateTime());
          row.put("modifiedDate", rs.getTimestamp("modifiedDate").toLocalDateTime());
          row.put("title", rs.getString("title"));
          row.put("body", rs.getString("body"));
          row.put("isBlind", rs.getBoolean("isBlind"));
          return row;
        }
        return null;

      } catch (Exception e) {
        throw new RuntimeException("selectRow() 오류 발생", e);
      }
    } catch (Exception e) {
      throw new RuntimeException("selectRow() 오류 발생", e);
    }
  }


  public <T> List<T> selectRows(Class<T> clazz) {
    try (PreparedStatement stmt = connection.prepareStatement(query.toString());
        ResultSet rs = stmt.executeQuery()) {

      List<T> rows = new ArrayList<>();
      ResultSetMetaData metaData = rs.getMetaData();

      while (rs.next()) {
        T instance = clazz.getDeclaredConstructor().newInstance();

        for (int i = 1; i <= metaData.getColumnCount(); i++) {
          String columnName = metaData.getColumnName(i);
          String setterName = "set" + columnName.substring(0, 1).toUpperCase() + columnName.substring(1);
          Field field =  clazz.getDeclaredField(columnName);
          if (columnName.startsWith("is")) {
            setterName = "set" + columnName.substring(2, 3).toUpperCase() + columnName.substring(3);
          }

          Method setter = clazz.getMethod(setterName, field.getType());
          Object value;
          if (field.getType() == LocalDateTime.class) {
            Timestamp timestamp = rs.getTimestamp(columnName);
            value = timestamp != null ? timestamp.toLocalDateTime() : null;
          } else {
            value = rs.getObject(columnName, field.getType());
          }
          setter.invoke(instance, value);
        }
        rows.add(instance);
      }
      return rows;

    } catch (Exception e) {
      throw new RuntimeException("selectRows(Class Type) 오류 발생", e);
    }
  }

  public <T> T selectRow(Class<T> clazz) {
    try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
      for (int i = 0; i < params.size(); i++) {
        stmt.setObject(i + 1, params.get(i));
      }

      try (ResultSet rs = stmt.executeQuery()) {
        ResultSetMetaData metaData = rs.getMetaData();
        T instance = clazz.getDeclaredConstructor().newInstance();

        if (rs.next()) {
          for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
            String setterName = "set" + columnName.substring(0, 1).toUpperCase() + columnName.substring(1);
            Field field =  clazz.getDeclaredField(columnName);
            if (columnName.startsWith("is")) {
              setterName = "set" + columnName.substring(2, 3).toUpperCase() + columnName.substring(3);
            }

            Method setter = clazz.getMethod(setterName, field.getType());
            Object value;
            if (field.getType() == LocalDateTime.class) {
              Timestamp timestamp = rs.getTimestamp(columnName);
              value = timestamp != null ? timestamp.toLocalDateTime() : null;
            } else {
              value = rs.getObject(columnName, field.getType());
            }
            setter.invoke(instance, value);
          }
          return instance;
        }
        return null;

      } catch (Exception e) {
        throw new RuntimeException("selectRow(Class Type) 오류 발생", e);
      }
    } catch (Exception e) {
      throw new RuntimeException("selectRow(Class Type) 오류 발생", e);
    }
  }



  public LocalDateTime selectDatetime() {
    try (PreparedStatement stmt = connection.prepareStatement(query.toString());
        ResultSet rs = stmt.executeQuery()) {

      if (rs.next()) {
        return rs.getTimestamp(1).toLocalDateTime();
      }
      return null;

    } catch (Exception e) {
      throw new RuntimeException("selectDatetime() 오류 발생", e);
    }
  }

  public Long selectLong() {
    try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
      for (int i = 0; i < params.size(); i++) {
        stmt.setObject(i + 1, params.get(i));
      }

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getLong(1);
        }
        return null;

      } catch (Exception e) {
        throw new RuntimeException("selectLong() 오류 발생", e);
      }

    } catch (Exception e) {
      throw new RuntimeException("selectLong() 오류 발생", e);
    }
  }

  public List<Long> selectLongs() {
    try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
      for (int i = 0; i < params.size(); i++) {
        stmt.setObject(i + 1, params.get(i));
      }

      List<Long> list = new ArrayList<>();
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          list.add(rs.getLong(1));
        }
        return list;

      } catch (Exception e) {
        throw new RuntimeException("selectLongs() 오류 발생", e);
      }

    } catch (Exception e) {
      throw new RuntimeException("selectLongs() 오류 발생", e);
    }
  }

  public String selectString() {
    try (PreparedStatement stmt = connection.prepareStatement(query.toString());
        ResultSet rs = stmt.executeQuery()) {

      if (rs.next()) {
        return rs.getString("title");
      }
      return null;

    } catch (Exception e) {
      throw new RuntimeException("selectString() 오류 발생", e);
    }
  }

  public Boolean selectBoolean() {
    try (PreparedStatement stmt = connection.prepareStatement(query.toString());
        ResultSet rs = stmt.executeQuery()) {

      if (rs.next()) {
        return rs.getBoolean(1);
      }
      return null;

    } catch (Exception e) {
      throw new RuntimeException("selectBoolean() 오류 발생", e);
    }
  }

}

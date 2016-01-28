/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.zeppelin.jdbc;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.apache.zeppelin.jdbc.JDBCInterpreter.DEFAULT_KEY;
import static org.apache.zeppelin.jdbc.JDBCInterpreter.DEFAULT_DRIVER;
import static org.apache.zeppelin.jdbc.JDBCInterpreter.DEFAULT_PASSWORD;
import static org.apache.zeppelin.jdbc.JDBCInterpreter.DEFAULT_USER;
import static org.apache.zeppelin.jdbc.JDBCInterpreter.DEFAULT_URL;
import static org.apache.zeppelin.jdbc.JDBCInterpreter.COMMON_MAX_LINE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.zeppelin.display.AngularObjectRegistry;
import org.apache.zeppelin.display.GUI;
import org.apache.zeppelin.interpreter.*;
import org.apache.zeppelin.jdbc.JDBCInterpreter;

import org.junit.Before;
import org.junit.Test;

import com.mockrunner.jdbc.BasicJDBCTestCaseAdapter;
/**
 * JDBC interpreter unit tests
 */
public class JDBCInterpreterTest extends BasicJDBCTestCaseAdapter {

  static String jdbcConnection;
    private InterpreterContext context;
  private static String getJdbcConnection() throws IOException {
    if(null == jdbcConnection) {
      Path tmpDir = Files.createTempDirectory("h2-test-");
      tmpDir.toFile().deleteOnExit();
      jdbcConnection = format("jdbc:h2:%s", tmpDir);
    }
    return jdbcConnection;
  }
  
  @Before
  public void setUp() throws Exception {
      InterpreterGroup intpGroup = new InterpreterGroup();
      InterpreterOutput intpOutput =  new InterpreterOutput(new InterpreterOutputListener() {
          @Override
          public void onAppend(InterpreterOutput out, byte[] line) {

          }

          @Override
          public void onUpdate(InterpreterOutput out, byte[] output) {

          }
      });

      context = new InterpreterContext("note", "id", "title", "text",
              new HashMap<String, Object>(),
              new GUI(),
              new AngularObjectRegistry(intpGroup.getId(), null),
              new LinkedList<InterpreterContextRunner>(),
              intpOutput);

    Class.forName("org.h2.Driver");
    Connection connection = DriverManager.getConnection(getJdbcConnection());
    Statement statement = connection.createStatement();
    statement.execute(
        "DROP TABLE IF EXISTS test_table; " +
        "CREATE TABLE test_table(id varchar(255), name varchar(255), value int);");
    statement.execute(
        "insert into test_table(id, name, value) values ('a', 'a_name', 1),('b', 'b_name', 2);"
    );
  }

  @Test
  public void testDefaultProperties() throws SQLException {
    JDBCInterpreter jdbcInterpreter = new JDBCInterpreter(new Properties());
    
    assertEquals("org.postgresql.Driver", jdbcInterpreter.getProperty(DEFAULT_DRIVER));
    assertEquals("jdbc:postgresql://localhost:5432/", jdbcInterpreter.getProperty(DEFAULT_URL));
    assertEquals("gpadmin", jdbcInterpreter.getProperty(DEFAULT_USER));
    assertEquals("", jdbcInterpreter.getProperty(DEFAULT_PASSWORD));
    assertEquals("1000", jdbcInterpreter.getProperty(COMMON_MAX_LINE));
  }
  
  @Test
  public void testSelectQuery() throws SQLException, IOException {
    Properties properties = new Properties();
    properties.setProperty("common.max_count", "1000");
    properties.setProperty("common.max_retry", "3");
    properties.setProperty("default.driver", "org.h2.Driver");
    properties.setProperty("default.url", getJdbcConnection());
    properties.setProperty("default.user", "");
    properties.setProperty("default.password", "");
    JDBCInterpreter t = new JDBCInterpreter(properties);
    t.open();

    String sqlQuery = "select * from test_table";

    InterpreterResult interpreterResult = t.interpret(sqlQuery, context);

    assertEquals(InterpreterResult.Code.SUCCESS, interpreterResult.code());
    assertEquals(InterpreterResult.Type.TABLE, interpreterResult.type());
    assertEquals("ID\tNAME\tVALUE\na\ta_name\t1\nb\tb_name\t2\n", interpreterResult.message());
  }

  @Test
  public void testSelectQueryMaxResult() throws SQLException, IOException {

    Properties properties = new Properties();
    properties.setProperty("common.max_count", "1");
    properties.setProperty("common.max_retry", "3");
    properties.setProperty("default.driver", "org.h2.Driver");
    properties.setProperty("default.url", getJdbcConnection());
    properties.setProperty("default.user", "");
    properties.setProperty("default.password", "");
    JDBCInterpreter t = new JDBCInterpreter(properties);
    t.open();

    String sqlQuery = "select * from test_table";

    InterpreterResult interpreterResult = t.interpret(sqlQuery, context);

    assertEquals(InterpreterResult.Code.SUCCESS, interpreterResult.code());
    assertEquals(InterpreterResult.Type.TABLE, interpreterResult.type());
    assertEquals("ID\tNAME\tVALUE\na\ta_name\t1\n", interpreterResult.message());
  }

  @Test
  public void testSelectQueryWithFunction() throws SQLException, IOException {
    Properties properties = new Properties();
    properties.setProperty("common.max_count", "1000");
    properties.setProperty("common.max_retry", "3");
    properties.setProperty("default.driver", "org.h2.Driver");
    properties.setProperty("default.url", getJdbcConnection());
    properties.setProperty("default.user", "");
    properties.setProperty("default.password", "");
    JDBCInterpreter t = new JDBCInterpreter(properties);
    t.open();

    String sqlQuery = "select sum(value) from test_table";

    InterpreterResult interpreterResult = t.interpret(sqlQuery, context);

    assertEquals(InterpreterResult.Code.SUCCESS, interpreterResult.code());
    assertEquals(InterpreterResult.Type.TABLE, interpreterResult.type());
    assertEquals("SUM(VALUE)\n3\n", interpreterResult.message());
  }
}

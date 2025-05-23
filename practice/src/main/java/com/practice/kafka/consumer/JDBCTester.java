package com.practice.kafka.consumer;

import java.sql.*;

public class JDBCTester {
    public static void main(String[] args) {

        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;


        String url = "jdbc:postgresql://127.0.0.1:3306/postgres"; // 원래 기본 포트는 5432인데 도커를 3306으로 뚫어버림
        String user = "postgres";
        String password = "postgres";
        try {
            conn = DriverManager.getConnection(url, user, password);
            st = conn.createStatement();
            rs = st.executeQuery("SELECT 'postgresql is connected' ");

            if (rs.next())
                System.out.println(rs.getString(1));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                rs.close();
                st.close();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
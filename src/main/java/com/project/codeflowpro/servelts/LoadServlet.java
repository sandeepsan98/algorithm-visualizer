package com.project.codeflowpro.servelts;



import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.project.codeflowpro.util.ConnectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

@WebServlet("/load")
public class LoadServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadServlet.class);
    private static final Gson GSON = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        HttpSession session = req.getSession(false);

        // Check if user is logged in
//        if (session == null || session.getAttribute("user_id") == null) {
//            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            resp.getWriter().write("""
//                    {"error": "User not logged in"}
//                    """);
//            LOGGER.warn("Unauthorized access attempt to /load");
//            return;
//        }

       // int userId = (int) session.getAttribute("user_id");
        int userId=1;

        // Parse JSON request body
        StringBuilder jsonBuffer = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuffer.append(line);
            }
        }

        JsonObject json;
        try {
            json = GSON.fromJson(jsonBuffer.toString(), JsonObject.class);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("""
                    {"error": "Invalid JSON format"}
                    """);
            LOGGER.error("Invalid JSON received: {}", e.getMessage());
            return;
        }

        String code = json.get("code").getAsString();
        String language = json.get("language").getAsString();

        // Validate inputs
        if (code == null || code.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("""
                    {"error": "Code cannot be empty"}
                    """);
            LOGGER.warn("Empty code submitted by user {}", userId);
            return;
        }
        if (!Arrays.asList("JavaScript", "Python", "Java").contains(language)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("""
                    {"error": "Invalid language"}
                    """);
            LOGGER.warn("Invalid language submitted by user {}: {}", userId, language);
            return;
        }

        // Store code in database
        try (Connection conn = ConnectionUtil.getConnection()) {
            String sql = "INSERT INTO code (user_id, code, language) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setString(2, code);
                ps.setString(3, language);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int codeId = rs.getInt(1);
                        LOGGER.info("Code saved for user {}, code_id: {}", userId, codeId);
                        resp.getWriter().write(String.format("""
                                {"code_id": %d, "status": "success"}
                                """, codeId));
                    } else {
                        throw new SQLException("Failed to retrieve generated code_id");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Database error for user {}: {}", userId, e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(String.format("""
                    {"error": "Database error: %s"}
                    """, e.getMessage()));
        }
    }
}
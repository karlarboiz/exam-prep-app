package com.examprep.service;

import com.examprep.dao.UserDao;
import com.examprep.model.ExamLevel;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.util.JwtUtil;
import com.examprep.util.PasswordUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public Optional<User> authenticate(String username, String password) throws SQLException {
        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public String issueToken(User user) {
        return JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
    }

    public User register(String username, String email, String password, ExamLevel examLevel) throws SQLException {
        if (examLevel == null) {
            throw new IllegalArgumentException("Exam level is required");
        }
        if (userDao.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userDao.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        String hash = PasswordUtil.hash(password);
        return userDao.create(username, email, hash, Role.USER, examLevel);
    }

    public Optional<User> findById(Long id) throws SQLException {
        return userDao.findById(id);
    }

    public List<User> findAllUsers() throws SQLException {
        return userDao.findAll();
    }
}

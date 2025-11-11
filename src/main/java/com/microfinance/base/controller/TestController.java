package com.microfinance.base.controller;

import com.microfinance.base.dto.AuthResponse;
import com.microfinance.base.dto.LoginRequest;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    private final AuthService authService;

//TEST END POINTS////
    //curl -X GET "http://localhost:8080/api/test/test-login?password=admin123"
    // Add this test endpoint
    @GetMapping("/test-login")
    public ResponseEntity<String> testLogin(@RequestParam String password) {
        try {
            User admin = userRepository.findByUsername("admin")
                    .orElseThrow(() -> new RuntimeException("Admin user not found"));

            StringBuilder result = new StringBuilder();
            result.append("=== AUTHENTICATION TEST ===\n");
            result.append("Username: admin\n");
            result.append("Password tested: ").append(password).append("\n");
            result.append("Stored hash: ").append(admin.getPassword()).append("\n");

            boolean matches = passwordEncoder.matches(password, admin.getPassword());
            result.append("Password matches: ").append(matches).append("\n");

            if (matches) {
                result.append("✅ LOGIN SHOULD WORK!\n");
            } else {
                result.append("❌ LOGIN WILL FAIL!\n");

                // Test common passwords
                result.append("\nTesting common passwords:\n");
                String[] commonPasswords = {"admin123", "password", "123456", "admin", "Admin123",
                        "admin@123", "Admin@123", "microfinance", "12345"};

                for (String commonPwd : commonPasswords) {
                    if (passwordEncoder.matches(commonPwd, admin.getPassword())) {
                        result.append("✅ This hash matches: '").append(commonPwd).append("'\n");
                    }
                }
            }

            return ResponseEntity.ok(result.toString());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    //curl -X POST http://localhost:8080/api/test/reset-admin-now

    @PostMapping("/reset-admin-now")
    public ResponseEntity<String> resetAdminNow() {
        try {
            User admin = userRepository.findByUsername("admin")
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            String newPassword = "admin123";
            String newHash = passwordEncoder.encode(newPassword);

            admin.setPassword(newHash);
            admin.setFailedLoginAttempts(0);
            admin.setAccountLockedUntil(null);
            userRepository.save(admin);

            return ResponseEntity.ok(
                    "✅ ADMIN PASSWORD RESET SUCCESSFUL!\n" +
                            "Username: admin\n" +
                            "New Password: admin123\n" +
                            "New Hash: " + newHash + "\n" +
                            "You can now login with username: admin, password: admin123"
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
//curl to test login
//$ curl -X GET http://localhost:8080/api/auth/test-login
// -H "Content-Type: application/json"
// -d '{"username":"admin","password":"admin123"}'






}
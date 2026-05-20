package com.example.demo.service;

import com.example.demo.dto.UserLoginRequestDto;
import java.util.Optional;

import com.example.demo.dto.UserRegisterRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ModelMapper model;
    private final HttpServletRequest request;

    public ResponseEntity register(UserRegisterRequestDto userRegisterRequestDto) {

        List<User> userList = userRepository.findByEmailEqualsOrPhoneEqualsAllIgnoreCase(
                userRegisterRequestDto.getEmail(),
                userRegisterRequestDto.getPhone()
        );

        if (userList.size() > 0) {
            Map<String, Object> hm = Map.of("success", false, "message", "Bu email veya telefon numarası zaten kullanımda.");
            return ResponseEntity.badRequest().body(hm);
        }

        User user = model.map(userRegisterRequestDto, User.class);

        String hashPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashPassword);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        UserResponseDto responseDto = model.map(savedUser, UserResponseDto.class);

        return ResponseEntity.ok().body(responseDto);
    }

    public ResponseEntity login(UserLoginRequestDto userLoginRequestDto) {

        Optional<User> optionalUser = userRepository.findByEnabledTrueAndEmailIgnoreCaseOrEnabledTrueAndPhoneIgnoreCase(
                userLoginRequestDto.getUsername(),
                userLoginRequestDto.getUsername()
        );

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            boolean isMatch = BCrypt.checkpw(userLoginRequestDto.getPassword(), user.getPassword());

            if (isMatch) {
                // Eski session'ı geçersiz kıl
                HttpSession oldSession = request.getSession(false);
                if (oldSession != null) oldSession.invalidate();

                HttpSession newSession = request.getSession(true);
                UserResponseDto userResponseDto = model.map(user, UserResponseDto.class);
                newSession.setAttribute("user", userResponseDto);

                return ResponseEntity.ok().body(userResponseDto);
            }
        }

        Map<String, Object> hm = Map.of("success", false, "message", "Kullanıcı adı veya şifre hatalı.");
        return ResponseEntity.badRequest().body(hm);
    }

    public ResponseEntity logout() {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok().body(Map.of("success", true, "message", "Oturum başarıyla kapatıldı."));
    }

}
package com.booking.service;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.booking.constant.Role;
import com.booking.constant.UserStatus;
import com.booking.dto.LoginDTO;
import com.booking.dto.LoginResponseDTO;
import com.booking.dto.UserDTO;
import com.booking.entity.User;
import com.booking.exception.UserNotFoundException;
import com.booking.repository.UserRepository;
import com.booking.security.jwt.JwtUtils;
import com.booking.security.service.UserDetailsImpl;
import com.booking.utils.ImageUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserService {

	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepo;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	JwtUtils jwtutils;

	@Autowired
	ObjectMapper objectMapper;

	@Value("${file.upload-profile-dir}")
	private String fileUploadProfileDir;

	public User registerUser(String user, MultipartFile file) {

		try {
			UserDTO userDTO = objectMapper.readValue(user, UserDTO.class);

			if (userRepo.existsByEmailIdIgnoreCase(userDTO.getEmailId())) {

				throw new NotFoundException();
			}

			User createUser = new User();
			createUser.setUserName(userDTO.getUserName());
			createUser.setEmailId(userDTO.getEmailId());

			createUser.setFirstName(userDTO.getFirstName());
			createUser.setLastName(userDTO.getLastName());

			createUser.setStatus(UserStatus.PENDING.name());

			createUser.setRole(Role.ROLE_ADMIN.name());

			File fileName = ImageUtils.uploadFile(file, fileUploadProfileDir);

			if (fileName != null) {
				createUser.setPicture(fileUploadProfileDir + File.separator + fileName);
			}
			createUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));

			return userRepo.save(createUser);
		} catch (Exception e) {
			throw new UsernameNotFoundException(e.getMessage());
		}

	}

	public LoginResponseDTO login(LoginDTO loginDTO) {
		try {

			Authentication authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword()));

			SecurityContextHolder.getContext().setAuthentication(authentication);

			String jwt = jwtutils.generateJwtToken(authentication);

			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

			return new LoginResponseDTO(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getFirstName(),
					userDetails.getLastName(), userDetails.getPicture(), userDetails.getUserNameLogin(),
					userDetails.getStatus(), userDetails.getRole());

		} catch (Exception e) {

			throw new UserNotFoundException(e.getMessage());
		}

	}

}

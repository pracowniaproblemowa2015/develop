package com.scheduler.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scheduler.model.AccessToken;
import com.scheduler.model.User;
import com.scheduler.repository.AccessTokenRepository;
import com.scheduler.repository.UserRepository;
import com.scheduler.service.UserService;

@Service
@Transactional
public class UserServiceImpl implements UserService {

	private static final int ACCESS_TOKEN_PERSISTANCE_DAYS = 30;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccessTokenRepository accessTokenRepository;

	@Autowired
	private HttpServletRequest request;

	@Value("${salt}")
	private String salt;

	@Override
	public User getUser(long id) {
		User user = userRepository.findOne(id);
		if (user == null) {
			return null;
		}
		return user;
	}

	@Override
	public List<User> getAll() {
		return userRepository.findAll();
	}

	@Override
	public User saveUser(User user) {
		Md5PasswordEncoder enc = new Md5PasswordEncoder();

		User existingUser = userRepository.findOneByLogin(user.getLogin());
		if (existingUser != null) {
			return null;
		}

		user.setPassword(enc.encodePassword(user.getPassword(), salt));

		return userRepository.save(user);
	}

	@Override
	public AccessToken authenticate(String login, String password) {

		User user = userRepository.findOneByLogin(login);
		if (user == null) {
			return null;
		}
		Md5PasswordEncoder enc = new Md5PasswordEncoder();

		if (!enc.isPasswordValid(user.getPassword(), password, salt)) {
			return null;
		}

		List<AccessToken> accessTokens = accessTokenRepository.findByUserOrderByExpireDesc(user);
		AccessToken accessToken = null;

		if (accessTokens.size() > 0) {
			accessToken = accessTokens.get(0);
		}

		if (accessToken == null || accessToken.getExpire().getTime() < new Date().getTime()) {
			accessToken = new AccessToken();
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, ACCESS_TOKEN_PERSISTANCE_DAYS);
			accessToken.setExpire(cal.getTime());
			accessToken.setUser(user);
			String token = KeyGenerators.string().generateKey() + KeyGenerators.string().generateKey();
			accessToken.setToken(token);
			accessTokenRepository.save(accessToken);
		}

		SecurityContextHolder.getContext().setAuthentication(
				new PreAuthenticatedAuthenticationToken(user, accessToken.getToken()));

		return accessToken;
	}

	@Override
	public User getCurrentUser() {
		String token = request.getParameter("access_token");
		if (token == null) {
			return null;
		}
		AccessToken accessToken = accessTokenRepository.findByToken(token);
		if (accessToken == null) {
			return null;
		}
		return accessToken.getUser();
	}

	@Override
	public User updateUser(User user) {
		if (user.getId() == null) {
			return null;
		}
		User existing = userRepository.findOne(user.getId());

		if (user.getFirstName() != null) {
			existing.setFirstName(user.getFirstName());
		}
		if (user.getLastName() != null) {
			existing.setLastName(user.getLastName());
		}
		if (user.getPassword() != null) {
			Md5PasswordEncoder enc = new Md5PasswordEncoder();
			existing.setPassword(enc.encodePassword(user.getPassword(), existing.getLastName()));
		}

		if (user.getLogin() != null) {
			existing.setLogin(user.getLogin());
		}
		if (user.getEmail() != null) {
			existing.setEmail(user.getEmail());
		}
		if (user.getRole() != null) {
			existing.setRole(user.getRole());
		}
		return userRepository.save(existing);

	}

	@Override
	public User delete(Long id) {
		User user = userRepository.findOne(id);
		accessTokenRepository.delete(accessTokenRepository.findByUser(user));
		userRepository.delete(id);

		return user;

	}

}

package com.scheduler.interceptor;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.scheduler.model.AccessToken;
import com.scheduler.repository.AccessTokenRepository;

public class CheckAccessTokenInterceptor implements HandlerInterceptor {

	@Autowired
	private AccessTokenRepository accessTokenRepository;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String uri = request.getRequestURI().replace(request.getContextPath(), "");

		if (uri.startsWith("/frontend") || uri.startsWith("/resources")) {
			return true;
		}

		if (uri.endsWith("/users") && request.getMethod().equalsIgnoreCase("POST")) {
			return true;
		}

		if (uri.endsWith("/users/auth")) {
			return true;
		}

		String token = request.getParameter("access_token");

		if (token == null) {
			return false;
		}

		AccessToken accessToken = accessTokenRepository.findByToken(token);

		if (accessToken == null) {
			return false;
		}

		if (accessToken.getExpire().getTime() < new Date().getTime()) {
			return false;
		}

		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) {
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
	}

}

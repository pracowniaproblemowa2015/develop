package com.scheduler.controller.rest;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.scheduler.model.AccessToken;
import com.scheduler.model.User;
import com.scheduler.service.UserService;

@Transactional
@Controller
@RequestMapping("/api/user")
public class UserController {

	@Autowired
	private UserService userService;

	@RequestMapping(method = RequestMethod.GET, headers = "Accept=application/json", produces = "application/json")
	@ResponseBody
	public List<User> getAllUsers() {
		return userService.getAll();
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET, headers = "Accept=application/json", produces = "application/json")
	@ResponseBody
	public User getUser(@PathVariable Long id) {
		return userService.getUser(id);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
	@ResponseBody
	public User updateUser(@PathVariable Long id, @RequestBody User user)
			throws MissingServletRequestParameterException {
		return userService.updateUser(user);
	}

	@RequestMapping(value = "/me", method = RequestMethod.GET, headers = "Accept=application/json", produces = "application/json")
	@ResponseBody
	public User getCurrentUser() {
		return userService.getCurrentUser();
	}

	@RequestMapping(method = RequestMethod.POST, consumes = "application/json", headers = "Accept=application/json", produces = "application/json")
	@ResponseBody
	public User addUser(HttpServletResponse response, @RequestBody User user) throws IOException {
		if (user.getId() != null) {
			user.setId(null);
		}
		if (user.getLogin() == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing field: login");
		}
		if (user.getPassword() == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing field: password");
		}
		user.setCreationDate(Calendar.getInstance().getTime());
		user.setModificationDate(Calendar.getInstance().getTime());
		return userService.saveUser(user);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	@ResponseBody
	public User deleteUser(@PathVariable Long id) {

		return userService.delete(id);
	}

	@RequestMapping(value = "/auth", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public AccessToken authenticate(@RequestBody User user) {
		return userService.authenticate(user.getLogin(), user.getPassword());
	}

}

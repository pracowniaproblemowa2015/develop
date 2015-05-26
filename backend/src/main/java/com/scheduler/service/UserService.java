package com.scheduler.service;

import java.util.List;

import org.springframework.web.bind.MissingServletRequestParameterException;

import com.scheduler.model.AccessToken;
import com.scheduler.model.User;

/**
 * This service is created for support user functionality. It's used for
 * manipulations on user data in application.
 * 
 * @author Łukasz Kracoń
 * 
 */
public interface UserService {

	/**
	 * Method used to find user with specified id
	 * 
	 * @param id
	 *            - user id
	 * @return User or null if not exist
	 */
	User getUser(long id);

	/**
	 * Returns list of all users in application
	 * 
	 * @return - if there is no user, returns empty list
	 */
	List<User> getAll();

	/**
	 * Method used to add user to application.
	 * 
	 * @param user
	 *            - Password should be passed as plain text
	 * @return - User saved in database, with hashed password
	 */
	User saveUser(User user);

	/**
	 * Method used for authentication purpose. Checks if credentials are valid.
	 * 
	 * @param login
	 *            - user login
	 * @param password
	 *            - user password in plain text
	 * @return - existing AccessToken. Generate new if there is no AccessToken
	 *         for this user, or AccessToken is older than 1 month.
	 */
	AccessToken authenticate(String login, String password);

	/**
	 * Returns current user based on
	 * {@link javax.servlet.http.HttpServletRequest} access_token param.
	 * 
	 * @return - User. Null if access_token is not present in request, or
	 *         acccess_token is invalid.
	 */
	User getCurrentUser();

	/**
	 * Update user in database, but not override on null values.
	 * 
	 * @param user
	 *            - user before update.
	 * @return - user after update.
	 * @throws MissingServletRequestParameterException
	 */
	User updateUser(User user) throws MissingServletRequestParameterException;

	/**
	 * Delete user based on id.
	 * 
	 * @param id
	 *            - user id
	 * @return - user, that has been deleted
	 */
	User delete(Long id);

}

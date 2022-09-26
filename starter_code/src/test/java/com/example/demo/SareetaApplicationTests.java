package com.example.demo;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class SareetaApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate testRestTemplate;

	private String baseUrl;

	@Before
	public void setupEach() {
		baseUrl = String.format("http://localhost:%s", port);
	}

	@Test
	public void contextLoads() {
	}

	@Test
	public void createUserShouldReturnOkAndUserIfRequestIsValid() {
		final CreateUserRequest req = CreateUserRequest.builder()
				.username("user")
				.password("password")
				.confirmPassword("password")
				.build();

		final ResponseEntity<User> resp = createUser(req);
		assertEquals(HttpStatus.OK, resp.getStatusCode());
		assertEquals(1L, resp.getBody().getId());
		assertEquals(req.getUsername(), resp.getBody().getUsername());
	}

	@Test
	public void createUserShouldReturnBadRequestIfRequestIsNotValid() {
		final CreateUserRequest req = CreateUserRequest.builder()
				.username("user")
				.password("a")
				.confirmPassword("a")
				.build();

		final ResponseEntity<User> resp = createUser(req);
		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
	}

	@Test
	public void loginShouldReturnOkAndAuthorizationHeaderIfCredentialsAreCorrect() throws JSONException {
		final CreateUserRequest createUserReq = CreateUserRequest.builder()
				.username("user")
				.password("password")
				.confirmPassword("password")
				.build();

		createUser(createUserReq);

		final ResponseEntity<Object> resp = login(createUserReq.getUsername(), createUserReq.getPassword());
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final String authorization = getAuthorizationFromLoginResponse(resp);
		assertNotNull(authorization);
	}

	@Test
	public void loginShouldReturnForbiddenIfCredentialsAreNotCorrect() throws JSONException {
		final CreateUserRequest createUserReq = CreateUserRequest.builder()
				.username("user")
				.password("password")
				.confirmPassword("password")
				.build();

		createUser(createUserReq);

		final ResponseEntity<Object> resp = login(createUserReq.getUsername(), "hfdsj");
		assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
	}

	@Test
	public void getUserShouldReturnOkAndUserIfAuthenticatedAndUserExists() throws JSONException {
		final String username = "user";

		final MultiValueMap<String, String> headers = createUserAndLoginAndReturnHeadersWithAuthorization(username);

		final ResponseEntity<User> userResp = testRestTemplate.exchange(
				baseUrl + "/api/user/{username}",
				HttpMethod.GET,
				new HttpEntity<>(headers),
				User.class,
				username);
		assertEquals(HttpStatus.OK, userResp.getStatusCode());
		assertEquals(username, userResp.getBody().getUsername());
	}

	@Test
	public void getUserShouldReturnNotFoundIfAuthenticatedAndUserNotExists() throws JSONException {
		final MultiValueMap<String, String> headers = createUserAndLoginAndReturnHeadersWithAuthorization("user");

		final ResponseEntity<User> userResp = testRestTemplate.exchange(
				baseUrl + "/api/user/{username}",
				HttpMethod.GET,
				new HttpEntity<>(headers),
				User.class,
				"anotheruser");
		assertEquals(HttpStatus.NOT_FOUND, userResp.getStatusCode());
	}

	@Test
	public void getUserShouldReturnForbiddenIfNotAuthenticated() throws JSONException {
		final ResponseEntity<User> userResp = testRestTemplate.exchange(
				baseUrl + "/api/user/{user}",
				HttpMethod.GET,
				new HttpEntity<>(new LinkedMultiValueMap<>()),
				User.class,
				"user");
		assertEquals(HttpStatus.FORBIDDEN, userResp.getStatusCode());
	}

	@Test
	public void getItemByIdShouldReturnOkAndItemIfAuthenticatedAndItemExists() throws JSONException {
		final MultiValueMap<String, String> headers = createUserAndLoginAndReturnHeadersWithAuthorization("user");

		final Long id = 1L;

		final ResponseEntity<Item> resp = testRestTemplate.exchange(
				baseUrl + "/api/item/{id}",
				HttpMethod.GET,
				new HttpEntity<>(headers),
				Item.class,
				id);
		assertEquals(HttpStatus.OK, resp.getStatusCode());
		assertEquals(id, resp.getBody().getId());
	}

	@Test
	public void getItemByIdShouldReturnForbiddenIfNotAuthenticated() {
		final ResponseEntity<Item> resp = testRestTemplate.exchange(
				baseUrl + "/api/item/{id}",
				HttpMethod.GET,
				new HttpEntity<>(new LinkedMultiValueMap<>()),
				Item.class,
				1L);
		assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
	}

	@Test
	public void getItemsByNameShouldReturnOkAndItemIfAuthenticatedAndItemExists() throws JSONException {
		final MultiValueMap<String, String> headers = createUserAndLoginAndReturnHeadersWithAuthorization("user");

		final String name = "Round Widget";

		final ResponseEntity<List<Item>> resp = testRestTemplate.exchange(
				baseUrl + "/api/item/name/{name}",
				HttpMethod.GET,
				new HttpEntity<>(headers),
				new ParameterizedTypeReference<List<Item>>() {},
				name);
		assertEquals(HttpStatus.OK, resp.getStatusCode());
		assertEquals(name, resp.getBody().get(0).getName());
	}

	@Test
	public void getItemsByNameShouldReturnForbiddenIfNotAuthenticated() {
		final ResponseEntity<Object> resp = testRestTemplate.exchange(
				baseUrl + "/api/item/name/{name}",
				HttpMethod.GET,
				new HttpEntity<>(new LinkedMultiValueMap<>()),
				Object.class,
				"abc");
		assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
	}

	@Test
	public void getHistoryShouldReturnOkAndOrdersIfAuthenticatedAndUserExists() throws JSONException {
		final String username = "user";

		final MultiValueMap<String, String> headers = createUserAndLoginAndReturnHeadersWithAuthorization(username);

		final ResponseEntity<List<UserOrder>> resp = testRestTemplate.exchange(
				baseUrl + "/api/order/history/{username}",
				HttpMethod.GET,
				new HttpEntity<>(headers),
				new ParameterizedTypeReference<List<UserOrder>>() {},
				username);
		assertEquals(HttpStatus.OK, resp.getStatusCode());
	}

	@Test
	public void getHistoryShouldReturnForbiddenIfNotAuthenticated() {
		final ResponseEntity<Object> resp = testRestTemplate.exchange(
				baseUrl + "/api/order/history/{username}",
				HttpMethod.GET,
				new HttpEntity<>(new LinkedMultiValueMap<>()),
				Object.class,
				"user");
		assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
	}

	@Test
	public void addToCartShouldReturnOkAndCartIfAuthenticatedAndItemExists() throws JSONException {
		final String username = "user";

		final MultiValueMap<String, String> headers = createUserAndLoginAndReturnHeadersWithAuthorization(username);

		final long itemId = 1L;
		final int quantity = 3;

		final ModifyCartRequest req = ModifyCartRequest.builder()
				.username(username)
				.itemId(itemId)
				.quantity(quantity)
				.build();

		final ResponseEntity<Cart> resp = testRestTemplate.exchange(
				baseUrl + "/api/cart/addToCart",
				HttpMethod.POST,
				new HttpEntity<>(req, headers),
				Cart.class);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final Cart cart = resp.getBody();
		assertEquals(quantity, cart.getItems().size());
	}

	@Test
	public void addToCartShouldReturnForbiddenIfNotAuthenticated() {
		final ModifyCartRequest req = ModifyCartRequest.builder()
				.username("username")
				.itemId(1L)
				.quantity(3)
				.build();

		final ResponseEntity<Cart> resp = testRestTemplate.exchange(
				baseUrl + "/api/cart/addToCart",
				HttpMethod.POST,
				new HttpEntity<>(req, new LinkedMultiValueMap<>()),
				Cart.class);
		assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
	}

	private ResponseEntity<User> createUser(final CreateUserRequest req) {
		return testRestTemplate.postForEntity(baseUrl + "/api/user/create", req, User.class);
	}

	private ResponseEntity<Object> login(final String username, final String password) throws JSONException {
		final JSONObject req = new JSONObject()
				.put("username", username)
				.put("password", password);

		return testRestTemplate.postForEntity(
				baseUrl + "/login", new HttpEntity<>(req.toString()), Object.class);
	}

	private String getAuthorizationFromLoginResponse(final ResponseEntity<Object> resp) {
		final List<String> authorization = resp.getHeaders().get("Authorization");
		return authorization.get(0);
	}

	private MultiValueMap<String, String> createUserAndLoginAndReturnHeadersWithAuthorization(final String username)
			throws JSONException {
		final CreateUserRequest createUserReq = CreateUserRequest.builder()
				.username(username)
				.password("password")
				.confirmPassword("password")
				.build();

		createUser(createUserReq);

		final ResponseEntity<Object> loginResp = login(createUserReq.getUsername(), createUserReq.getPassword());
		final String authorization = getAuthorizationFromLoginResponse(loginResp);

		final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.add("Authorization", authorization);

		return headers;
	}

}

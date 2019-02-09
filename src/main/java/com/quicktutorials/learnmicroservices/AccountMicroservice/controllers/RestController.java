package com.quicktutorials.learnmicroservices.AccountMicroservice.controllers;

import com.quicktutorials.learnmicroservices.AccountMicroservice.entities.Operation;
import com.quicktutorials.learnmicroservices.AccountMicroservice.entities.User;
import com.quicktutorials.learnmicroservices.AccountMicroservice.services.LoginService;
import com.quicktutorials.learnmicroservices.AccountMicroservice.services.OperationService;
import com.quicktutorials.learnmicroservices.AccountMicroservice.utils.ConfigurableProperties;
import com.quicktutorials.learnmicroservices.AccountMicroservice.utils.UserNotLoggedException;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

//@Controller
@org.springframework.web.bind.annotation.RestController
public class RestController {

    private static final Logger log = LoggerFactory.getLogger(RestController.class);

    @Autowired
    LoginService loginService;

    @Autowired
    OperationService operationService;

    @Autowired
    ConfigurableProperties configurableProperties;

    @RequestMapping("/hello")
    @ResponseBody
    public String sayHello() {
        //return "Hello everyone";
        return configurableProperties.sayHelloWorld();
    }

    //Da testare su Postman con x-www-form-urlencoded

    @RequestMapping("/newuser1")
    public String addUser(User user) {
        return "User added correctly " + user.getId() + ", " + user.getUsername();
    }

    @RequestMapping("/newuser2")
    public String addUserValid(@Valid User user) {
        return "User added correctly " + user.getId() + ", " + user.getUsername();
    }

    @RequestMapping("/newuser3")
    public String addUserValidPlusBinding(@Valid User user, BindingResult result) {
        if (result.hasErrors()) {
            return result.toString();
        }

        return "User added correctly " + user.getId() + ", " + user.getUsername();
    }

    @RequestMapping("/newuser4")
    public String addUserValidPlusBinding2(@Valid User user, BindingResult result) {
        UserValidator userValidator = new UserValidator();
        userValidator.validate(user, result);

        if (result.hasErrors()) {
            return result.toString();
        }
        return "User added correctly " + user.getId() + ", " + user.getUsername();
    }

    @RequestMapping(value = "/login", method = POST)
    public ResponseEntity<JsonResponseBody> loginUser(@RequestParam(value = "id") String id, @RequestParam(value = "password") String pwd) {
        //1) Check if user exist in DB
        //2) If exist generate JWT and send back to client
        //   --> Service (@Component -> @Controller, @Service, @Repository )
        try {
            Optional<User> userr =  loginService.getUserFormDbAndVerifyPassword(id, pwd);
            if(userr.isPresent()) {
                User user = userr.get();
                String jwt = loginService.createJwt(user.getId(), user.getUsername(), user.getPermission(), new Date());
                return ResponseEntity.status(HttpStatus.OK).header("jwt", jwt).body(new JsonResponseBody(HttpStatus.OK.value(), "Success. User logged in!"));
            }
        } catch (UserNotLoggedException e1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JsonResponseBody(HttpStatus.FORBIDDEN.value(), "Login failed. Wrong credentials " + e1.toString()));
        } catch (UnsupportedEncodingException e2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JsonResponseBody(HttpStatus.FORBIDDEN.value(), "Token error " + e2.toString()));
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JsonResponseBody(HttpStatus.FORBIDDEN.value(), "No corrispondence in the database of user"));
    }

    @RequestMapping("/operations/account/{account}")
    public ResponseEntity<JsonResponseBody> fetchAllOperationsPerAccount(HttpServletRequest request, @PathVariable(name = "account") String account) {
        // 1) request
        // 2) Fetch JWT
        // 3) Check validity
        // 4) Get operations from the user account
        try {
            loginService.verifyJwtAndGetData(request);
            return ResponseEntity.status(HttpStatus.OK).body(new JsonResponseBody(HttpStatus.OK.value(), operationService.getAllOperationPerAccount(account)));
        } catch (UnsupportedEncodingException e1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JsonResponseBody(HttpStatus.FORBIDDEN.value(), "Unsupported encoding " + e1.toString()));
        } catch (UserNotLoggedException e2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JsonResponseBody(HttpStatus.FORBIDDEN.value(), "User not logged " + e2.toString()));
        } catch(ExpiredJwtException e3) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(new JsonResponseBody(HttpStatus.GATEWAY_TIMEOUT.value(), "Session expired " + e3.toString()));
        }
    }

    @RequestMapping(value="accounts/user", method = POST)
    public ResponseEntity<JsonResponseBody> fetchAllAcountsPerUser(HttpServletRequest request) {
        //1) request
        //2) fetch JWT
        //3) recover user data
        //4) Get user account from DB
        try {
            Map<String, Object> userData = loginService.verifyJwtAndGetData(request);
            return ResponseEntity.status(HttpStatus.OK).body(new JsonResponseBody(HttpStatus.OK.value(), operationService.getAllAccountPerUser((String)userData.get("subject"))));
        } catch (UnsupportedEncodingException e1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JsonResponseBody(HttpStatus.BAD_REQUEST.value(), "Bad request " + e1.toString()));
        } catch (UserNotLoggedException e2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JsonResponseBody(HttpStatus.FORBIDDEN.value(), "User not logged! Login first " + e2.toString()));
        } catch (ExpiredJwtException e3) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(new JsonResponseBody(HttpStatus.GATEWAY_TIMEOUT.value(), "Session expired " + e3.toString()));
        }
    }

    @RequestMapping(value="/operations/add", method = POST)
    public ResponseEntity<JsonResponseBody> addOperation(HttpServletRequest request, @Valid Operation operation, BindingResult bindingResult) {
        // 1) request
        // 2) fetch JWT
        // 3) recover user data
        // 4) Save valid operation in db
        if(bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JsonResponseBody(HttpStatus.FORBIDDEN.value(), "Error! Invalid format of data"));
        }

        try {
           loginService.verifyJwtAndGetData(request);
           return ResponseEntity.status(HttpStatus.OK).body(new JsonResponseBody(HttpStatus.OK.value(), new JsonResponseBody(HttpStatus.OK.value(), operationService.saveOperation(operation))));
        } catch (UnsupportedEncodingException e1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JsonResponseBody(HttpStatus.BAD_REQUEST.value(), "Bad request " + e1.toString()));
        } catch (UserNotLoggedException e2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JsonResponseBody(HttpStatus.FORBIDDEN.value(), "User not logged! Login first " + e2.toString()));
        } catch (ExpiredJwtException e3) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(new JsonResponseBody(HttpStatus.GATEWAY_TIMEOUT.value(), "Session expired " + e3.toString()));
        }
    }

    private class UserValidator implements Validator {
        @Override
        public boolean supports(Class<?> aClass) {
            return User.class.equals(aClass);
        }

        @Override
        public void validate(Object o, Errors errors) {
            User user = (User) o;
            if (user.getPassword().length() < 8) {
                errors.rejectValue("password", "The password must be at least 8 chars long!");
            }
        }
    }

    /**
     * inner class used as the Object tied into the Body of the ResponseEntity.
     * It's important to have this Object because it is composed of server response code and response object.
     * Then, JACKSON LIBRARY automatically convert this JsonResponseBody Object into a JSON response.
     */
    @AllArgsConstructor
    public class JsonResponseBody {
        @Getter
        @Setter
        private int server;

        @Getter
        @Setter
        private Object response;
    }

}

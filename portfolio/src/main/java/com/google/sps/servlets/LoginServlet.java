// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String button;
    UserService userService = UserServiceFactory.getUserService();

    // If user is not logged in, return a login button
    if (!userService.isUserLoggedIn()) {
      String loginUrl = userService.createLoginURL("/index.html");
      button = "<p><a id=\"login-button\" href=\"" + loginUrl + "\">Login</a></p>";
    }
    // User is logged in, return a logout button
    else {
      String logoutUrl = userService.createLogoutURL("/index.html");
      button = "<p><a id=\"logout-button\" href=\"" + logoutUrl + "\">Logout</a></p>";
    }
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.println(button);
  }
}

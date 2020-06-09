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
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/reply")
public class ReplyServlet extends HttpServlet {

  /*
   * Called when a client submits a POST request to the /reply URL
   * Adds submitted comment to internal record if the comment is
   * non-empty. 
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String parsedBody = CharStreams.toString(request.getReader());
    JsonObject jsonObject = UtilityFunctions.stringToJsonObject(parsedBody);

    String userComment = UtilityFunctions.getFieldFromJsonObject(jsonObject, "comment", "");
    if (userComment.length() != 0) {
      String userName = UtilityFunctions.getFieldFromJsonObject(jsonObject, "name", "Anonymous");
      String userEmail = UtilityFunctions.getFieldFromJsonObject(
          jsonObject, "email", "janedoe@gmail.com");
      String currDate = String.valueOf(System.currentTimeMillis());
      long userDate = Long.parseLong(UtilityFunctions.getFieldFromJsonObject(
          jsonObject, "timestamp", currDate));
      long parentId = Long.parseLong(UtilityFunctions.getFieldFromJsonObject(
          jsonObject, "parentid", "0"));
      long rootId = Long.parseLong(UtilityFunctions.getFieldFromJsonObject(
          jsonObject, "rootid", "0"));
      UtilityFunctions.addToDatastore(userName, userEmail, userDate, userComment, parentId,
          rootId, /* isReply = */ true, /* upvotes = */ 0, /* downvotes = */ 0);
    }
  }
}

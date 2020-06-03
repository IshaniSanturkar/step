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

import com.google.auto.value.AutoValue;

@AutoValue 
abstract class UserComment {
  static UserComment create(String name, String email, String comment, String timestamp) {
    return new AutoValue_UserComment(name, email, comment, timestamp);
  }

  abstract String name();
  abstract String email();
  abstract String comment();
  abstract String timestamp();
}
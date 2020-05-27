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

/**
 * Adds a random fact about me to the page.
 */
function addRandomFact() {
  const facts =
      ['I\'m allergic to avocados', 'I was born on 9/11/2000', 'I moved to the US when I was 16', 'My sister and I are 8 years apart in age'];

  // Pick a random fact.
  const fact = facts[Math.floor(Math.random() * facts.length)];

  // Add it to the page.
  const factContainer = document.getElementById('fact-container');
  factContainer.innerText = fact;
}

function toggleCourseList(listObject) {
    let semester = listObject.id;
    let semCourseList = "courselist" + semester;
    let lst = document.getElementById(semCourseList);
    if(lst.style.display === "none" || lst.style.display === "")
    {
        lst.style.display = "block";
    }
    else
    {
        lst.style.display = "none";
    }
}
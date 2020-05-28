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



const loc = "/images/life/";
let currImageNum = 1;
let sliderSpeed=5000;

// Function that shows or hides the list of courses
// for a particular semester upon user click
function toggleCourseList(listObject) {
    let semester = listObject.id;
    let semCourseList = "courselist" + semester;
    let lst = document.getElementById(semCourseList);
    if(listObject.className === "up")
    {
        lst.style.display = "block";
        document.getElementById(semester).className = "down";
    } else {
        lst.style.display = "none";
        document.getElementById(semester).className = "up";
    }
}


// function that changes slideshow's picture to the next 
// picture when the user presses the next button
function next() {
    currImageNum++;
    if(currImageNum === 17)
    {
        currImageNum = 1;
    }
    clearInterval(myTimer);
    let newImgPath = loc + currImageNum + ".jpg";
    document.getElementById("galleryimg").src = newImgPath;
    myTimer = setInterval(loopOverImages, sliderSpeed);
}

// function that changes slideshow's picture to the previous 
// picture when the user presses the previous button
function prev() {
    currImageNum--;
    if(currImageNum === 0)
    {
        currImageNum = 16;
    }
    clearInterval(myTimer);
    let newImgPath = loc + currImageNum + ".jpg";
    document.getElementById("galleryimg").src = newImgPath;
    myTimer = setInterval(loopOverImages, sliderSpeed);
}

// function that is called every sliderSpeed seconds
// changes slideshow to display next image in list of 
// images
function loopOverImages() 
{
    currImageNum++;
    if(currImageNum === 17)
    {
        currImageNum = 1;
    }
    let newImgPath = loc + currImageNum + ".jpg";
    document.getElementById("galleryimg").src = newImgPath;
}
myTimer = setInterval(loopOverImages, sliderSpeed);

// modifies slider speed if user changes value
// of slider on gallery page
function sliderMoved() {
    let sliderValue = document.getElementById("galleryslider").value;
    sliderSpeed = sliderValue * 1000;
    clearInterval(myTimer);
    myTimer = setInterval(loopOverImages, sliderSpeed);
}
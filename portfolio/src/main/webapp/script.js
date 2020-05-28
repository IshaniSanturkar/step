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

// Function that shows or hides the list of courses
// for a particular semester upon user click


const loc = "/images/life/";
let index = 1;
let sliderSpeed=5000;

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

// gets the number of the image currently being 
// displayed on the slideshow
function getImageNum() {
    let currImg = document.getElementById("galleryimg");
    let imgName = currImg.src;
    let startOfImgNum = imgName.indexOf(loc) + loc.length;
    let endOfImgNum = imgName.indexOf(".jpg");
    let imgNum = Number(imgName.slice(startOfImgNum, endOfImgNum));
    return imgNum;
}

// function that changes slideshow's picture to the next 
// picture when the user presses the next button
function next() {
    let imgNum = getImageNum();
    let newImageNum = (imgNum + 1);
    index++;
    if(newImageNum === 17)
    {
        newImageNum = 1;
        index = 1;
    }
    clearInterval(myTimer);
    let newImgPath = loc + newImageNum + ".jpg";
    document.getElementById("galleryimg").src = newImgPath;
    myTimer = setInterval(loopOverImages, sliderSpeed);
}

// function that changes slideshow's picture to the previous 
// picture when the user presses the previous button
function prev() {
    let imgNum = getImageNum();
    let newImageNum = (imgNum - 1);
    index--;
    if(newImageNum === 0)
    {
        newImageNum = 16;
        index = 16;
    }
    clearInterval(myTimer);
    let newImgPath = loc + newImageNum + ".jpg";
    document.getElementById("galleryimg").src = newImgPath;
    myTimer = setInterval(loopOverImages, sliderSpeed);
}


function loopOverImages() 
{
    console.log(sliderSpeed);
    index++;
    if(index === 17)
    {
        index = 1;
    }
    let newImgPath = loc + index + ".jpg";
    document.getElementById("galleryimg").src = newImgPath;
}
myTimer = setInterval(loopOverImages, sliderSpeed);

function sliderMoved() {
    let sliderValue = document.getElementById("galleryslider").value;
    sliderSpeed = sliderValue * 1000;
    clearInterval(myTimer);
    myTimer = setInterval(loopOverImages, sliderSpeed);
}
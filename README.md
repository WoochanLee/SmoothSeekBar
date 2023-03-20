
SmoothSeekBar
===============
[![](https://jitpack.io/v/WoochanLee/SmoothSeekBar.svg)](https://jitpack.io/#WoochanLee/SmoothSeekBar)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A Smooth Seek Bar for smooth UI / UX.

<img src="https://github.com/WoochanLee/SmoothSeekBar/blob/main/screenshot/screenshot.gif?raw=true" width="50%"/>

Gradle
------
- settings.gradle
```
dependencyResolutionManagement {
    ...
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```
- build.gradle
```
dependencies {
    ...
    implementation 'com.github.WoochanLee:SmoothSeekBar:1.0.0'
}
```

Usage
-----
```xml
<com.woody.lee.library.smoothseekbar.SmoothSeekBar
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:animationSpeed="10"
        app:barColor="#aaaaaa"
        app:barEdgeRadius="0dp"
        app:barHeight="13dp"
        app:barHorizontalPadding="5dp"
        app:labelContents="0%|50%|100%"
        app:labelTextColor="#000000"
        app:labelTextSize="15dp"
        app:labelTopMargin="50dp"
        app:max="100"
        app:progress="50"
        app:progressColor="#000000"
        app:thumbWidth="30dp"
        app:thumbHeight="30dp"
        app:thumbHorizontalPadding="9dp"
        app:thumbSrc="@android:drawable/star_big_on" />
```

Limitations
-----------
* This custom view doesn't inherit `Google SeekBar`, it inherits `View`.

Changelog
---------
* **1.0.0**
  * Initial release

License
-------

    MIT License

    Copyright (c) 2023 WoochanLee

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.

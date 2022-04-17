# shtookacollection
A Java library that manages and plays audio from a Shtooka .tar collection. Those collections are downloadable from Shtooka's page: http://shtooka.net/download.php

Works entirely with random access file, so the .tar file or voice clips are not put in memory.

<h1>Usage example</h1>

```java
import java.io.IOException;

import com.github.sahlaysta.shtooka.ShtookaCollection;
import com.github.sahlaysta.shtooka.ShtookaVoiceClip;

public class Main {
    public static void main(String[] args) throws IOException {
        ShtookaCollection sc = new ShtookaCollection("C:\\cmn-caen-tan_flac.tar");
        ShtookaVoiceClip voiceClip = sc.getVoiceClip("效率");
        voiceClip.play(); //plays the voice clip audio
        sc.close();
    }
}
```

<h2>License</h2>
This library is distributed under the GNU General Public License v3.0.

    Copyright (C) 2022 Shtooka Collection Contributors
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with this program. If not, see <https://www.gnu.org/licenses/>.

The code for FLAC audio file decoding used by this library is obtained from Project Nayuki, distributed under the GNU General Public License v3.0<br>
Copyright (c) Project Nayuki https://www.nayuki.io/page/flac-library-java

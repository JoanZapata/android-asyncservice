> #### This is a work in progress, do not use, but feel free to open issues if you want to suggest anything!

![Logo](https://raw.githubusercontent.com/JoanZapata/android-kiss/master/logo.png)

## Motivation

**Kiss** is born from an [article](http://blog.joanzapata.com/robust-architecture-for-an-android-app/) I wrote a few weeks ago, which gave me a lot of feedbacks and interesting comments. **Kiss** manages threading and caching transparently in your Android app. It's an alternative to [AsyncTasks](http://developer.android.com/reference/android/os/AsyncTask.html), [Loaders](http://developer.android.com/guide/components/loaders.html), or more advanced libs like [RxJava](https://github.com/Netflix/RxJava), [Robospice](https://github.com/stephanenicolas/robospice), [Groundy](https://github.com/telly/groundy),… but **Kiss** focuses on keeping your code short and simple! Its name means *keep it simple, stupid*, an old [design principle](http://en.wikipedia.org/wiki/KISS_principle) invented in the 60s.

## Sample

**DemoService.java**
```java 
@KissService
public class DemoService {

    public User getUser(String name) {
        // Runs asynchronously.
        return ...;
    }

}
```

**DemoActivity.java**
```java
public class DemoActivity extends Activity {

    @InjectService 
    public DemoService service;

    @Override 
    protected void onCreate(Bundle savedInstanceState) {
        ...
        Kiss.inject(this);
        
        // Returns nothing directly
        service.getUser("joan");
    }

    @OnMessage 
    public void onUser(User e) {
        // Runs on UI thread.
    }

}
```

See the [Wiki](https://github.com/JoanZapata/android-kiss/wiki) for more information.

## License

```
Copyright 2014 Joan Zapata

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[![Build Status](https://travis-ci.org/JoanZapata/android-kiss.svg?branch=master)](https://travis-ci.org/JoanZapata/android-kiss)

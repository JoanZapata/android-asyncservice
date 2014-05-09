### This is a work in progress, do not use

![Logo](https://raw.githubusercontent.com/JoanZapata/android-kiss/master/logo.png)

## Motivation

**Kiss** is born from an [article](http://blog.joanzapata.com/robust-architecture-for-an-android-app/) I wrote a few weeks ago, which gave me a lot of feedbacks and interesting comments. **Kiss** manages threading and caching transparently in your Android app. It's an alternative to [AsyncTasks](http://developer.android.com/reference/android/os/AsyncTask.html), [Loaders](http://developer.android.com/guide/components/loaders.html), or more advanced libs like [RxJava](https://github.com/Netflix/RxJava), [Robospice](https://github.com/stephanenicolas/robospice), [Groundy](https://github.com/telly/groundy),… but **Kiss** focuses on keeping your code short and simple! Its name means *keep it simple, stupid*, an old [design principle](http://en.wikipedia.org/wiki/KISS_principle) invented in the 60s.

## Usage

Create a Kiss service

```java
// Annotate a class with @KissService
@KissService
public class DemoService {

    /*
      Define a method that takes arguments and return a result.
      You can make network calls and/or long running ops here.
    */
    @Cached
    public UserEvent getUser(Long id) {
        return ...;
    }

}
```

Then inject it in your activities (or any class) and use it!

```java
public class MainActivity extends Activity {

    // Inject the service
    @InjectService 
    public DemoService service;

    @Override 
    protected void onCreate(Bundle savedInstanceState) {
        ...
        
        // That's the only boilerplate code you'll need to write for this lib!
        Kiss.inject(this);
        
        // Then call the methods you want on the service
        service.getUser("joan");
    }

    /* 
        You receive results asynchronously here, on the UI thread.
        As a default, if the user was in cache, you'll receive
        a cached result first, and then the result of getUser()
        when it returns.
    */
    @Result 
    public void onUserFetched(UserEvent e) {
        ...
    }

}
```

## How does it work?

First of all, **Kiss** does not rely heavily upon reflection. It's an annotation processor, which generates code that will call your code directly at runtime, without reflection. The only bit of reflection is done to find and instantiate the injector for your class when you call ```inject()```.

// TODO schema

It generates a subclass of your ```KissService``` managing cache and threading for you, and an ```Injector``` for each class using ```@InjectService``` or ```@InjectResponse```, to avoid using reflection when you call ```Kiss.inject()```.

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

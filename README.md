# ExceptionHandler-Library
It is a library for generating bug reports.

### Integration
**Step 1**. Add Jitpack to your project-level build.gradle
``` 
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
**Step 2**. Add library dependency to your app-level build.gradle
```
dependencies {
    implementation 'com.github.MaxSavTeam:ExceptionHandler-Library:0.2.1'
}
```
**Step 3**. Add to code
```
Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(Activity callerActivity, Class<?> afterExceptionActivityClass, boolean useDefaultHandler));
```
Where  
``callerActivity`` is activity from which activity for showing error will be launched and from which will be got application id and application context.  
``afterExceptionActivity`` is activity to be launched when error occurred. In intent will be passed path to the stacktrace file ("path" extra). Pass `null` if you do not want to launch some activity.  
``useDefaultHandler`` indicates that after creating stacktrace file will be called ``Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread, Exception)``

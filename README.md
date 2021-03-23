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
    implementation 'com.github.MaxSavTeam:ExceptionHandler-Library:1.0'
}
```
**Step 3**. Add to code
```
Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(Context applicationContext, Class<?> afterExceptionActivityClass, boolean useDefaultHandler));
```
Where  
``applicationContext`` is application context from which will be got app info and launched after exception activity. Should not be null  
``afterExceptionActivity`` is activity to be launched when error occurred. In intent will be passed path to the stacktrace file ("path" extra). Pass `null` if you do not want to launch some activity.  
``useDefaultHandler`` indicates that after creating stacktrace file will be called ``Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread, Exception)``

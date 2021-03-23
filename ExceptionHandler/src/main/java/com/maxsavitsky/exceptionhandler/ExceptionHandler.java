package com.maxsavitsky.exceptionhandler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
	public enum CallTag {
		CALLED_MANUALLY,
		CALLED_TO_SEND_LOG,
		CALLED_FROM_EXCEPTION_HANDLER
	}

	private Context mApplicationContext;
	private final Class<?> mAfterExceptionActivity;
	private Thread.UncaughtExceptionHandler mDefaultHandler;

	private static Context sApplicationContext;

	/**
	 * @param applicationContext     Should not be null because handler should get app info (version name, code, etc.) and start after exception activity
	 * @param afterExceptionActivity The activity to be launched when error occurred.
	 *                               In intent will be passed path to the stacktrace file ("path" extra). Pass {@code null}
	 *                               if you do not want to launch some activity.
	 * @param useDefaultHandler      After creating report file default exception handler will be used.<br>
	 *                               If afterExceptionActivity not null then this parameter will be ignored
	 */
	public ExceptionHandler(@NonNull Context applicationContext, @Nullable Class<?> afterExceptionActivity, boolean useDefaultHandler) {
		mApplicationContext = applicationContext.getApplicationContext();
		sApplicationContext = mApplicationContext;
		mAfterExceptionActivity = afterExceptionActivity;
		if ( afterExceptionActivity == null && useDefaultHandler ) {
			mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		}
	}

	/**
	 * @param applicationContext     Should not be null because handler should get app info (version name, code, etc.) and start after exception activity
	 * @param afterExceptionActivity The activity to be launched when error occurred.
	 *                               In intent will be passed path to the stacktrace file ("path" extra)
	 *                               or {@code null} if some errors were during creating stacktrace file
	 *                               Pass {@code null} if you do not want to launch some activity.
	 */
	public ExceptionHandler(@NonNull Context applicationContext, @Nullable Class<?> afterExceptionActivity) {
		this( applicationContext, afterExceptionActivity, false );
	}

	/**
	 * Sets static application context to work with static methods such as {@link #justWriteException(Thread, Throwable)}, {@link #prepareLogToSend(Thread, Throwable)}
	 */
	public static void setApplicationContext(Context applicationContext) {
		ExceptionHandler.sApplicationContext = applicationContext.getApplicationContext();
	}

	@Override
	public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
		File stacktraceFile = prepareStacktrace( mApplicationContext, t, e, CallTag.CALLED_FROM_EXCEPTION_HANDLER );

		if ( mAfterExceptionActivity != null ) {
			Intent intent = new Intent( mApplicationContext, mAfterExceptionActivity );
			intent.putExtra( "path", stacktraceFile == null ? null : stacktraceFile.getPath() );
			intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_CLEAR_TASK
					| Intent.FLAG_ACTIVITY_NEW_TASK );

			PendingIntent pendingIntent = PendingIntent.getActivity( mApplicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT );
			AlarmManager mgr = (AlarmManager) mApplicationContext.getSystemService( Context.ALARM_SERVICE );
			mgr.set( AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent );
		}
		if ( mDefaultHandler != null ) {
			mDefaultHandler.uncaughtException( t, e );
		}
	}

	/**
	 * Writes exception to stacktrace file like after uncaught exception.
	 * Stacktrace file will be created with suffix -m
	 */
	public static void justWriteException(Context context, Thread t, Throwable tr) {
		prepareStacktrace( context.getApplicationContext(), t, tr, CallTag.CALLED_MANUALLY );
	}

	/**
	 * Writes exception to stacktrace file like after uncaught exception.
	 * Stacktrace file will be created with suffix -m
	 * <p>
	 * This method uses application context which has been set in constructor or with setApplicationContext method
	 *
	 * @throws IllegalArgumentException If no default application context set or context is null.
	 */
	public static void justWriteException(Thread t, Throwable tr) {
		if ( sApplicationContext == null ) {
			throw new IllegalArgumentException( "No default application context set. Please call setApplicationContext or use it after constructor" );
		}
		prepareStacktrace( sApplicationContext, t, tr, CallTag.CALLED_MANUALLY );
	}

	/**
	 * Writes exception to stacktrace file like after uncaught exception.
	 * Stacktrace file will be created with suffix -m-sl
	 * <p>
	 * This method uses application context which has been set in constructor or with setApplicationContext method
	 *
	 * @throws IllegalArgumentException If no default application context set or context is null.
	 */
	public static File prepareLogToSend(Thread t, Throwable tr) {
		if ( sApplicationContext == null ) {
			throw new IllegalArgumentException( "No default application context set. Please call setApplicationContext or use it after constructor" );
		}
		return prepareStacktrace( sApplicationContext, t, tr, CallTag.CALLED_TO_SEND_LOG );
	}

	/**
	 * Writes exception to stacktrace file like after uncaught exception.
	 * Stacktrace file will be created with suffix -m-sl
	 */
	public static File prepareLogToSend(Context applicationContext, Thread t, Throwable tr) {
		return prepareStacktrace( applicationContext.getApplicationContext(), t, tr, CallTag.CALLED_TO_SEND_LOG );
	}

	/**
	 * Clears all resources that were used (context, etc.).<br>
	 * If method called, handler will no longer receive uncaught exceptions and default handler should be recreated
	 */
	public void destroy() {
		mApplicationContext = null;
		mDefaultHandler = null;
	}

	private static File prepareStacktrace(Context applicationContext, Thread t, Throwable e, CallTag type) {
		e.printStackTrace();
		PackageInfo mPackageInfo = null;
		try {
			mPackageInfo = applicationContext.getPackageManager().getPackageInfo( applicationContext.getPackageName(), 0 );
		} catch (PackageManager.NameNotFoundException ex) {
			ex.printStackTrace();
		}

		File file = new File( applicationContext.getExternalFilesDir( null ) + "/stacktraces/" );
		if ( !file.exists() ) {
			if ( !file.mkdir() ) {
				return null;
			}
		}
		Date date = new Date( System.currentTimeMillis() );
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "dd.MM.yyyy HH:mm:ss", Locale.ROOT );
		SimpleDateFormat fileFormatter = new SimpleDateFormat( "dd-MM-yyyy_HH:mm:ss", Locale.ROOT );
		String formattedDate = fileFormatter.format( date );
		String suffix = "";
		if ( type == CallTag.CALLED_MANUALLY ) {
			suffix = "-m";
		} else if ( type == CallTag.CALLED_TO_SEND_LOG ) {
			suffix = "-m-sl";
		}
		file = new File( file.getPath() + "/stacktrace-" + formattedDate + suffix + ".txt" );

		try {
			if ( !file.createNewFile() ) {
				return null;
			}
		} catch (IOException ignored) {
			return null;
		}
		StringBuilder report = new StringBuilder();
		report.append( type.toString() ).append( "\n" );
		report.append( "Time: " ).append( simpleDateFormat.format( date ) ).append( "\n" )
				.append( "Thread name: " ).append( t.getName() ).append( "\n" )
				.append( "Thread id: " ).append( t.getId() ).append( "\n" )
				.append( "Thread state: " ).append( t.getState() ).append( "\n" )
				.append( "Package: " ).append( applicationContext.getPackageName() ).append( "\n" )
				.append( "Manufacturer: " ).append( Build.MANUFACTURER ).append( "\n" )
				.append( "Model: " ).append( Build.MODEL ).append( "\n" )
				.append( "Brand: " ).append( Build.BRAND ).append( "\n" )
				.append( "Android Version: " ).append( Build.VERSION.RELEASE ).append( "\n" )
				.append( "Android SDK: " ).append( Build.VERSION.SDK_INT ).append( "\n" );
		if ( mPackageInfo != null ) {
			report.append( "Version name: " ).append( mPackageInfo.versionName ).append( "\n" )
					.append( "Version code: " ).append( mPackageInfo.versionCode ).append( "\n" );
		}
		printStackTrace( e, report );
		report.append( "Caused by:\n" );
		Throwable cause = e.getCause();
		if ( cause != null ) {
			for (StackTraceElement element : cause.getStackTrace()) {
				report.append( "\tat " ).append( element.toString() ).append( "\n" );
			}
		} else {
			report.append( "\tN/A\n" );
		}
		try {
			FileWriter fr = new FileWriter( file, false );
			fr.write( report.toString() );
			fr.flush();
			fr.close();
		} catch (IOException ignored) {
		}
		return file;
	}

	private static void printStackTrace(Throwable t, StringBuilder builder) {
		if ( t == null ) {
			return;
		}
		StackTraceElement[] stackTraceElements = t.getStackTrace();
		builder
				.append( "Exception: " ).append( t.getClass().getName() ).append( "\n" )
				.append( "Message: " ).append( t.getMessage() ).append( "\n" )
				.append( "Stacktrace:\n" );
		for (StackTraceElement stackTraceElement : stackTraceElements) {
			builder.append( "\t" ).append( stackTraceElement.toString() ).append( "\n" );
		}
	}

}

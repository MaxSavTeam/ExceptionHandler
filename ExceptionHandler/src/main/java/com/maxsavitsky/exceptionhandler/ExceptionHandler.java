package com.maxsavitsky.exceptionhandler;

import android.app.Activity;
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
	public enum CALL_TAGS{
		CALLED_MANUALLY,
		CALLED_FROM_ALERT_DIALOG_TO_SEND_LOG,
		CALLED_FROM_EXCEPTION_HANDLER
	}

	private final Context mApplicationContext;
	private final Activity mActivity;
	private final Class<?> mAfterExceptionActivity;

	public ExceptionHandler(Activity callerActivity, @Nullable Class<?> afterExceptionActivity){
		mActivity = callerActivity;
		mApplicationContext = callerActivity.getApplicationContext();
		mAfterExceptionActivity = afterExceptionActivity;
	}

	@Override
	public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
		File stacktraceFile = prepareStacktrace( t, e, CALL_TAGS.CALLED_FROM_EXCEPTION_HANDLER );

		if(mAfterExceptionActivity != null) {
			Intent intent = new Intent( mActivity, mAfterExceptionActivity );
			intent.putExtra( "crash", true ).putExtra( "path", stacktraceFile.getPath() );
			intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_CLEAR_TASK
					| Intent.FLAG_ACTIVITY_NEW_TASK );

			PendingIntent pendingIntent = PendingIntent.getActivity( mApplicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT );
			AlarmManager mgr = (AlarmManager) mApplicationContext.getSystemService( Context.ALARM_SERVICE );
			mgr.set( AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent );
		}
		mActivity.finish();
		System.exit( 1 );
	}

	public static void justWriteException(Thread t, Throwable tr){
		new ExceptionHandler( null, null ).prepareStacktrace( t, tr, CALL_TAGS.CALLED_MANUALLY );
	}

	private File prepareStacktrace(Thread t, Throwable e, CALL_TAGS type) {
		e.printStackTrace();
		PackageInfo mPackageInfo = null;
		try {
			mPackageInfo = mApplicationContext.getPackageManager().getPackageInfo( mApplicationContext.getPackageName(), 0 );
		} catch (PackageManager.NameNotFoundException ex) {
			ex.printStackTrace();
		}

		File file = new File( mApplicationContext.getExternalFilesDir( null ) + "/stacktraces/" );
		if ( !file.exists() ) {
			file.mkdir();
		}
		Date date = new Date( System.currentTimeMillis() );
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "dd.MM.yyyy HH:mm:ss", Locale.ROOT );
		SimpleDateFormat fileFormatter = new SimpleDateFormat( "dd-MM-yyyy_HH:mm:ss", Locale.ROOT );
		String formattedDate = fileFormatter.format( date );
		String suffix = "";
		if(type == CALL_TAGS.CALLED_MANUALLY)
			suffix = "-m";
		else if(type == CALL_TAGS.CALLED_FROM_ALERT_DIALOG_TO_SEND_LOG)
			suffix = "-m-ad-sl";
		file = new File( file.getPath() + "/stacktrace-" + formattedDate + suffix + ".txt" );

		try {
			file.createNewFile();
		} catch (IOException ignored) {
		}
		StringBuilder report = new StringBuilder();
		report.append( type.toString() ).append( "\n" );
		report.append( "Time: " ).append( simpleDateFormat.format( date ) ).append( "\n" )
				.append( "Thread name: " ).append( t.getName() ).append( "\n" )
				.append( "Thread id: " ).append( t.getId() ).append( "\n" )
				.append( "Thread state: " ).append( t.getState() ).append( "\n" )
				.append( "Package: " ).append( mApplicationContext.getPackageName() ).append( "\n" )
				.append( "Manufacturer: " ).append( Build.MANUFACTURER ).append( "\n" )
				.append( "Model: " ).append( Build.MODEL ).append( "\n" )
				.append( "Brand: " ).append( Build.BRAND ).append( "\n" )
				.append( "Android Version: " ).append( Build.VERSION.RELEASE ).append( "\n" )
				.append( "Android SDK: " ).append( Build.VERSION.SDK_INT ).append( "\n" );
		if(mPackageInfo != null) {
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

	private void printStackTrace(Throwable t, StringBuilder builder) {
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

package com.maxsavitsky.exceptionhandlerexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.maxsavitsky.exceptionhandler.ExceptionHandler;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		Thread.setDefaultUncaughtExceptionHandler( new ExceptionHandler( this, AfterExceptionActivity.class ) );

		findViewById( R.id.btnMakeException ).setOnClickListener( view->{
			throw new RuntimeException("Exception handler test");
		} );
	}
}
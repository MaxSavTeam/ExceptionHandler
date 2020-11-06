package com.maxsavitsky.exceptionhandlerexample;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class AfterExceptionActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_after_exception );

		String path = getIntent().getStringExtra( "path" );
		findViewById( R.id.btnShowStacktrace ).setOnClickListener( view->{
			if ( path == null ) {
				Toast.makeText( this, R.string.stacktrace_file_not_created, Toast.LENGTH_SHORT ).show();
			} else {
				try (FileReader fr = new FileReader( new File( path ) )) {
					String msg = "";
					while ( fr.ready() ) {
						msg = String.format( "%s%c", msg, (char) fr.read() );
					}

					AlertDialog.Builder builder = new AlertDialog.Builder( this );
					builder.setMessage( msg )
							.setCancelable( false )
							.setPositiveButton( "OK", (dialog, which)->dialog.cancel() );
					builder.show();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} );
	}
}
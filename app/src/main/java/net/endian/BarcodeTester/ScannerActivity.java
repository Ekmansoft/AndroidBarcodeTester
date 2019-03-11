package net.endian.BarcodeTester;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuItem;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import androidx.core.app.NavUtils;
import androidx.core.content.FileProvider;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ScannerActivity extends AppCompatActivity implements GMLBarcodeReader.BarcodeCallback
{
  /**
   * Whether or not the system UI should be auto-hidden after
   * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
   */
  private static final boolean AUTO_HIDE = true;

  //private FirebaseVisionBarcodeDetector detector;

  /**
   * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
   * user interaction before hiding the system UI.
   */
  private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

  /**
   * Some older devices needs a small delay between UI widget updates
   * and a change of the status and navigation bar.
   */
  private static final int UI_ANIMATION_DELAY = 300;
  private final Handler mHideHandler = new Handler();
  private View mContentView;
  private final Runnable mHidePart2Runnable = new Runnable()
  {
    @SuppressLint("InlinedApi")
    @Override
    public void run()
    {
      // Delayed removal of status and navigation bar

      // Note that some of these constants are new as of API 16 (Jelly Bean)
      // and API 19 (KitKat). It is safe to use them, as they are inlined
      // at compile-time and do nothing on earlier devices.
      mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
          | View.SYSTEM_UI_FLAG_FULLSCREEN
          | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
          | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
          | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
          | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
  };
  private View mControlsView;
  private final Runnable mShowPart2Runnable = new Runnable()
  {
    @Override
    public void run()
    {
      // Delayed display of UI elements
      ActionBar actionBar = getSupportActionBar();
      if (actionBar != null)
      {
        actionBar.show();
      }
      mControlsView.setVisibility(View.VISIBLE);
    }
  };
  private boolean mVisible;
  private final Runnable mHideRunnable = new Runnable()
  {
    @Override
    public void run()
    {
      hide();
    }
  };

  private ImageView imageView;

  private GMLBarcodeReader barcodeReader;
  /**
   * Touch listener to use for in-layout UI controls to delay hiding the
   * system UI. This is to prevent the jarring behavior of controls going away
   * while interacting with activity UI.
   */
  private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener()
  {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent)
    {
      if (AUTO_HIDE)
      {
        delayedHide(AUTO_HIDE_DELAY_MILLIS);
      }
      return false;
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_scanner);
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null)
    {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    mVisible = true;
    mControlsView = findViewById(R.id.fullscreen_content_controls);
    mContentView = findViewById(R.id.fullscreen_content);
    imageView = findViewById(R.id.imageView);


    // Set up the user interaction to manually show or hide the system UI.
    mContentView.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View view)
      {
        toggle();
      }
    });

    // Upon interacting with UI controls, delay any scheduled hide()
    // operations to prevent the jarring behavior of controls going away
    // while interacting with the UI.
    findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState)
  {
    super.onPostCreate(savedInstanceState);

    // Trigger the initial hide() shortly after the activity has been
    // created, to briefly hint to the user that UI controls
    // are available.
    delayedHide(100);

    barcodeReader = new GMLBarcodeReader(this, this);
    dispatchTakePictureIntent();
  }

  String currentPhotoPath;

  private File createImageFile() throws IOException
  {
    // Create an image file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    File image = File.createTempFile(
        imageFileName,  /* prefix */
        ".jpg",         /* suffix */
        storageDir      /* directory */
    );

    // Save a file: path for use with ACTION_VIEW intents
    currentPhotoPath = image.getAbsolutePath();
    return image;
  }

  static final int REQUEST_TAKE_PHOTO = 1;

  private void dispatchTakePictureIntent() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // Ensure that there's a camera activity to handle the intent
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      // Create the File where the photo should go
      File photoFile = null;
      try {
        photoFile = createImageFile();
      } catch (IOException ex) {
        // Error occurred while creating the File
        Log.e("scanner","IOException " + ex.toString());
      }
      // Continue only if the File was successfully created
      if (photoFile != null) {
        Log.d("scanner","dispatch take pic " + photoFile.getName());
        Uri photoURI = FileProvider.getUriForFile(this,
            "net.endian.BarcodeTester.fileprovider",
            photoFile);
        if (photoURI != null)
        {
          Log.d("scanner", "dispatch take pic uri " + photoURI.getEncodedPath());
          takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
          //takePictureIntent.setData(photoURI);
          startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }else
        {
          Log.e("scanner", "dispatch take pic photouri failed ");
        }
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    int id = item.getItemId();
    if (id == android.R.id.home)
    {
      // This ID represents the Home or Up button.
      NavUtils.navigateUpFromSameTask(this);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void toggle()
  {
    if (mVisible)
    {
      hide();
    } else
    {
      show();
    }
  }

  private void hide()
  {
    // Hide UI first
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null)
    {
      actionBar.hide();
    }
    mControlsView.setVisibility(View.GONE);
    mVisible = false;

    // Schedule a runnable to remove the status and navigation bar after a delay
    mHideHandler.removeCallbacks(mShowPart2Runnable);
    mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK)
    {
      Bundle extras = data.getExtras();

      String action = data.getAction();
      Log.d("scanner","action " + action);
      Log.d("scanner","contents " + data.describeContents());

      String scheme =  data.getScheme();
      Log.d("scanner","scheme " + scheme);


      Set<String> cats = data.getCategories();

      if(cats != null)
      {
        for(String s : cats)
        {
          Log.d("scanner","cat " + s);
        }
      }else
      {
        Log.d("scanner","cats null");

      }

      if (extras != null)
      {
        Bitmap imageBitmap = (Bitmap) extras.get("data");
        if (imageBitmap != null)
        {
          Log.d("scanner", "bitmap " + imageBitmap);
          imageView.setImageBitmap(imageBitmap);
        }
      } else
      {
        Log.e("scanner","Could not get new extra data");
      }
      Uri uri = data.getData();
      Log.d("scanner","file path " + currentPhotoPath);
      Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);

      if (bitmap  != null)
      {
        barcodeReader.AnalyzeImage(bitmap, 11);
        imageView.setImageBitmap(bitmap);
      } else
      {
        Log.e("scanner","bitmap decode failed " );
      }

      if (uri != null)
      {
        Log.d("scanner","bitmap " + uri);
        //imageView.setImageBitmap(imageBitmap);
      } else
      {
        Log.e("scanner","Could not get new uri");
      }
    }
  }

  @SuppressLint("InlinedApi")
  private void show()
  {
    // Show the system bar
    mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    mVisible = true;

    // Schedule a runnable to display UI elements after a delay
    mHideHandler.removeCallbacks(mHidePart2Runnable);
    mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
  }

  /**
   * Schedules a call to hide() in delay milliseconds, canceling any
   * previously scheduled calls.
   */
  private void delayedHide(int delayMillis)
  {
    mHideHandler.removeCallbacks(mHideRunnable);
    mHideHandler.postDelayed(mHideRunnable, delayMillis);
  }

  @Override
  public void barcodeAnalysisDone(int reference, boolean success, AbstractList<GMLBarcodeReader.BarcodeResult> barcodes)
  {
    Log.d("scanner", "results received " + reference + " " + success);

    if (barcodes != null)
    {
      for (GMLBarcodeReader.BarcodeResult barcode : barcodes)
      {
        Log.d("scanner", "results received " + barcode.BoundingBox + " " + barcode.Type + " " + barcode.Content);
      }
    }
  }
}

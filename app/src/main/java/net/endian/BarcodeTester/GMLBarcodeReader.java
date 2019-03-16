package net.endian.BarcodeTester;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

import static com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.TYPE_CALENDAR_EVENT;
import static com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.TYPE_CONTACT_INFO;
import static com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.TYPE_DRIVER_LICENSE;
import static com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.TYPE_EMAIL;
import static com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.TYPE_GEO;
import static com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.TYPE_ISBN;
import static com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.TYPE_PHONE;
import static com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.TYPE_PRODUCT;
import static com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.TYPE_SMS;
import static com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.TYPE_TEXT;
import static com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.TYPE_UNKNOWN;

public class GMLBarcodeReader
{
  public class BarcodeResult
  {
    public String Content;
    public Rect BoundingBox;
    public Point ImageSize;
    public String Type;
  }

  public interface BarcodeCallback {
    void barcodeAnalysisDone(int reference, boolean success, AbstractList<BarcodeResult> barcodes);
  }

  private FirebaseVisionBarcodeDetector detector;
  private BarcodeCallback callback;

  public GMLBarcodeReader(Context context, BarcodeCallback callback)
  {
    FirebaseApp.initializeApp(context);
    this.detector = FirebaseVision.getInstance().getVisionBarcodeDetector(
        new FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_ALL_FORMATS)
            .build());

    this.callback = callback;
  }

  private String GetBarcodeType(int typeId)
  {
    switch (typeId)
    {
      case TYPE_ISBN:
        return "ISBN";
      case TYPE_PRODUCT:
        return "PRODUCT";
      case TYPE_DRIVER_LICENSE:
        return "DRIVER_LICENSE";
      case TYPE_CALENDAR_EVENT:
        return "CALENDAR_EVENT";
      case TYPE_CONTACT_INFO:
        return "CONTACT_INFO";
      case TYPE_EMAIL:
        return "EMAIL";
      case TYPE_GEO:
        return "GEO";
      case TYPE_PHONE:
        return "PHONE";
      case TYPE_SMS:
        return "SMS";
      case TYPE_TEXT:
        return "TEXT";
      case TYPE_UNKNOWN:
        return "UNKNOWN";
      default:
        return "UNKNOWN_ID:" + typeId;
    }
  }

  public void AnalyzeImage(final Bitmap bitmap, final int reference)
  {
    FirebaseVisionImage fbImage = FirebaseVisionImage.fromBitmap(bitmap);
    Task<List<FirebaseVisionBarcode>> task =  detector.detectInImage(fbImage);

    if (task != null)
    {
      task.addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
        @Override
        public void onSuccess(List<FirebaseVisionBarcode> barcodes) {
          // Task completed successfully
          // ...
          ArrayList<BarcodeResult> barcodeResults = new ArrayList<>();
          Log.d("scanner", "barcode list no " + barcodes.size());
          for(FirebaseVisionBarcode barcode : barcodes)
          {
            BarcodeResult result = new BarcodeResult();
            result.Content = barcode.getDisplayValue();
            result.BoundingBox = barcode.getBoundingBox();
            result.ImageSize = new Point(bitmap.getWidth(), bitmap.getHeight());

            Log.d("scanner", "barcode " + barcode.getRawValue() + " " +
                barcode.getDisplayValue() + " " +
                barcode.getBoundingBox() + " " +
                barcode.getUrl() + " " +
                barcode.getValueType() + " " +
                barcode.getContactInfo() + " " +
                barcode.getFormat());
            result.Type = GetBarcodeType(barcode.getValueType());
            barcodeResults.add(result);
          }
          callback.barcodeAnalysisDone(reference, true, barcodeResults);
        }
      }).addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
          // Task failed with an exception
          Log.d("scanner", "Exception " + e.toString());
          // ...
          callback.barcodeAnalysisDone(reference, false, null);
        }
      });
    }
  }
}

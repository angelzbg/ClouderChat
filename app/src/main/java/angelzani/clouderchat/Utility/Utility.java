package angelzani.clouderchat.Utility;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class Utility {
    public static Bitmap CropBitmapCenterCircle(Bitmap b){
        Bitmap bitmap;
        if (b.getWidth() >= b.getHeight()){
            bitmap = Bitmap.createBitmap(
                    b,
                    b.getWidth()/2 - b.getHeight()/2,
                    0,
                    b.getHeight(),
                    b.getHeight()
            );
        }else{
            bitmap = Bitmap.createBitmap(
                    b,
                    0,
                    b.getHeight()/2 - b.getWidth()/2,
                    b.getWidth(),
                    b.getWidth()
            );
        }
        bitmap = Bitmap.createScaledBitmap(bitmap, 250, 250, true);

        Bitmap circleBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        BitmapShader shader = new BitmapShader (bitmap,  Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setShader(shader);
        Canvas c = new Canvas(circleBitmap);
        c.drawCircle(bitmap.getWidth()/2, bitmap.getHeight()/2, bitmap.getWidth()/2, paint);

        return circleBitmap;
    }
    public static String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b, Base64.NO_WRAP);
        return imageEncoded;
    }
    public static Bitmap StringToBitMap(String encodedString){
        byte[] decodedByte;
        try {
            decodedByte = Base64.decode(encodedString, 0);
        } catch(Exception e) {
            return null;
        }
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }

    public static void setMargins (View v, int left, int top, int right, int bottom) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            v.requestLayout();
        }
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static Bitmap resizeBitmapTo1024pxMax(Bitmap bitmap){
        if(bitmap==null) return null;

        // Оразмеряване преди изпращане
        final int maxWidth=1024, maxHeight=1024;
        int finalWidth = 1024, finalHeight = 1024;
        int bitmapWidth = bitmap.getWidth(), bitmapHeight = bitmap.getHeight();

        if(bitmapWidth > maxWidth || bitmapHeight > maxHeight) { // трябва оразмеряване

            if(bitmapWidth > maxWidth && bitmapHeight <= maxHeight) { // само ширината излиза извън размера
                //Toast.makeText(getApplicationContext(), "Само ширината излиза извън размера", Toast.LENGTH_LONG).show();
                finalWidth = 1024;
                double scale = (bitmapWidth*1.00) / (bitmapHeight*1.00);
                finalHeight = (int) (finalWidth / scale);

            } else if (bitmapHeight > maxHeight && bitmapWidth <= maxWidth) { // само височината излиза извън размера
                //Toast.makeText(getApplicationContext(), "Само височината излиза извън размера", Toast.LENGTH_LONG).show();
                finalHeight = 1024;
                double scale = (bitmapHeight*1.00)/(bitmapWidth*1.00);
                finalWidth = (int)(finalHeight/scale);

            } else { // и двете излизат извън размера
                //Toast.makeText(getApplicationContext(), "И двете излизат извън размера", Toast.LENGTH_LONG).show();

                if(bitmapWidth > bitmapHeight) { // ширината е по-голяма
                    //Toast.makeText(getApplicationContext(), "И двете излизат извън размера -> ширината е по-голяма", Toast.LENGTH_LONG).show();
                    finalWidth = 1024;
                    double scale = (bitmapWidth*1.00) / (bitmapHeight*1.00);
                    finalHeight = (int) (finalWidth / scale);

                } else if (bitmapHeight > bitmapWidth) { // височината е по-голяма
                    //Toast.makeText(getApplicationContext(), "И двете излизат извън размера -> височината е по-голяма", Toast.LENGTH_LONG).show();
                    finalHeight = 1024;
                    double scale = (bitmapHeight*1.00)/(bitmapWidth*1.00);
                    finalWidth = (int)(finalHeight/scale);

                } else if(bitmapHeight == bitmapWidth) { // равни са -> квадрат
                    //Toast.makeText(getApplicationContext(), "И двете излизат извън размера -> квадрат", Toast.LENGTH_LONG).show();
                    finalWidth = 1024;
                    finalHeight = 1024;
                }

            }

        } else { // не трябва оразмряване
            return bitmap;
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
    }//end of resizeBitmapTo1024pxMax()

    public static Bitmap resizeBitmapToMini200pxMax(Bitmap bitmap){
        if(bitmap==null) return null;

        // Оразмеряване преди изпращане
        final int maxWidth=200, maxHeight=200;
        int finalWidth = 200, finalHeight = 200;
        int bitmapWidth = bitmap.getWidth(), bitmapHeight = bitmap.getHeight();

        if(bitmapWidth > maxWidth || bitmapHeight > maxHeight) { // трябва оразмеряване

            if(bitmapWidth > maxWidth && bitmapHeight <= maxHeight) { // само ширината излиза извън размера
                //Toast.makeText(getApplicationContext(), "Само ширината излиза извън размера", Toast.LENGTH_LONG).show();
                finalWidth = 200;
                double scale = (bitmapWidth*1.00) / (bitmapHeight*1.00);
                finalHeight = (int) (finalWidth / scale);

            } else if (bitmapHeight > maxHeight && bitmapWidth <= maxWidth) { // само височината излиза извън размера
                //Toast.makeText(getApplicationContext(), "Само височината излиза извън размера", Toast.LENGTH_LONG).show();
                finalHeight = 200;
                double scale = (bitmapHeight*1.00)/(bitmapWidth*1.00);
                finalWidth = (int)(finalHeight/scale);

            } else { // и двете излизат извън размера
                //Toast.makeText(getApplicationContext(), "И двете излизат извън размера", Toast.LENGTH_LONG).show();

                if(bitmapWidth > bitmapHeight) { // ширината е по-голяма
                    //Toast.makeText(getApplicationContext(), "И двете излизат извън размера -> ширината е по-голяма", Toast.LENGTH_LONG).show();
                    finalWidth = 200;
                    double scale = (bitmapWidth*1.00) / (bitmapHeight*1.00);
                    finalHeight = (int) (finalWidth / scale);

                } else if (bitmapHeight > bitmapWidth) { // височината е по-голяма
                    //Toast.makeText(getApplicationContext(), "И двете излизат извън размера -> височината е по-голяма", Toast.LENGTH_LONG).show();
                    finalHeight = 200;
                    double scale = (bitmapHeight*1.00)/(bitmapWidth*1.00);
                    finalWidth = (int)(finalHeight/scale);

                } else if(bitmapHeight == bitmapWidth) { // равни са -> квадрат
                    //Toast.makeText(getApplicationContext(), "И двете излизат извън размера -> квадрат", Toast.LENGTH_LONG).show();
                    finalWidth = 200;
                    finalHeight = 200;
                }

            }

        } else { // не трябва оразмряване
            return bitmap;
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
    }//end of resizeBitmapTo1024pxMax()

    public static Bitmap createBlurBitmapFromScreen(View mainView, Context applicationContext, int width, int height) {

        int dstWidth = 480, dstHeight = 480;
        if(width < height) {
            double scale = (height*1.00)/(width*1.00);
            dstHeight = (int)(dstWidth*scale);
        } else if(width > height) {
            double scale = (width*1.00)/(height*1.00);
            dstHeight = (int)(dstWidth/scale);
        } /*else { //равни са
            dstHeight = 480;
        }*/

        Bitmap bitmap = null;
        mainView.setDrawingCacheEnabled(true);
        bitmap = Bitmap.createBitmap(mainView.getDrawingCache());
        mainView.setDrawingCacheEnabled(false);
        bitmap = Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, false);

        Bitmap result = null;
        try {
            RenderScript rsScript = RenderScript.create(applicationContext);
            Allocation alloc = Allocation.createFromBitmap(rsScript, bitmap);

            ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rsScript, Element.U8_4(rsScript));
            blur.setRadius(21);
            blur.setInput(alloc);

            result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Allocation outAlloc = Allocation.createFromBitmap(rsScript, result);

            blur.forEach(outAlloc);
            outAlloc.copyTo(result);

            rsScript.destroy();
        } catch (Exception e) {
            return bitmap;
        }
        return result;
    }//end of createBlurBitmapFromScreen()

}
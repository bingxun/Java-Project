package com.google.android.gms.location.sample.locationupdates;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.GpsSatellite;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bing Xun on 12/21/2016.
 */
public class SatelliteView extends View {

    private final static String TAG = "SatelliteView";

    private final static double DEG_TO_RAD = Math.PI / 180.0;
    private final static float AXIS_TEXT_SIZE = 1;
    private final static float AXIS_TEXT_MARGIN = 4;
    private final static float SAT_TEXT_SIZE = 16;
    private final static int NUM_AXIS_RADIUS = 3;
    private final static String LABEL_NORTH = "N";

    private Bitmap mBitmapAxis = null;
    private Paint mPaintCircle = null;
    private Paint mPaintText = null;

    private float mDensity = 0;

    private float mCenterX = 0;
    private float mCenterY = 0;
    private float mTextHalfHeight = 0;
    private float mCompassRadius = 0;
    private float mSatRadius = 0;

    private List<SatellitePoint> mList = new ArrayList<SatellitePoint>();


    /**
     * === constructor ===
     */
    public SatelliteView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    /**
     * === constructor ===
     */
    public SatelliteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    /**
     * === constructor ===
     */
    public SatelliteView(Context context) {
        super(context);
        initView(context);
    }

    /**
     * draw round satellite
     */
    private void initView(Context context) {
        getScaledDensity(); //call method
        mPaintCircle = new Paint(); //create Paint obj
        mPaintCircle.setStyle(Paint.Style.STROKE); //set style to stroke
        mPaintCircle.setColor(Color.RED); //stroke color red
        mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintText.setTextSize(SAT_TEXT_SIZE * mDensity); //set the prn no. inside the circle
        Paint.FontMetrics metrics = mPaintText.getFontMetrics(); //call fontmetrics
        mTextHalfHeight = (metrics.ascent + metrics.descent) / 2; //uses ascent and descent to put the prn no. in the middle of the circle
    }

    /**
     * getScaledDensity
     */
    public float getScaledDensity() { //display the metrics on the compass
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        mDensity = metrics.scaledDensity; //scale the font size display = 1.5
        return mDensity;
    }
    @Override
    public void onWindowFocusChanged( boolean hasFocus ) { //call when the phone window gain focus
        int width = getWidth(); //get the width of the window 492
        int height = getHeight(); //get the height of the window 454
        mCenterX = width / 2; //set origin centre point
        mCenterY = ( height + ( AXIS_TEXT_SIZE + AXIS_TEXT_MARGIN ) * mDensity ) / 2; //set centre of y axis as the height of the window
                                                                                      //(454 + (4 + 4) * 1.5) / 2
        mCompassRadius = (float) ( 0.95 * Math.min( mCenterX, mCenterY ) ); //0.95 * Min (492,454)   0.95*454 = 431.3
        mSatRadius = 12 * mDensity ; //12*1.5 = 18
        initAxis( width, height ); //call method pass width = 492 and height = 454
    }
    /**
     * draw compass
     */
    private void initAxis(int width, int height) {
        Paint paint_line = new Paint();
        Paint paint_circle = new Paint();
        paint_circle.setStyle(Paint.Style.STROKE);
        Paint paint_text = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint_text.setTextSize(AXIS_TEXT_SIZE * mDensity);

        float line_x0 = mCenterX - mCompassRadius;
        float line_x1 = mCenterX + mCompassRadius;
        float line_y0 = mCenterY - mCompassRadius;
        float line_y1 = mCenterY + mCompassRadius;
        float text_x = mCenterX - paint_text.measureText( LABEL_NORTH ) ;
        float text_y = line_y0 - ( AXIS_TEXT_MARGIN * mDensity );
        float div = mCompassRadius / NUM_AXIS_RADIUS;

        mBitmapAxis = Bitmap.createBitmap( width, height, Bitmap.Config.RGB_565 ); //bitmap describes how pixel is stored and display. Red 5 bits, Green 6 bits and Blue 5 bits
        Canvas canvas = new Canvas();
        canvas.setBitmap(mBitmapAxis);
        canvas.drawColor(Color.WHITE);
        canvas.drawText(LABEL_NORTH, text_x, text_y, paint_text);
        canvas.drawLine(line_x0, mCenterY, line_x1, mCenterY, paint_line);
        canvas.drawLine(mCenterX, line_y0, mCenterX, line_y1, paint_line);
        for (int i = 0; i < NUM_AXIS_RADIUS; i++) {
            float r = div * (i + 1); //draw number of rounds in the compass using for loop
            canvas.drawCircle( mCenterX, mCenterY, r, paint_circle ); //draw the compass
        }
    }

    @Override
    protected void onDraw(Canvas canvas){

        if ( mBitmapAxis != null ) {
            canvas.drawBitmap( mBitmapAxis, 0, 0, null );
        }

        for ( int i = 0; i < mList.size(); i++ ) {
            SatellitePoint point;
            point = mList.get(i);
            int getPrn = Integer.parseInt(point.name);
            if (getPrn >0 && getPrn <= 32) {
                float x = point.x * mCompassRadius + mCenterX;
                float y = point.y * mCompassRadius + mCenterY;
                float text_x = x - mPaintText.measureText(point.name) / 2;

                float text_y = y - mTextHalfHeight;
                canvas.drawCircle(x, y, mSatRadius, mPaintCircle);
                canvas.drawText(point.name, text_x, text_y, mPaintText);
            }
            else
            {
                System.out.println("Non GPS satellite detected! " + point.name);
            }
        }

    }

    /**
     * setList
     */
    public void setList( Iterable<GpsSatellite> list ) {
        mList.clear();
        for(GpsSatellite sat : list) {
            mList.add( new SatellitePoint(sat) );
        }
        invalidate();
    }

    /**
     * class SatellitePoint
     */
    private class SatellitePoint {
        public String name = "";
        public float x = 0;
        public float y = 0;

        public SatellitePoint(GpsSatellite sat)
        {
            name = Integer.toString( sat.getPrn() );
                float r = (90 - sat.getElevation()) / 90;
                double azimuth = (270 + sat.getAzimuth()) * DEG_TO_RAD;
                x = r * (float) Math.cos(azimuth);
                y = r * (float) Math.sin(azimuth);
        }
    }

    /**
     * log_d
     */
    @SuppressWarnings("unused")
    private void log_d( String str ) {
        Log.d( TAG, str );
    }


}

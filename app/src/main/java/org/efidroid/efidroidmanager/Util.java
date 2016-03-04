package org.efidroid.efidroidmanager;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.support.design.widget.AppBarLayout;
import android.util.TypedValue;
import android.view.ViewGroup;

import org.efidroid.efidroidmanager.activities.OperatingSystemEditActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class Util {
    public static OperatingSystemEditActivity.MultibootPartitionInfo getPartitionInfoByName(ArrayList<OperatingSystemEditActivity.MultibootPartitionInfo> list, String name) {
        for(OperatingSystemEditActivity.MultibootPartitionInfo info : list) {
            if(info.name.equals(name))
                return info;
        }

        return null;
    }

    public static String name2path(String name) {
        return name.replaceAll("\\W+", "_");
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE/Byte.SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(x);
        return buffer.array();
    }

    public static String byteToHexStr(byte b) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02X", b));
        return sb.toString();
    }

    public static void savePng(File file, Bitmap bitmap) throws IOException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }

            throw e;
        }
    }

    public static long ROUNDUP(long number, long alignment) {
        return (((number) + ((alignment)-1)) & ~((alignment)-1));
    }

    public static int getStatusBarHeight(Context context) {
      int result = 0;
      int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
      if (resourceId > 0) {
          result = context.getResources().getDimensionPixelSize(resourceId);
      }
      return result;
    }

    public static int getToolBarHeight(Context context) {
        TypedValue typedValue = new TypedValue();
        int indexOfAttrTextSize = 0;

        int[] textSizeAttr = new int[] { android.R.attr.actionBarSize };
        TypedArray a = context.obtainStyledAttributes(typedValue.data, textSizeAttr);
        int textSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
        a.recycle();

        return textSize;
    }

    public static void setToolBarHeight(final AppBarLayout appBarLayout, int heightDP, final boolean expand) {
        int animTime = appBarLayout.getContext().getResources().getInteger(android.R.integer.config_mediumAnimTime);

        // calculate new height
        int heightPX = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDP, appBarLayout.getContext().getResources().getDisplayMetrics());
        final int totalHeightPX = Util.getStatusBarHeight(appBarLayout.getContext()) + Util.getToolBarHeight(appBarLayout.getContext()) + heightPX;
        int startHeightPx = appBarLayout.getMeasuredHeight();
        if(startHeightPx==0) {
            startHeightPx = totalHeightPX;
            animTime = 0;
        }

        // create value animator
        ValueAnimator anim = ValueAnimator.ofInt(startHeightPx, totalHeightPX);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = appBarLayout.getLayoutParams();
                layoutParams.height = val;
                appBarLayout.setLayoutParams(layoutParams);
            }
        });

        // start animation
        anim.setDuration(animTime);
        appBarLayout.setExpanded(expand, animTime>0);
        anim.start();
    }
}

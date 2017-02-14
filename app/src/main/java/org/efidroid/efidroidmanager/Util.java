package org.efidroid.efidroidmanager;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.design.widget.AppBarLayout;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import org.efidroid.efidroidmanager.activities.OperatingSystemEditActivity;
import org.efidroid.efidroidmanager.types.ArgbEvaluator;
import org.efidroid.efidroidmanager.types.SystemPropertiesProxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public static List<String> getABIs() {
        ArrayList<String> abis = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            abis.addAll(Arrays.asList(Build.SUPPORTED_ABIS));
        }
        else {
            abis.add(Build.CPU_ABI);
            abis.add(Build.CPU_ABI2);
        }
        return abis;
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE/Byte.SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(x);
        return buffer.array();
    }

    public static String byteToHexStr(byte b) {
        return String.format("%02X", b);
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

    public static float dp2px(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static void setToolBarHeight(final AppBarLayout appBarLayout, int heightDP, final boolean expand) {
        int animTime = appBarLayout.getContext().getResources().getInteger(android.R.integer.config_mediumAnimTime);

        // calculate new height
        int heightPX = (int) Util.dp2px(appBarLayout.getContext(), heightDP);
        final int totalHeightPX = Util.getStatusBarHeight(appBarLayout.getContext()) + Util.getToolBarHeight(appBarLayout.getContext()) + heightPX;
        int startHeightPx = appBarLayout.getMeasuredHeight();
        if(startHeightPx==0) {
            startHeightPx = totalHeightPX;
            animTime = 0;
        }

        // create value animator
        final ViewGroup.LayoutParams layoutParams = appBarLayout.getLayoutParams();
        ValueAnimator anim = ValueAnimator.ofInt(startHeightPx, totalHeightPX);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                layoutParams.height = (int) (Integer) valueAnimator.getAnimatedValue();
                appBarLayout.setLayoutParams(layoutParams);
            }
        });
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                appBarLayout.setExpanded(expand, false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        // start animation
        anim.setDuration(animTime);
        anim.start();
    }

    public static void animateVisibility(final View v, final int visibility, int duration) {
        ValueAnimator anim = ValueAnimator.ofFloat(v.getVisibility() == View.VISIBLE ? 1 : 0, visibility == View.VISIBLE ? 1 : 0);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (Float) valueAnimator.getAnimatedValue();
                v.setAlpha(val);
            }
        });
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                v.setAlpha(1);
                v.setVisibility(visibility);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        anim.setDuration(duration);

        // show view now
        if(v.getVisibility() != View.VISIBLE)
            v.setAlpha(0);
        else v.setAlpha(1);
        v.setVisibility(View.VISIBLE);

        // start animation
        anim.start();
    }

    /**
     * Constructs and returns a ValueAnimator that animates between color values. A single
     * value implies that that value is the one being animated to. However, this is not typically
     * useful in a ValueAnimator object because there is no way for the object to determine the
     * starting value for the animation (unlike ObjectAnimator, which can derive that value
     * from the target object and property being animated). Therefore, there should typically
     * be two or more values.
     *
     * @param values A set of values that the animation will animate between over time.
     * @return A ValueAnimator object that is set up to animate between the given values.
     */
    public static ValueAnimator CompatAnimatorOfArgb(int... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(values);
        anim.setEvaluator(ArgbEvaluator.getInstance());
        return anim;
    }

    public static boolean isDeviceEncryptionEnabled(Context context) {
        final String status = SystemPropertiesProxy.get(context, "ro.crypto.state", "unsupported");
        return "encrypted".equalsIgnoreCase(status);
    }
}

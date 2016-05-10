/*
 * Copyright 2015 Shawn Wang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.in404.frametextview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatTextView;
import android.text.Layout;
import android.text.StaticLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

/**
 * Created by shawn
 * Data: 11/12/2015
 * Blog: effmx.com
 *
 * FrameTextView is a TextView can be interpolated or regularly rendered,
 *     the rendered content include: text content(for example: ["One", "Two", "Three"], 1 to 100),
 *     text color(for example: ["#000000", "#FFFFFF"]),
 *     text position, and so on.
 * <p>
 * Not only that, FrameTextView provide abundant custom features, allow you to custom
 *     the render TODO
 */

public class FrameTextView extends AppCompatTextView {
    static final String TAG = "FrameTextView";

    /**
     * INTERPOLATED_MODE
     * In this mode, FrameTextView will be rendered interpolated.
     * You can set all different kinds of {@link Interpolator} to custom render speed.
     */
    private static final int INTERPOLATED_MODE = 0;

    /**
     * INTERVAL_MODE
     * In this mode, FrameTextView will be rendered regularly.
     * You can set {@link #interval} to custom render interval.
     */
    private static final int INTERVAL_MODE = 1;

    /**
     * ASC, DESC, RANDOM, GRADUAL
     * These params will decide the sequence of {@link #textItems}, {@link #from} num,
     *     {@link #to} num and {@link #colorItems} will be displayed.
     * {@link #GRADUAL} only work for {@link #colorItems}
     */
    private static final int ASC = 0;
    private static final int DESC = 1;
    private static final int RANDOM = 2;
    private static final int GRADUAL = 3;

    /**
     * The maximum scrolling character
     */
    private static final int MAX_SCROLLING_CHAR = 2;

    /**
     * The move steps of new character,
     *     if move step is 10, {@link #onDraw(Canvas)} will scroll in new character in 10 times.
     *     if move step is 30, {@link #onDraw(Canvas)} will scroll in new character in 30 times.
     *     The speed will become slower when move step gets bigger.
     * SCROLL_MOVE_STEPS value correspond 30, 20, 10 with {@link #scrollSpeed} value 0, 1, 2.
     */
    private static final int[] SCROLL_MOVE_STEPS = new int[] {30, 20, 10};

    /**
     * The Mode of FrameTextView
     * One of {@link #INTERPOLATED_MODE}, {@link #INTERVAL_MODE}
     */
    private int mode;

    /**
     * If this FrameTextView view will start automatically,
     * If true, {@link #start()} will be revoked after construction
     */
    private boolean autoStart;

    /**
     * The delay time of revoking {@link #start()}
     */
    private int startDelay;

    /**
     * The from number for {@link #textProvider}, collaborate with {@link #to}.
     * This will take effect when {@link #textItems} is empty or null.
     */
    private ValueHolder from;

    /**
     * The to number for {@link #textProvider}, collaborate with {@link #from}.
     * This will take effect when {@link #textItems} is empty or null.
     */
    private ValueHolder to;

    /**
     * The text items will be displayed
     * example: ["Kobe", "Michael", "Obama"]
     */
    private CharSequence[] textItems;
    // The sequence to display {textItems}.
    private int itemsSequence;
    // The text provider of FrameTextView
    private TextProvider textProvider;
    // Transformed text colors in animation
    private int[] colorItems;
    // The sequence to transform the text color
    private int colorSequence;
    // How variables will be used in given text format
    private ColorProvider colorProvider;

    /**
     * The text format to be format by data get from {@link TextProvider}
     */
    private CharSequence textFormat;

    /**--- Animation Mode member value ---*/
    // Animation duration.
    private int duration;
    // Animation interpolator, decide the speed of text switching.
    private Interpolator interpolator;
    // Animation repeat mode
    private int repeatMode;
    // Animation repeat count
    private int repeatCount;
    // The valueAnimator used to render the FrameText via interpolation
    private ValueAnimator valueAnimator;
    private Animator.AnimatorListener animatorListener;

    /* Interval Mode member value */
    // The interval of setText()
    private int interval;
    // The execution time of setText()
    private int amount;
    // The Runnable to be executed by interval FrameTextView
    private Runnable runnable;
    // The timer task executed count
    private int count = 0;

    /**
     * If new character will scroll in.
     * If true, there will be a scrolling animation from old character to  new character.
     */
    private boolean scroll;

    /**
     * Control the scroll speed transformation of scrolling animation.
     *     All kinds of {@link Interpolator} can be assigned.
     * This only work when {@link #scroll} is true.
     */
    private Interpolator scrollInterpolator;

    /**
     * The scroll speed of new character
     * scrollSpeed value 0, 1, 2 correspond with {@link #SCROLL_MOVE_STEPS} value 30, 20, 10.
     */
    private int scrollSpeed;

    /**
     * The move step correspond with {@link #scrollSpeed} in {@link #SCROLL_MOVE_STEPS},
     *     initialized in {@link #init()}.
     */
    private int moveStep;

    /**
     * The {@link StaticLayoutData} list will be used in {@link #onDraw(Canvas)}.
     * This used only when {@link #scroll} is true
     *
     */
    StaticLayoutData leadingLayout;
    StaticLayoutData tailLayout;

    /**
     * The next text will be rendered to FrameTextView.
     * This value used only when {@link #scroll} is true.
     */
    private CharSequence nText;

    /**
     * The current text of FrameTextView.
     * This value used only when {@link #scroll} is true.
     */
    private CharSequence pText;

    /**
     * The bounds of a single char,
     *     this will be used to determine the size of StaticLayout in {@link #onDraw(Canvas)}
     */
    private Rect charBounds;

    /**
     * Is FrameTextView need to be init again before start.
     * This value will be estimated in {@link #start()}, you should not set the value manually.
     */
    private boolean isNeedInit = true;


    public FrameTextView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.frameTextViewStyle);
    }

    public FrameTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FrameTextView,
                defStyleAttr, R.style.DefaultFrameTextViewStyle);
        this.mode = a.getInteger(R.styleable.FrameTextView_mode,
                getResources().getInteger(R.integer.ftv_default_mode));
        this.autoStart = a.getBoolean(R.styleable.FrameTextView_autoStart,
                getResources().getBoolean(R.bool.ftv_default_autoStart));
        this.startDelay = a.getInt(R.styleable.FrameTextView_startDelay,
                getResources().getInteger(R.integer.ftv_default_startDelay));
        this.from = ValueHolder.parseValue(a.peekValue(R.styleable.FrameTextView_from));
        this.to = ValueHolder.parseValue(a.peekValue(R.styleable.FrameTextView_to));
        this.textItems = a.getTextArray(R.styleable.FrameTextView_textItems);
        this.itemsSequence = a.getInt(R.styleable.FrameTextView_itemsSequence,
                getResources().getInteger(R.integer.ftv_default_itemsSequence));
        final int textColorsId = a.getResourceId(R.styleable.FrameTextView_colorItems, 0);
        this.colorItems = textColorsId == 0 ? null : getResources().getIntArray(textColorsId);
        this.colorSequence = a.getInt(R.styleable.FrameTextView_colorSequence,
                getResources().getInteger(R.integer.ftv_default_colorsSequence));
        this.textFormat = a.getString(R.styleable.FrameTextView_format);
        this.scroll = a.getBoolean(R.styleable.FrameTextView_scroll,
                getResources().getBoolean(R.bool.ftv_default_scroll));

        if (this.mode != INTERVAL_MODE) {
            this.duration = a.getInt(R.styleable.FrameTextView_duration,
                    getResources().getInteger(R.integer.ftv_default_duration));
            this.interpolator = AnimationUtils.loadInterpolator(context, a.getResourceId(
                    R.styleable.FrameTextView_interpolator, android.R.anim.linear_interpolator));
            this.repeatMode = a.getInt(R.styleable.FrameTextView_repeatMode,
                    getResources().getInteger(R.integer.ftv_default_repeatMode));
            this.repeatCount = a.getInt(R.styleable.FrameTextView_repeatCount,
                    getResources().getInteger(R.integer.ftv_default_repeatCount));
        } else {
            this.interval = a.getInt(R.styleable.FrameTextView_interval,
                    getResources().getInteger(R.integer.ftv_default_interval));
            this.amount = a.getInt(R.styleable.FrameTextView_amount,
                    getResources().getInteger(R.integer.ftv_default_amount));
        }

        if (this.scroll) {
            this.scrollInterpolator = AnimationUtils.loadInterpolator(context, a.getResourceId(
                    R.styleable.FrameTextView_scrollInterpolator, android.R.anim.linear_interpolator));
            this.leadingLayout = new StaticLayoutData();
            this.tailLayout = new StaticLayoutData();
            this.charBounds = new Rect();
            this.scrollSpeed = a.getInt(R.styleable.FrameTextView_scrollSpeed,
                    getResources().getInteger(R.integer.ftv_default_scrollSpeed));
            this.pText = this.nText = "";
        }

        a.recycle();

        if (this.autoStart) {
            this.postDelayed(new Runnable() {
                @Override
                public void run() {
                    start();
                }
            }, this.startDelay);
        }

    }

    public void start() {
        if (this.isNeedInit && !init()) return;

        if (mode == INTERPOLATED_MODE) {
            this.valueAnimator.start();
        } else {
            this.post(runnable);
        }

    }


    public void stop() {
        if (mode == INTERPOLATED_MODE) {
            this.valueAnimator.cancel();
        } else {
            this.removeCallbacks(runnable);
        }
    }

    /**
     * This method will be revoked when FrameTextView be invalidated
     *     after {@link #setText(CharSequence)}
     * Generally, FrameTextView will invoke super.onDraw() method to finish render work,
     *     when {@link #scroll} is true, this onDraw() method will do some work
     *     let new character scroll in.
     */
    @Override
    public void onDraw(Canvas canvas) {
        Log.i(TAG, "onDraw");
        if (!this.scroll) {
            // Call super onDraw() method directly.
            super.onDraw(canvas);
            return;
        }

        if (this.nText.length() != this.pText.length()) {
            this.pText = this.nText;
            return;
        }

        int length = this.nText.length();
        if (length == 0) return;
        float widths[] = new float[length];
        getPaint().getTextWidths(nText.toString(), widths);
        float width = sumArray(widths);
        float moveX = width - widths[length - 1]; //TODO java.lang.ArrayIndexOutOfBoundsException: length=0; index=-1
        char n = this.nText.charAt(length - 1);
        char p = this.pText.charAt(length - 1);
        tailLayout.layout = new StaticLayout("" + n + p, getPaint(),
                (int) widths[nText.length() - 1], Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, true);
        if (n != p && n <= '9' && n >= '0' && p <= '9' && p >= '0') tailLayout.moveCount = 0;
        int moveCount = tailLayout.moveCount;
        float interpolation = 1;
        if (moveCount != -1) {
            interpolation = scrollInterpolator.getInterpolation(moveCount / ((float) moveStep));
            tailLayout.moveCount = moveCount > this.moveStep ? -1 : ++moveCount;
            postInvalidate();
        }

        leadingLayout.layout = new StaticLayout(nText.subSequence(0, nText.length() - 1), getPaint(),
                (int) moveX, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, true);
        leadingLayout.layout.draw(canvas);

        canvas.save();
        canvas.translate(moveX, charBounds.height() * (interpolation - 1));
        tailLayout.layout.draw(canvas);
        canvas.restore();

        this.pText = this.nText;
    }

    private static float sumArray(float[] a) {
        float sum = 0;
        for (float item:a) {
            sum += item;
        }
        return sum;
    }


    public boolean init() {
        if (!checkParameters()) return false;

        if (mode == INTERPOLATED_MODE) {
            initValueAnimator();
        } else {
            initRunnable();
        }

        if (textProvider == null || textProvider instanceof DefaultTextProvider)
            this.textProvider = new DefaultTextProvider();

        if (colorItems != null && colorItems.length > 1)
            this.colorProvider = new DefaultColorProvider();

        if (scroll) {
            getPaint().getTextBounds("0", 0, 1, charBounds);
            this.moveStep = SCROLL_MOVE_STEPS[this.scrollSpeed];
        }

        isNeedInit = false;
        return true;
    }

    private boolean checkParameters() {
        if (this.mode == INTERPOLATED_MODE) {
            if (this.duration <= 0) {
                Log.e(TAG, "Animation {duration} less than 0 second");

                return false;
            }
        } else if (this.mode == INTERVAL_MODE) {
            if (this.interval <= 0) {
                Log.e(TAG, "FrameTextView {interval} less than 0 second");
                return false;
            }
        } else {
            Log.e(TAG, "{mode} error, value must be INTERPOLATED_MODE or INTERVAL_MODE");
            return false;
        }

        if (textProvider == null && (textItems == null || textItems.length == 0)
                && from == null && to == null) {
            Log.e(TAG, "{textItems} is empty, {numFrom}, {numTo} and {frameTextProvider} is null," +
                    " nothing to render");
            return false;
        }

        return true;
    }

    private void initValueAnimator() {
        this.valueAnimator = ValueAnimator.ofFloat(0f, 1f);
        this.valueAnimator.setDuration(this.duration);
        this.valueAnimator.setInterpolator(this.interpolator);
        if (this.repeatCount > 0) {
            this.valueAnimator.setRepeatMode(this.repeatMode);
            this.valueAnimator.setRepeatCount(this.repeatCount);
        }
        if (this.animatorListener != null)
            this.valueAnimator.addListener(this.animatorListener);

        this.valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float interpolation = (float) animation.getAnimatedValue();

                // Get data from textProvider
                Object[] data = textProvider.getData(interpolation);
                // Format the next text will be rendered to FrameTextView
                nText = (getTextFormat() == null) ? arrayToString(data)
                        : String.format(getTextFormat().toString(), data);
                // Set new text and text color
                FrameTextView.this.setText(nText);
                if (getColorProvider() != null)
                    FrameTextView.this.setTextColor(getColorProvider().getColor(interpolation));
            }
        });
    }

    private void initRunnable() {
        this.runnable = new Runnable() {
            @Override
            public void run() {
                Object[] data = textProvider.getData(count);
                CharSequence text = (getTextFormat() == null) ?
                        arrayToString(data) : String.format(getTextFormat().toString(), data);
                FrameTextView.this.setText(text);
                if (getColorProvider() != null)
                    FrameTextView.this.setTextColor(getColorProvider().getColor(count));

                if (count++ >= amount && amount > 0) return;

                FrameTextView.this.postDelayed(runnable, interval);
            }
        };
    }

    public int getMode() {
        return this.mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public boolean getAutoStart() {
        return this.autoStart;
    }

    public void setAutoStart(final boolean autoStart) {
        this.autoStart = autoStart;
    }

    public ValueHolder getFrom() {
        return this.from;
    }

    public void setFrom(String from) {
        this.from.string = from;
        this.from.type = TypedValue.TYPE_STRING;
        this.isNeedInit = true;
    }

    public void setFrom(int from) {
        this.from.data = from;
        this.from.type = TypedValue.TYPE_INT_DEC;
        this.isNeedInit = true;
    }

    public ValueHolder getTo() {
        return this.to;
    }

    public void setTo(String to) {
        this.to.string = to;
        this.to.type= TypedValue.TYPE_STRING;
        this.isNeedInit = true;
    }

    public void setTo(int to) {
        this.to.data = to;
        this.to.type = TypedValue.TYPE_INT_DEC;
        this.isNeedInit = true;
    }

    public CharSequence[] getTextItems() {
        return textItems;
    }

    public void setTextItems(final CharSequence[] textItems) {
        this.textItems = textItems;
        this.isNeedInit = true;
    }

    public int getItemsSequence() {
        return itemsSequence;
    }

    public void setItemsSequence(final int itemsSequence) {
        this.itemsSequence = itemsSequence;
        this.isNeedInit = true;
    }

    public int[] getColorItems() {
        return this.colorItems;
    }

    public void setColorItems(int[] textColors) {
        this.colorItems = textColors;
    }

    public int getColorSequence() {
        return colorSequence;
    }

    public void setColorSequence(final int colorSequence) {
        this.colorSequence = colorSequence;
        this.isNeedInit = true;
    }

    public ColorProvider getColorProvider() {
        return this.colorProvider;
    }

    public void setColorProvider(ColorProvider colorProvider) {
        this.colorProvider = colorProvider;
    }

    public CharSequence getTextFormat() {
        return textFormat;
    }

    public void setTextFormat(final CharSequence textFormat) {
        this.textFormat = textFormat;
        this.isNeedInit = true;
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(final int duration) {
        this.duration = duration;
        this.isNeedInit = true;
    }

    public Interpolator getInterpolator() {
        return interpolator;
    }

    public void setInterpolator(final Interpolator interpolator) {
        this.interpolator = interpolator;
        this.isNeedInit = true;
    }

    public Animator.AnimatorListener getAnimatorListener() {
        return animatorListener;
    }

    public void setAnimatorListener(final Animator.AnimatorListener animatorListener) {
        this.animatorListener = animatorListener;
        this.isNeedInit = true;
    }

    public TextProvider getFrameTextProvider() {
        return textProvider;
    }

    public void setFrameTextProvider(final TextProvider frameTextProvider) {
        this.textProvider = frameTextProvider;
        this.isNeedInit = true;
    }

    public int getInterval() {
        return this.interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public boolean isScroll() {
        return scroll;
    }

    public void setScroll(boolean scroll) {
        this.scroll = scroll;
    }

    public Interpolator getScrollInterpolator() {
        return scrollInterpolator;
    }

    public void setScrollInterpolator(Interpolator scrollInterpolator) {
        this.scrollInterpolator = scrollInterpolator;
    }

    private String arrayToString(Object[] oa) {
        if (oa == null)
            return "";

        StringBuilder builder = new StringBuilder();
        for(Object obj:oa) {
            builder.append(String.valueOf(obj));
        }

        return builder.toString();
    }

    private static class ValueHolder {
        public int type;

        public CharSequence string;

        public int data;

        static ValueHolder parseValue(TypedValue value) {
            ValueHolder v = new ValueHolder();

            if (value == null) return null;

            v.type = value.type;
            if (v.type == TypedValue.TYPE_STRING)
                v.string = value.string;
            else
                v.data = value.data;

            return v;
        }
    }

    private static class StaticLayoutData {

        public StaticLayout layout;

        public int moveCount = 0;
    }

    /**
     *
     */
    public interface TextProvider {
        Object[] getData(float interpolation);
        Object[] getData(int count);
    }

    private class DefaultTextProvider implements TextProvider {


        CharSequence textCs;
        int startIndex, endIndex;
        int fromNum, toNum;
        int diff;

        public DefaultTextProvider() {
            setDifArray();
        }

        @Override
        public Object[] getData(float interpolation) {
            if (interpolation < 0){
                interpolation = 0;
            } else if (interpolation > 1) {
                interpolation = 1;
            }

            if (getTextItems() != null && getTextItems().length > 0) {
                float value = interpolation;
                switch (getItemsSequence()) {
                    case DESC:
                        value = 1 - interpolation;
                        break;
                    case RANDOM:
                        value = (float) Math.random();
                        break;
                }
                return new Object[]{getTextItems()[(int) ((getTextItems().length - 1) * value)]};
            }

            if (textCs != null) {
                return new Object[]{textCs.subSequence(0, startIndex + (int) (diff * interpolation))};
            } else if (diff != 0) {
                return new Object[]{(int) (fromNum + diff * interpolation)};
            }

            return null;
        }

        @Override
        public Object[] getData(int count) {
            if (getTextItems() != null && getTextItems().length > 0) {
                int index = count % getTextItems().length;
                switch (getItemsSequence()) {
                    case DESC:
                        index = getTextItems().length - index - 1;
                        break;
                    case RANDOM:
                        index = (int) (Math.random() * (getTextItems().length - 1));
                        break;
                }
                return new Object[]{getTextItems()[index]};
            }

            int m = diff + 1;
            if (textCs != null) {
                return new Object[]{textCs.subSequence(0, startIndex + diff == 0 ? 0:count % m)};
            } else if (diff != 0) {
                return new Object[]{fromNum + count % m};
            }

            return null;
        }

        private void setDifArray() {
            if (getFrom() == null && getTo() == null) return;

            if ((getFrom() != null && getFrom().type == TypedValue.TYPE_STRING)
                    || (getTo() != null && getTo().type == TypedValue.TYPE_STRING)) {
                CharSequence fromCs = getFrom() == null && getFrom().string == null
                        ? "" : getFrom().string;
                CharSequence toCs = getTo() == null && getTo().string == null ? "" : getTo().string;
                textCs = fromCs.length() > toCs.length() ? fromCs : toCs;
                Log.i(TAG, "textCs:" + textCs);
                startIndex = fromCs.length() - 1;
                endIndex = toCs.length() - 1;
                diff = endIndex - startIndex;
            } else {
                fromNum = getFrom() == null ? 0 : getFrom().data;
                toNum = getTo() == null ? 0 : getTo().data;
                diff = toNum - fromNum;
            }

        }

    }

    public interface ColorProvider {
        int getColor(int count);
        int getColor(float interpolation);
    }

    public class DefaultColorProvider implements ColorProvider {

        int itemIndex = 0;
        int[][] rgbDiffArray;
        float[][] rgbIntervalArray;
        float[] currentDelta;
        int length = 0;
        float section;

        public DefaultColorProvider() {
            length = colorItems.length;
            section = 1.0f / (length - 1);
            rgbDiffArray = new int[length][4];
            rgbIntervalArray = new float[length][4];
            currentDelta = new float[]{0f, 0f, 0f, 0f};
            for (int i = 0; i < length - 1; i++) {
                int c1 = colorItems[i];
                int c2 = colorItems[i + 1];
                rgbDiffArray[i] = new int[]{Color.alpha(c2) - Color.alpha(c1),
                        Color.red(c2) - Color.red(c1),
                        Color.green(c2) - Color.green(c1),
                        Color.blue(c2) - Color.blue(c1)};
                rgbIntervalArray[i] = new float[]{ rgbDiffArray[i][0] / 16.0f,
                        rgbDiffArray[i][1] / 16.0f,
                        rgbDiffArray[i][2] / 16.0f,
                        rgbDiffArray[i][3] / 16.0f };
            }
        }

        @Override
        public int getColor(int count) {
            return getColor(-1);
        }

        @Override
        public int getColor(float interpolation) {
            switch (colorSequence) {
                case ASC:
                    itemIndex = (itemIndex + 1) % length;
                    break;
                case DESC:
                    itemIndex = (itemIndex - 1) % length;
                    break;
                case RANDOM:
                    itemIndex = (int) (Math.random() * length);
                    break;
                case GRADUAL:
                    if (interpolation != -1) {
                        if (interpolation >= 1)
                            return colorItems[length - 1];

                        itemIndex = (int) (interpolation / section);
                        float percent = (interpolation % section) / section;
                        int item = colorItems[itemIndex];
                        return Color.argb(
                                (int) (Color.alpha(item) + rgbDiffArray[itemIndex][0] * percent),
                                (int) (Color.red(item) + rgbDiffArray[itemIndex][1] * percent),
                                (int) (Color.green(item) + rgbDiffArray[itemIndex][2] * percent),
                                (int) (Color.blue(item) + rgbDiffArray[itemIndex][3] * percent));
                    }else {
                        if (itemIndex >= length - 1)
                            itemIndex = 0;

                        for (int i = 0; i <4; i++)
                            currentDelta[i] = currentDelta[i] + rgbIntervalArray[itemIndex][i];

                        if (Math.abs(currentDelta[0]) >= Math.abs(rgbDiffArray[itemIndex][0])) {
                            itemIndex++;
                            currentDelta = new float[]{0f, 0f, 0f, 0f};
                        }
                        int item = colorItems[itemIndex];
                        return Color.rgb((int) (Color.red(item) + currentDelta[0]),
                                (int) (Color.green(item) + currentDelta[1]),
                                (int) (Color.blue(item) + currentDelta[2]));
                    }
            }
            return colorItems[itemIndex];
        }

    }

}

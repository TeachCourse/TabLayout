/*
 * Copyright (C) 2013 Andreas Stuetz <andreas.stuetz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.teahcourse.tablayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;


public class TabLayout extends HorizontalScrollView {
    private static final String TAG = "TabLayout";

    public interface IconTabProvider {
        int getPageIconResId(int position);
    }

    // @formatter:off
    private static final int[] ATTRS = new int[]{android.R.attr.textSize,
            android.R.attr.textColor};
    // @formatter:on

    private LinearLayout.LayoutParams defaultTabLayoutParams;
    private LinearLayout.LayoutParams expandedTabLayoutParams;

    private final PageListener pageListener = new PageListener();
    public OnPageChangeListener delegatePageListener;

    private LinearLayout tabsContainer;
    private ViewPager pager;

    private int tabCount;

    private int currentPosition = 0;
    private int selectedPosition = 0;
    private float currentPositionOffset = 0f;

    private Paint rectPaint;
    private Paint dividerPaint;

    private int indicatorColor = 0xFF666666;
    private int underlineColor = 0x1A000000;
    private int dividerColor = 0x1A000000;

    private boolean shouldExpand = false;
    private boolean textAllCaps = true;

    private int scrollOffset = 52;
    private int indicatorHeight = 4;
    private int underlineHeight = 2;
    private int indicatorWidth = -1;
    private int dividerPadding = 12;
    private int tabPadding = 24;
    private int dividerWidth = 1;

    private int textSize = 12;
    private int textColor = 0xFF666666;
    private int textColorSelected = 0xFF666666;
    private int tabBackgroundResId = R.drawable.background_tab;
    private int lastScrollX = 0;

    private Typeface tabTypeface = null;
    private int tabTypefaceStyle = Typeface.NORMAL;

    private Locale locale;

    private Shader mShader;

    public TabLayout(Context context) {
        this(context, null);
    }

    public TabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setFillViewport(true);
        setWillNotDraw(false);

        tabsContainer = new LinearLayout(context);
        tabsContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabsContainer.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(tabsContainer);
        init(context, attrs);


        rectPaint = new Paint();
        rectPaint.setAntiAlias(true);
        rectPaint.setStyle(Style.FILL);

        dividerPaint = new Paint();
        dividerPaint.setAntiAlias(true);
        dividerPaint.setStrokeWidth(dividerWidth);

        //设置Tab item布局样式
        defaultTabLayoutParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        expandedTabLayoutParams = new LinearLayout.LayoutParams(0,
                LayoutParams.MATCH_PARENT, 1.0f);

        if (locale == null) {
            locale = getResources().getConfiguration().locale;
        }
    }

    private void init(Context context, AttributeSet attrs) {
        //解析默认属性值
        DisplayMetrics dm = getResources().getDisplayMetrics();

        scrollOffset = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, scrollOffset, dm);
        indicatorHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, indicatorHeight, dm);
        underlineHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, underlineHeight, dm);
        dividerPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dividerPadding, dm);
        tabPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, tabPadding, dm);
        dividerWidth = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dividerWidth, dm);
        textSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, textSize, dm);
        initSystemAttr(context, attrs);

        initCustomAttr(context, attrs);

    }

    private void initSystemAttr(Context context, AttributeSet attrs) {
        // get system attrs (android:textSize and android:textColor)

        TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
        //获取系统属性android:textColor/android:textSize文件属性值
        textSize = a.getDimensionPixelSize(0, textSize);
        textColor = a.getColor(1, textColor);

        a.recycle();
    }

    private void initCustomAttr(Context context, AttributeSet attrs) {
        TypedArray a;// get custom attrs

        a = context.obtainStyledAttributes(attrs,
                R.styleable.TabLayout);
        //获取自定义属性配置文件字段
        indicatorColor = a.getColor(
                R.styleable.TabLayout_tab_indicator_color,
                indicatorColor);
        underlineColor = a.getColor(
                R.styleable.TabLayout_tab_underline_color,
                underlineColor);
        dividerColor = a
                .getColor(R.styleable.TabLayout_tab_divider_color,
                        dividerColor);
        indicatorHeight = a.getDimensionPixelSize(
                R.styleable.TabLayout_tab_indicator_height,
                indicatorHeight);
        indicatorWidth = a.getDimensionPixelSize(
                R.styleable.TabLayout_tab_indicator_width,
                indicatorWidth);
        underlineHeight = a.getDimensionPixelSize(
                R.styleable.TabLayout_tab_underline_height,
                underlineHeight);
        dividerPadding = a.getDimensionPixelSize(
                R.styleable.TabLayout_tab_divider_padding,
                dividerPadding);
        tabPadding = a.getDimensionPixelSize(
                R.styleable.TabLayout_tab_padding_left_to_right,
                tabPadding);
        tabBackgroundResId = a.getResourceId(
                R.styleable.TabLayout_tab_background,
                tabBackgroundResId);
        shouldExpand = a
                .getBoolean(R.styleable.TabLayout_tab_should_expand,
                        shouldExpand);
        scrollOffset = a
                .getDimensionPixelSize(
                        R.styleable.TabLayout_tab_scroll_offset,
                        scrollOffset);
        textAllCaps = a.getBoolean(
                R.styleable.TabLayout_tab_text_all_caps, textAllCaps);
        textColorSelected = a.getColor(R.styleable.TabLayout_tab_text_color_selected,
                textColorSelected);
        textColor=a.getColor(R.styleable.TabLayout_tab_text_color,
                textColor);
        textSize=a.getDimensionPixelSize(R.styleable.TabLayout_tab_text_color,
                textSize);

        a.recycle();
    }

    public void setViewPager(ViewPager pager) {
        this.pager = pager;

        if (pager.getAdapter() == null) {
            throw new IllegalStateException(
                    "ViewPager does not have adapter instance.");
        }

        pager.setOnPageChangeListener(pageListener);

        notifyDataSetChanged();
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.delegatePageListener = listener;
    }


    public void notifyDataSetChanged() {

        tabsContainer.removeAllViews();

        tabCount = pager.getAdapter().getCount();

        for (int i = 0; i < tabCount; i++) {

            if (pager.getAdapter() instanceof IconTabProvider) {
                addIconTab(i, ((IconTabProvider) pager.getAdapter()).getPageIconResId(i));
            } else {
                addTextTab(i, pager.getAdapter().getPageTitle(i).toString());
            }

        }

        updateTabStyles();

        getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {
                        getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        currentPosition = pager.getCurrentItem();
                        scrollToChild(currentPosition, 0);
                    }
                });

    }

    private void addTextTab(final int position, String title) {
        TextView tab = new TextView(getContext());
        tab.setText(title);
        tab.setGravity(Gravity.CENTER);
        tab.setSingleLine();
        addTab(position, tab);
    }

    private void addIconTab(final int position, int resId) {
        ImageButton tab = new ImageButton(getContext());
        tab.setImageResource(resId);

        addTab(position, tab);
    }

    private void addTab(final int position, View tab) {
        tab.setFocusable(true);
        tab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pager.setCurrentItem(position);
            }
        });

        tab.setPadding(tabPadding, 0, tabPadding, 0);
        tabsContainer
                .addView(tab, position, shouldExpand ? expandedTabLayoutParams
                        : defaultTabLayoutParams);
    }

    private void updateTabStyles() {

        for (int i = 0; i < tabCount; i++) {

            View v = tabsContainer.getChildAt(i);

            v.setBackgroundResource(tabBackgroundResId);

            if (v instanceof TextView) {

                TextView tab = (TextView) v;
                tab.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                tab.setTypeface(tabTypeface, tabTypefaceStyle);
                tab.setTextColor(textColor);

                // setAllCaps() is only available from API 14, so the upper case
                // is made manually if we are on a
                // pre-ICS-build
                if (textAllCaps) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        tab.setAllCaps(true);
                    } else {
                        tab.setText(tab.getText().toString()
                                .toUpperCase(locale));
                    }
                }
                if (i == selectedPosition) {
                    tab.setTextColor(textColorSelected);
                }
            }
        }

    }

    private void scrollToChild(int position, int offset) {

        if (tabCount == 0) {
            return;
        }

        int newScrollX = tabsContainer.getChildAt(position).getLeft() + offset;

        if (position > 0 || offset > 0) {
            newScrollX -= scrollOffset;
        }

        if (newScrollX != lastScrollX) {
            lastScrollX = newScrollX;
            scrollTo(newScrollX, 0);
        }

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isInEditMode() || tabCount == 0) {
            return;
        }
        final int height = getHeight();

        drawUnderline(canvas, height);

        drawIndicator(canvas, height);

        drawDivider(canvas, height);
    }

    /**
     * 绘制指示器之间的分隔线，自定义颜色、内边距
     *
     * @param canvas
     * @param height
     */
    private void drawDivider(Canvas canvas, int height) {
        // draw divider

        dividerPaint.setColor(dividerColor);
        for (int i = 0; i < tabCount - 1; i++) {
            View tab = tabsContainer.getChildAt(i);
            canvas.drawLine(tab.getRight(), dividerPadding, tab.getRight(),
                    height - dividerPadding, dividerPaint);
        }
    }

    /**
     * 绘制导航底部下划线，自定义颜色、高度
     *
     * @param canvas
     * @param height
     */

    private void drawUnderline(Canvas canvas, int height) {
        // draw underline
        rectPaint.setColor(underlineColor);
        canvas.drawRect(0, height - underlineHeight, tabsContainer.getWidth(),
                height, rectPaint);
    }

    /**
     * 绘制选中文字底部下划线，自定义颜色
     *
     * @param canvas
     * @param height
     */
    private void drawIndicator(Canvas canvas, int height) {
        // draw indicator line
        rectPaint.setColor(indicatorColor);
        dividerPaint.setShader(mShader);

        // default: line below current tab
        View currentTab = tabsContainer.getChildAt(currentPosition);
        float lineLeft = currentTab.getLeft();
        float lineRight = currentTab.getRight();

        // if there is an offset, start interpolating left and right coordinates
        // between current and next tab

        int width = currentTab.getWidth();
        if (currentPositionOffset > 0f && currentPosition < tabCount - 1) {

            View nextTab = tabsContainer.getChildAt(currentPosition + 1);
            final float nextTabLeft = nextTab.getLeft();
            final float nextTabRight = nextTab.getRight();

            lineLeft = (currentPositionOffset * nextTabLeft + (1f - currentPositionOffset) * lineLeft);
            lineRight = (currentPositionOffset * nextTabRight + (1f - currentPositionOffset) * lineRight);

            width = nextTab.getWidth();
        }
        //绘制选中tab指示器底部下划线，默认为当前Tab宽度的一半
        if (indicatorWidth == -1)
            canvas.drawRect(lineLeft + width / 4, height - indicatorHeight, lineRight - width / 4, height, rectPaint);
        else if (indicatorWidth < width && indicatorWidth > 0) {
            int offset = width - indicatorWidth;
            canvas.drawRect(lineLeft + offset / 2, height - indicatorHeight, lineRight - offset / 2, height, rectPaint);
        }
    }

    private class PageListener implements OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset,
                                   int positionOffsetPixels) {
            currentPosition = position;
            currentPositionOffset = positionOffset;

            scrollToChild(position, (int) (positionOffset * tabsContainer
                    .getChildAt(position).getWidth()));

            invalidate();

            if (delegatePageListener != null) {
                delegatePageListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            Log.e(TAG, positionOffset + "");
            int colors[] = new int[]{Color.GREEN, Color.TRANSPARENT};

            //设置切换过程中的渐变色
            mShader = new LinearGradient(0, 0, positionOffset, 90, colors, null, Shader.TileMode.REPEAT);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

            if (state == ViewPager.SCROLL_STATE_IDLE) {
                scrollToChild(pager.getCurrentItem(), 0);
            }

            if (delegatePageListener != null) {
                delegatePageListener.onPageScrollStateChanged(state);
            }

            Log.e(TAG, "currentState: " + state);
        }

        @Override
        public void onPageSelected(int position) {
            selectedPosition = position;
            updateTabStyles();
            if (delegatePageListener != null) {
                delegatePageListener.onPageSelected(position);
            }
        }

    }


    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        currentPosition = savedState.currentPosition;
        requestLayout();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.currentPosition = currentPosition;
        return savedState;
    }

    static class SavedState extends BaseSavedState {
        int currentPosition;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            currentPosition = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentPosition);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public static class Builder {
        private Context context;
        private int indicatorColor = 0xFF666666;
        private int underlineColor = 0x1A000000;
        private int dividerColor = 0x1A000000;

        private boolean shouldExpand = false;
        private boolean textAllCaps = true;

        private int scrollOffset = 52;
        private int indicatorHeight = 4;
        private int indicatorWidth = -1;
        private int underlineHeight = 2;
        private int dividerPadding = 12;
        private int tabPadding = 24;
        private int dividerWidth = 1;

        private int textSize = 12;
        private int textColor = 0xFF666666;
        private int textColorSelected = 0xFF666666;
        private int tabBackgroundResId = R.drawable.background_tab;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setIndicatorColor(int indicatorColor) {
            this.indicatorColor = indicatorColor;
            return this;
        }

        public Builder setUnderlineColor(int underlineColor) {
            this.underlineColor = underlineColor;
            return this;
        }

        public Builder setDividerColor(int dividerColor) {
            this.dividerColor = dividerColor;
            return this;
        }

        public Builder setShouldExpand(boolean shouldExpand) {
            this.shouldExpand = shouldExpand;
            return this;
        }

        public Builder setTextAllCaps(boolean textAllCaps) {
            this.textAllCaps = textAllCaps;
            return this;
        }


        public Builder setScrollOffset(int scrollOffset) {
            this.scrollOffset = scrollOffset;
            return this;
        }

        public Builder setIndicatorHeight(int indicatorHeight) {
            this.indicatorHeight = indicatorHeight;
            return this;
        }

        public Builder setUnderlineHeight(int underlineHeight) {
            this.underlineHeight = underlineHeight;
            return this;
        }

        public Builder setDividerPadding(int dividerPadding) {
            this.dividerPadding = dividerPadding;
            return this;
        }

        public Builder setTabPadding(int tabPadding) {
            this.tabPadding = tabPadding;
            return this;
        }

        public Builder setDividerWidth(int dividerWidth) {
            this.dividerWidth = dividerWidth;
            return this;
        }

        public Builder setTextSize(int textSize) {
            this.textSize = textSize;
            return this;
        }

        public Builder setTextColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder setTextColorSelected(int textColorSelected) {
            this.textColorSelected = textColorSelected;
            return this;
        }

        public Builder setTabBackgroundResId(int tabBackgroundResId) {
            this.tabBackgroundResId = tabBackgroundResId;
            return this;
        }

        public Builder setIndicatorWidth(int width){
            this.indicatorWidth=width;
            return this;
        }

        public TabLayout create() {
            TabLayout layout = new TabLayout(context);
            layout.indicatorColor = indicatorColor;
            layout.underlineColor = underlineColor;
            layout.dividerColor = dividerColor;

            layout.shouldExpand = shouldExpand;
            layout.textAllCaps = textAllCaps;

            layout.scrollOffset = scrollOffset;
            layout.indicatorHeight = indicatorHeight;
            layout.indicatorWidth=indicatorWidth;
            layout.underlineHeight = underlineHeight;
            layout.dividerPadding = dividerPadding;
            layout.tabPadding = tabPadding;
            layout.dividerWidth = dividerWidth;

            layout.textSize = textSize;
            layout.textColor = textColor;
            layout.textColorSelected = textColorSelected;
            layout.tabBackgroundResId = tabBackgroundResId;
            return layout;
        }
    }
}

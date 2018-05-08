## 摘要
一个快速导航控件，扩展SlidingTabStrip已有功能并优化性能

在xml布局文件的用法

```
<cn.teahcourse.tablayout.TabLayout
    android:id="@+id/layout_tab"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:tab_indicator_color="#3cb158"
    app:tab_text_color_selected="#3cb158"
    app:tab_divider_color="@android:color/transparent"
    app:tab_indicator_height="4px"
    app:tab_underline_height="4px"
    app:tab_indicator_width="98px"
    app:tab_text_size="28px"
    app:tab_should_expand="true"/>
```

在java代码文件的用法

```
TabLayout tabLayout=new TabLayout.Builder(getContext())
        .setDividerColor(Color.TRANSPARENT)
        .setIndicatorColor(Color.parseColor("#3cb158"))
        .setTextColorSelected(Color.parseColor("#3cb158"))
        .setIndicatorHeight(4)
        .setIndicatorWidth(88)
        .setTextSize(28)
        .setUnderlineHeight(4)
        .setShouldExpand(true)
        .create();
```


运行效果图：

<img src="img/20180508144623.jpg" width="480" />

package com.github.rove24.wellbeing;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CardLayoutHelper {

    private static final String TAG = "WellbeingM3E";

    public enum CardPosition {
        TOP, MIDDLE, BOTTOM, SINGLE
    }

    public static Class<?> findTargetClass(ClassLoader cl, String className) {
        if (className == null || cl == null) return null;
        String[] candidates;
        if (className.contains(".")) {
            candidates = new String[]{className};
        } else {
            candidates = new String[]{className, "defpackage." + className};
        }

        for (String name : candidates) {
            try {
                return Class.forName(name, false, cl);
            } catch (Throwable ignored) {}
            try {
                return cl.loadClass(name);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
            current = current.getSuperclass();
        }
        return null;
    }

    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Method m = current.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * Apply horizontal margins (16dp) to MainSwitchBar.
     * Only modify switchBar margin itself, DO NOT modify inner frame margin.
     */
    public static void applySwitchBarMargin(View view) {
        if (view == null) return;
        try {
            Context context = view.getContext();
            int margin = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    16.0f,
                    context.getResources().getDisplayMetrics()
            );

            // Clear parent item view's horizontal padding so switch bar aligns precisely with cards
            ViewParent parent = view.getParent();
            if (parent instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) parent;
                if (vg.getPaddingLeft() != 0 || vg.getPaddingRight() != 0 || vg.getPaddingStart() != 0 || vg.getPaddingEnd() != 0) {
                    vg.setPaddingRelative(0, vg.getPaddingTop(), 0, vg.getPaddingBottom());
                }
            }

            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                if (mlp.leftMargin == margin && mlp.rightMargin == margin &&
                        mlp.getMarginStart() == margin && mlp.getMarginEnd() == margin) {
                    return;
                }
                mlp.setMarginStart(margin);
                mlp.setMarginEnd(margin);
                mlp.leftMargin = margin;
                mlp.rightMargin = margin;
                view.setLayoutParams(mlp);
                view.requestLayout();
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Recursively scan activity view tree to detect and format Walking Detection views.
     */
    /**
     * Format Walking Detection screen (Switch Bar + 3 Permission items as continuous cards).
     */
    public static void formatWalkingDetectionActivity(Activity activity) {
        if (activity == null) return;
        try {
            View decorView = activity.getWindow().getDecorView();
            formatWalkingDetectionViews(decorView);
        } catch (Throwable ignored) {}
    }

    public static void formatWalkingDetectionViews(View root) {
        if (root == null) return;
        try {
            // 1. Find and style MainSwitchBar
            List<View> switchBars = new ArrayList<>();
            findViewsByClassName(root, "MainSwitchBar", switchBars);
            for (View sb : switchBars) {
                applySwitchBarMargin(sb);
            }

            // 2. Find permission items by inspecting view hierarchy
            List<View> permissionItems = new ArrayList<>();
            findPermissionItems(root, permissionItems);

            if (!permissionItems.isEmpty()) {
                if (permissionItems.size() == 1) {
                    applyDarQCardLayout(permissionItems.get(0), CardPosition.SINGLE);
                } else {
                    for (int i = 0; i < permissionItems.size(); i++) {
                        CardPosition pos;
                        if (i == 0) {
                            pos = CardPosition.TOP;
                        } else if (i == permissionItems.size() - 1) {
                            pos = CardPosition.BOTTOM;
                        } else {
                            pos = CardPosition.MIDDLE;
                        }
                        applyDarQCardLayout(permissionItems.get(i), pos);
                    }
                }

                // Remove redundant bottom divider line following the permission card cluster
                View last = permissionItems.get(permissionItems.size() - 1);
                if (last.getParent() instanceof ViewGroup) {
                    ViewGroup parent = (ViewGroup) last.getParent();
                    int idx = parent.indexOfChild(last);
                    if (idx >= 0 && idx + 1 < parent.getChildCount()) {
                        View next = parent.getChildAt(idx + 1);
                        if (!(next instanceof TextView)) {
                            next.setVisibility(View.GONE);
                        }
                    }
                }
            }

            // 3. Format "Send feedback / 发送反馈" into an M3 tonal pill button
            Context ctx = root.getContext();
            String pkg = ctx.getPackageName();
            int feedbackId = ctx.getResources().getIdentifier("send_feedback", "id", pkg);
            View fbView = feedbackId != 0 ? root.findViewById(feedbackId) : null;
            if (fbView == null) {
                fbView = findViewByResourceEntryName(root, "send_feedback");
            }
            if (fbView instanceof TextView) {
                formatPillButton((TextView) fbView);
            }
        } catch (Throwable ignored) {}
    }

    private static void findViewsByClassName(View root, String simpleName, List<View> out) {
        if (root == null) return;
        if (root.getClass().getName().contains(simpleName)) {
            out.add(root);
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                findViewsByClassName(vg.getChildAt(i), simpleName, out);
            }
        }
    }

    private static void findPermissionItems(View root, List<View> out) {
        if (root == null) return;
        if (isPermissionItem(root)) {
            out.add(root);
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                findPermissionItems(vg.getChildAt(i), out);
            }
        }
    }

    private static boolean isPermissionItem(View view) {
        if (!(view instanceof ViewGroup)) return false;
        ViewGroup vg = (ViewGroup) view;

        // Check by resource entry name of the container
        int containerId = view.getId();
        if (containerId != 0 && containerId != View.NO_ID) {
            try {
                String entry = view.getResources().getResourceEntryName(containerId);
                if ("physical_activity_permission".equals(entry) ||
                    "post_notifications_permission".equals(entry) ||
                    "location_permission".equals(entry)) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // Check by children (permission_icon / permission_title)
        boolean hasIcon = false;
        boolean hasTitle = false;
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            int id = child.getId();
            if (id != 0 && id != View.NO_ID) {
                try {
                    String entry = child.getResources().getResourceEntryName(id);
                    if ("permission_icon".equals(entry)) hasIcon = true;
                    if ("permission_title".equals(entry)) hasTitle = true;
                } catch (Throwable ignored) {}
            }
        }
        return hasIcon && hasTitle;
    }

    /**
     * Recursively scan activity view tree to detect and format Mindful Nudge switch bar.
     */
    public static void checkAndApplyMindfulNudge(View root) {
        if (root == null) return;
        try {
            Context ctx = root.getContext();
            String pkg = ctx.getPackageName();
            int switchId = ctx.getResources().getIdentifier("mindful_nudge_switch", "id", pkg);
            if (switchId != 0) {
                View switchBar = root.findViewById(switchId);
                if (switchBar != null) {
                    applySwitchBarMargin(switchBar);
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Convert CompositeToggle internal CheckBox -> Switch and apply DarQ card layout.
     */
    public static void handleCompositeToggleSetup(View view, ClassLoader cl) {
        if (view == null) return;
        try {
            Class<?> ctClass = view.getClass();
            Field eField = findField(ctClass, "e");
            if (eField != null) {
                Object current = eField.get(view);
                if (current instanceof CheckBox) {
                    CheckBox cb = (CheckBox) current;
                    cb.setVisibility(View.GONE);

                    ViewGroup vg = (ViewGroup) view;
                    Switch sw = null;
                    for (int i = 0; i < vg.getChildCount(); i++) {
                        View child = vg.getChildAt(i);
                        if (child instanceof Switch) {
                            sw = (Switch) child;
                            break;
                        }
                    }

                    if (sw != null) {
                        sw.setVisibility(View.VISIBLE);
                        sw.setChecked(cb.isChecked());
                        sw.setFocusable(false);
                        sw.setClickable(false);
                        eField.set(view, sw);

                        Class<?> ibvClass = findTargetClass(cl, "ibv");
                        if (ibvClass != null) {
                            try {
                                Field aField = ibvClass.getDeclaredField("a");
                                aField.setAccessible(true);
                                Object switchVal = aField.get(null);
                                if (switchVal != null) {
                                    Field lField = findField(ctClass, "l");
                                    if (lField != null) {
                                        lField.set(view, switchVal);
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    }

                    int dividerId = vg.getResources().getIdentifier("click_target_divider", "id", vg.getContext().getPackageName());
                    if (dividerId != 0) {
                        View div = vg.findViewById(dividerId);
                        if (div != null) div.setVisibility(View.GONE);
                    }
                }
            }

            // Apply DarQ continuous card styling
            setupAppItemCard(view);
        } catch (Throwable ignored) {}
    }

    /**
     * Dynamically determine the CardPosition (TOP, MIDDLE, BOTTOM, SINGLE) of an item
     * within a RecyclerView or ViewGroup, matching Google Contacts continuous card style.
     */
    public static CardPosition determineCardPosition(View view) {
        if (view == null) return CardPosition.SINGLE;
        try {
            ViewParent vp = view.getParent();
            ViewGroup parent = (vp instanceof ViewGroup) ? (ViewGroup) vp : null;

            // 1. Adapter & ViewHolder position check
            int pos = -1;
            int myType = -1;
            Object adapter = null;

            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null) {
                Object vh = null;
                Field fHolder = findField(lp.getClass(), "c");
                if (fHolder == null) {
                    fHolder = findField(lp.getClass(), "mViewHolder");
                }
                if (fHolder != null) {
                    vh = fHolder.get(lp);
                }

                if (vh != null) {
                    // Try getAdapterPosition / method 'a' in ViewHolder (calls recyclerView.b(this))
                    try {
                        Method mA = findMethod(vh.getClass(), "a");
                        if (mA != null) pos = (int) mA.invoke(vh);
                    } catch (Throwable ignored) {}
                    if (pos == -1) {
                        try {
                            Method m = findMethod(vh.getClass(), "getBindingAdapterPosition");
                            if (m != null) pos = (int) m.invoke(vh);
                        } catch (Throwable ignored) {}
                    }
                    if (pos == -1) {
                        try {
                            Method m = findMethod(vh.getClass(), "getAdapterPosition");
                            if (m != null) pos = (int) m.invoke(vh);
                        } catch (Throwable ignored) {}
                    }
                    // Try getLayoutPosition (method 'b') or field 'c' (mPosition)
                    if (pos == -1) {
                        try {
                            Method mPos = findMethod(vh.getClass(), "b");
                            if (mPos != null) pos = (int) mPos.invoke(vh);
                        } catch (Throwable ignored) {}
                    }
                    if (pos == -1) {
                        Field fPos = findField(vh.getClass(), "c");
                        if (fPos != null) pos = fPos.getInt(vh);
                    }

                    Field fType = findField(vh.getClass(), "f");
                    if (fType != null) {
                        myType = fType.getInt(vh);
                    } else {
                        Method mType = findMethod(vh.getClass(), "getItemViewType");
                        if (mType != null) myType = (int) mType.invoke(vh);
                    }

                    Field fAdapter = findField(vh.getClass(), "r");
                    if (fAdapter != null) adapter = fAdapter.get(vh);
                    if (adapter == null) {
                        Field fRv = findField(vh.getClass(), "q");
                        if (fRv != null) {
                            Object rv = fRv.get(vh);
                            if (rv != null) {
                                Field fRvAdapter = findField(rv.getClass(), "m");
                                if (fRvAdapter != null) adapter = fRvAdapter.get(rv);
                            }
                        }
                    }
                }
            }

            if (adapter == null && parent != null) {
                Field fRvAdapter = findField(parent.getClass(), "m");
                if (fRvAdapter != null) adapter = fRvAdapter.get(parent);
                if (adapter == null) {
                    Field fA = findField(parent.getClass(), "mAdapter");
                    if (fA != null) adapter = fA.get(parent);
                }
            }

            if (pos >= 0 && adapter != null) {
                int itemCount = -1;
                try {
                    Method mCount = findMethod(adapter.getClass(), "a");
                    if (mCount == null) mCount = findMethod(adapter.getClass(), "getItemCount");
                    if (mCount != null) itemCount = (int) mCount.invoke(adapter);
                } catch (Throwable ignored) {}

                Method mGetType = findMethod(adapter.getClass(), "b", int.class);
                if (mGetType == null) {
                    mGetType = findMethod(adapter.getClass(), "getItemViewType", int.class);
                }

                if (myType == -1 && mGetType != null) {
                    try {
                        myType = (int) mGetType.invoke(adapter, pos);
                    } catch (Throwable ignored) {}
                }

                if (mGetType != null && itemCount > 0 && myType != -1) {
                    boolean hasPrev = false;
                    if (pos > 0) {
                        int prevType = (int) mGetType.invoke(adapter, pos - 1);
                        hasPrev = (prevType == myType);
                    }

                    boolean hasNext = false;
                    if (pos + 1 < itemCount) {
                        int nextType = (int) mGetType.invoke(adapter, pos + 1);
                        hasNext = (nextType == myType);
                    }

                    if (hasPrev && hasNext) return CardPosition.MIDDLE;
                    if (!hasPrev && hasNext) return CardPosition.TOP;
                    if (hasPrev && !hasNext) return CardPosition.BOTTOM;
                    return CardPosition.SINGLE;
                }
            }

            // 2. Visual layout fallback: sort same-class siblings by vertical Y position
            if (parent != null) {
                Class<?> myClass = view.getClass();
                List<View> sameClassSiblings = new ArrayList<>();
                for (int i = 0; i < parent.getChildCount(); i++) {
                    View sibling = parent.getChildAt(i);
                    if (sibling.getVisibility() != View.GONE && sibling.getClass() == myClass) {
                        sameClassSiblings.add(sibling);
                    }
                }

                if (sameClassSiblings.size() > 1) {
                    java.util.Collections.sort(sameClassSiblings, (v1, v2) -> Float.compare(v1.getY(), v2.getY()));
                    int myVisualIndex = sameClassSiblings.indexOf(view);
                    if (myVisualIndex >= 0) {
                        boolean hasPrev = (myVisualIndex > 0);
                        boolean hasNext = (myVisualIndex < sameClassSiblings.size() - 1);

                        // If parent cannot scroll up, the topmost visible item is definitively the first item (TOP)
                        if (myVisualIndex == 0 && !parent.canScrollVertically(-1)) {
                            hasPrev = false;
                        }

                        if (hasPrev && hasNext) return CardPosition.MIDDLE;
                        if (!hasPrev && hasNext) return CardPosition.TOP;
                        if (hasPrev && !hasNext) return CardPosition.BOTTOM;
                        return CardPosition.SINGLE;
                    }
                } else if (sameClassSiblings.size() == 1) {
                    return CardPosition.SINGLE;
                }
            }
        } catch (Throwable ignored) {}

        return CardPosition.SINGLE;
    }

    /**
     * Apply dynamic continuous card layout to app item.
     */
    public static void setupAppItemCard(View view) {
        if (view == null) return;
        CardPosition pos = determineCardPosition(view);
        applyDarQCardLayout(view, pos);
        view.post(() -> {
            try {
                CardPosition p = determineCardPosition(view);
                applyDarQCardLayout(view, p);
            } catch (Throwable ignored) {}
        });
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                v.removeOnLayoutChangeListener(this);
                try {
                    CardPosition p = determineCardPosition(v);
                    applyDarQCardLayout(v, p);
                } catch (Throwable ignored) {}
            }
        });
    }



    /**
     * Apply balanced card layout to Focus Mode Schedule items:
     * - FocusModeScheduleListItemView ("排定时间")
     * - FocusModeAddScheduleListItemView ("+ 设置时间表")
     * Both normalized to 56dp height and symmetrical vertical margin.
     */
    public static void setupFocusModeScheduleCard(View view) {
        if (view == null) return;
        try {
            Context context = view.getContext();
            int pad6 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
            int h48 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
            int minH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, context.getResources().getDisplayMetrics());

            view.setMinimumHeight(minH);

            if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View c = vg.getChildAt(i);
                    int id = c.getId();
                    if (id != 0 && id != View.NO_ID) {
                        try {
                            String entry = c.getResources().getResourceEntryName(id);
                            if ("focus_mode_schedule_title".equals(entry)) {
                                c.setPadding(c.getPaddingLeft(), pad6, c.getPaddingRight(), c.getPaddingBottom());
                            } else if ("schedule_caption".equals(entry)) {
                                c.setPadding(c.getPaddingLeft(), c.getPaddingTop(), c.getPaddingRight(), pad6);
                            } else if ("delete_schedule_button".equals(entry)) {
                                ViewGroup.LayoutParams dlp = c.getLayoutParams();
                                if (dlp != null) {
                                    dlp.height = h48;
                                    c.setLayoutParams(dlp);
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }

            applyDarQCardLayout(view, CardPosition.SINGLE);

            // Symmetrical spacing between top description and bottom "Turn On Now" button
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                int margin16 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
                int margin4 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());
                int margin20 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, context.getResources().getDisplayMetrics());
                mlp.setMarginStart(margin16);
                mlp.setMarginEnd(margin16);
                mlp.leftMargin = margin16;
                mlp.rightMargin = margin16;
                mlp.topMargin = margin4;
                mlp.bottomMargin = margin20;
                view.setLayoutParams(mlp);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Apply Google Contacts Material 3 Expressive connected card UI:
     * - Connected card design with small corner radius at connection points:
     *   * TOP: rounded top 16dp, rounded bottom 4dp (connection notch), topMargin 2dp, bottomMargin 2dp
     *   * MIDDLE: rounded 4dp on all corners, topMargin 0, bottomMargin 2dp
     *   * BOTTOM: rounded top 4dp (connection notch), rounded bottom 16dp, topMargin 0, bottomMargin 2dp
     *   * SINGLE: rounded 16dp on all corners, topMargin 2dp, bottomMargin 2dp
     * - Dark card background: #1A202C
     * - 2dp gap between cards naturally reveals dark window background as a 100% full-width divider!
     * - Horizontal margins: 16dp
     */
    public static void applyDarQCardLayout(View root, CardPosition cardPosition) {
        if (root == null) return;
        try {
            Context context = root.getContext();
            boolean isNight = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

            int cardBgColor = isNight ? Color.parseColor("#1A202C") : Color.parseColor("#FFFFFF");
            int rippleColor = isNight ? Color.parseColor("#26FFFFFF") : Color.parseColor("#1A000000");

            float rLarge = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
            float rSmall = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());

            float[] radii;
            switch (cardPosition) {
                case TOP:
                    radii = new float[]{rLarge, rLarge, rLarge, rLarge, rSmall, rSmall, rSmall, rSmall};
                    break;
                case MIDDLE:
                    radii = new float[]{rSmall, rSmall, rSmall, rSmall, rSmall, rSmall, rSmall, rSmall};
                    break;
                case BOTTOM:
                    radii = new float[]{rSmall, rSmall, rSmall, rSmall, rLarge, rLarge, rLarge, rLarge};
                    break;
                case SINGLE:
                default:
                    radii = new float[]{rLarge, rLarge, rLarge, rLarge, rLarge, rLarge, rLarge, rLarge};
                    break;
            }

            GradientDrawable bgShape = new GradientDrawable();
            bgShape.setShape(GradientDrawable.RECTANGLE);
            bgShape.setColor(cardBgColor);
            bgShape.setCornerRadii(radii);

            GradientDrawable mask = new GradientDrawable();
            mask.setShape(GradientDrawable.RECTANGLE);
            mask.setColor(Color.WHITE);
            mask.setCornerRadii(radii);

            RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(rippleColor), bgShape, mask);
            root.setBackground(ripple);

            ViewGroup.LayoutParams lp = root.getLayoutParams();
            int margin16 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
            int gap2 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, context.getResources().getDisplayMetrics());

            ViewGroup.MarginLayoutParams mlp;
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                mlp = (ViewGroup.MarginLayoutParams) lp;
            } else {
                mlp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            mlp.setMarginStart(margin16);
            mlp.setMarginEnd(margin16);
            mlp.leftMargin = margin16;
            mlp.rightMargin = margin16;

            switch (cardPosition) {
                case SINGLE:
                    mlp.topMargin = gap2;
                    mlp.bottomMargin = gap2;
                    break;
                case TOP:
                    mlp.topMargin = gap2;
                    mlp.bottomMargin = gap2;
                    break;
                case MIDDLE:
                    mlp.topMargin = 0;
                    mlp.bottomMargin = gap2;
                    break;
                case BOTTOM:
                    mlp.topMargin = 0;
                    mlp.bottomMargin = gap2;
                    break;
            }
            root.setLayoutParams(mlp);
            root.requestLayout();
        } catch (Throwable ignored) {}
    }

    /**
     * Apply Google Contacts Material 3 Expressive continuous card layout to a PopupWindow:
     * - Dark window background: #12161F (#EEF0F6 in light mode) with 16dp rounded corners
     * - 2dp physical gap between items revealing the window background
    /**
     * Apply rounded corner card background and clean dividers to a PopupWindow.
     * - Card window background: #1A202C (#FFFFFF in light mode) with 16dp rounded corners
     */
    public static boolean isTopRightMenuAnchor(View anchor) {
        if (anchor == null) return false;
        if (isMenuChip(anchor)) return false;

        String cls = anchor.getClass().getName();
        if (cls.contains("OverflowMenuButton") || cls.contains("ActionMenuItemView")) {
            return true;
        }

        ViewParent parent = anchor.getParent();
        while (parent != null) {
            String pcls = parent.getClass().getName();
            if (pcls.contains("ActionMenuView") || pcls.contains("Toolbar") || pcls.contains("ActionBar")) {
                return true;
            }
            if (parent instanceof View) {
                parent = ((View) parent).getParent();
            } else {
                break;
            }
        }

        CharSequence desc = anchor.getContentDescription();
        if (desc != null) {
            String d = desc.toString().toLowerCase();
            if (d.contains("更多") || d.contains("more options") || d.contains("overflow")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMenuChip(View view) {
        if (view == null) return false;
        View cur = view;
        while (cur != null) {
            String cls = cur.getClass().getName();
            if (cls.contains("MenuChip") || cls.contains(".Chip")) {
                return true;
            }
            ViewParent p = cur.getParent();
            if (p instanceof View) {
                cur = (View) p;
            } else {
                break;
            }
        }
        return false;
    }

    /**
     * Style PopupWindow with rounded corners and dividers (ONLY for top-right overflow menu).
     * - 16dp rounded corners (smooth Material 3 aesthetic)
     * - Explicit elevation (6dp)
     * - Matches surfaceContainer / dark theme palette
     * - Completely disables scrollbars so no scrollbar ever appears
     * - Crisp 1dp visible divider between items (#2D3546 in dark, #E1E2EC in light)
     * - No extra padding to preserve exact wrap_content measurement and prevent height overflow
     */
    public static void applyPopupBackground(Object popupWindow) {
        if (popupWindow == null) return;
        try {
            if (!(popupWindow instanceof android.widget.PopupWindow)) return;
            android.widget.PopupWindow pw = (android.widget.PopupWindow) popupWindow;
            View contentView = pw.getContentView();
            Context context = contentView != null ? contentView.getContext() : null;
            if (context == null) {
                Field f = findField(android.widget.PopupWindow.class, "mContext");
                if (f != null) {
                    context = (Context) f.get(pw);
                }
            }
            if (context == null) return;

            boolean isNight = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            int cardBgColor = isNight ? Color.parseColor("#1A202C") : Color.parseColor("#FFFFFF");
            int dividerColor = isNight ? Color.parseColor("#2D3546") : Color.parseColor("#E1E2EC");

            float r16 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
            GradientDrawable windowBg = new GradientDrawable();
            windowBg.setShape(GradientDrawable.RECTANGLE);
            windowBg.setColor(cardBgColor);
            windowBg.setCornerRadius(r16);

            int elevation = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
            pw.setBackgroundDrawable(windowBg);
            pw.setElevation(elevation);

            if (contentView != null) {
                contentView.setClipToOutline(true);
                ListView lv = findListView(contentView);
                if (lv != null) {
                    // Disable all scrollbars completely
                    lv.setVerticalScrollBarEnabled(false);
                    lv.setHorizontalScrollBarEnabled(false);
                    lv.setOverScrollMode(View.OVER_SCROLL_NEVER);

                    // Add crisp 1dp divider
                    GradientDrawable divider = new GradientDrawable();
                    divider.setShape(GradientDrawable.RECTANGLE);
                    divider.setColor(dividerColor);
                    int dividerH = Math.max(1, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics()));
                    divider.setSize(-1, dividerH);
                    lv.setDivider(divider);
                    lv.setDividerHeight(dividerH);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static ListView findListView(View root) {
        if (root instanceof ListView) return (ListView) root;
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                ListView found = findListView(vg.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }



    /**
     * Resolve theme color attribute or return fallback color.
     */
    public static int resolveThemeColor(Context context, String attrName, int fallback) {
        if (context == null || attrName == null) return fallback;
        try {
            int id = context.getResources().getIdentifier(attrName, "attr", context.getPackageName());
            if (id == 0) {
                id = context.getResources().getIdentifier(attrName, "attr", "android");
            }
            if (id != 0) {
                TypedValue tv = new TypedValue();
                if (context.getTheme().resolveAttribute(id, tv, true)) {
                    if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                        return tv.data;
                    } else if (tv.resourceId != 0) {
                        return context.getColor(tv.resourceId);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return fallback;
    }

    /**
     * Format a TextView into a Material 3 Expressive start-aligned tonal pill button.
     * Matches the aesthetic and colors of the "Daily / 每天 ▼" filter chip.
     * Used for:
     * - "Remove sites / 移除网站" (App Details)
     * - "Remove past visits / 移除过去的访问记录" (Component Details)
     */
    public static void formatPillButton(TextView tv) {
        if (tv == null) return;
        try {
            Context context = tv.getContext();
            boolean isNight = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

            // Use vibrant primaryContainer blue matching "每天 ▼" chip (NOT muddy secondaryContainer)
            int defaultBg = isNight ? Color.parseColor("#004B75") : Color.parseColor("#D3E3FD");
            int containerColor = resolveThemeColor(context, "colorPrimaryContainer", defaultBg);

            int defaultText = isNight ? Color.parseColor("#B6E3FF") : Color.parseColor("#041E49");
            int textColor = resolveThemeColor(context, "colorOnPrimaryContainer", defaultText);

            int rippleColor = isNight ? Color.parseColor("#33FFFFFF") : Color.parseColor("#20000000");

            float r100 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());

            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setShape(GradientDrawable.RECTANGLE);
            pillBg.setColor(containerColor);
            pillBg.setCornerRadius(r100);

            GradientDrawable mask = new GradientDrawable();
            mask.setShape(GradientDrawable.RECTANGLE);
            mask.setColor(Color.WHITE);
            mask.setCornerRadius(r100);

            RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(rippleColor), pillBg, mask);

            tv.setBackground(ripple);
            tv.setTextColor(textColor);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            tv.setGravity(Gravity.CENTER);
            tv.setClickable(true);

            int padH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, context.getResources().getDisplayMetrics());
            int h36 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
            int margin16 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
            int bottomM = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, context.getResources().getDisplayMetrics());

            tv.setPaddingRelative(padH, 0, padH, 0);

            ViewGroup.LayoutParams lp = tv.getLayoutParams();
            ViewGroup.MarginLayoutParams mlp;
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                mlp = (ViewGroup.MarginLayoutParams) lp;
            } else {
                mlp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, h36);
            }
            mlp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            mlp.height = h36;
            mlp.leftMargin = margin16;
            mlp.rightMargin = margin16;
            mlp.setMarginStart(margin16);
            mlp.setMarginEnd(margin16);
            mlp.topMargin = margin16;
            mlp.bottomMargin = bottomM;

            if (mlp instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) mlp).gravity = Gravity.START;
            } else if (mlp instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) mlp).gravity = Gravity.START;
            }
            tv.setLayoutParams(mlp);
            tv.requestLayout();
        } catch (Throwable ignored) {}
    }






    public static View findViewByResourceEntryName(View root, String entryName) {
        if (root == null || entryName == null) return null;
        try {
            int id = root.getId();
            if (id != 0 && id != View.NO_ID) {
                String name = root.getResources().getResourceEntryName(id);
                if (entryName.equals(name)) return root;
            }
        } catch (Throwable ignored) {}
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View found = findViewByResourceEntryName(vg.getChildAt(i), entryName);
                if (found != null) return found;
            }
        }
        return null;
    }
}


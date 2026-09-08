package com.github.rove24.wellbeing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class TraditionalHookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "WellbeingM3E-Legacy";
    private static final String TARGET_PACKAGE = "com.google.android.apps.wellbeing";
    private static final Set<String> M3E_FLAGS = new HashSet<>(Arrays.asList(
            "45714530",
            "45761865",
            "45718700"
    ));

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        ClassLoader cl = lpparam.classLoader;
        XposedBridge.log(TAG + ": Hooking " + lpparam.packageName);

        // ==================== CORE M3E FLAG HOOKS ====================

        // 1. Hook SettingsThemeHelper (fcl)
        try {
            Class<?> fclClass = CardLayoutHelper.findTargetClass(cl, "fcl");
            if (fclClass != null) {
                for (Method m : fclClass.getDeclaredMethods()) {
                    if (Modifier.isStatic(m.getModifiers()) &&
                            m.getReturnType() == boolean.class &&
                            m.getParameterTypes().length == 1 &&
                            Context.class.isAssignableFrom(m.getParameterTypes()[0])) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true));
                        XposedBridge.log(TAG + ": Hooked fcl#" + m.getName() + " -> true");
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook fcl: " + t.getMessage());
        }

        // 2. Hook iiq.y
        try {
            Class<?> iiqClass = CardLayoutHelper.findTargetClass(cl, "iiq");
            if (iiqClass != null) {
                for (Method m : iiqClass.getDeclaredMethods()) {
                    if (Modifier.isStatic(m.getModifiers()) &&
                            m.getReturnType() == boolean.class &&
                            m.getParameterTypes().length == 2 &&
                            Context.class.isAssignableFrom(m.getParameterTypes()[0]) &&
                            m.getParameterTypes()[1] == boolean.class) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true));
                        XposedBridge.log(TAG + ": Hooked iiq#" + m.getName() + " -> true");
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook iiq: " + t.getMessage());
        }

        // 3. Hook jyy constructor
        try {
            Class<?> jyyClass = CardLayoutHelper.findTargetClass(cl, "jyy");
            if (jyyClass != null) {
                for (Constructor<?> ctor : jyyClass.getDeclaredConstructors()) {
                    Class<?>[] pts = ctor.getParameterTypes();
                    if (pts.length == 4 && pts[3] == boolean.class) {
                        XposedBridge.hookMethod(ctor, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                param.args[3] = true;
                            }
                        });
                        XposedBridge.log(TAG + ": Hooked jyy constructor");
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook jyy: " + t.getMessage());
        }

        // 4. Hook ilz constructor
        try {
            Class<?> ilzClass = CardLayoutHelper.findTargetClass(cl, "ilz");
            if (ilzClass != null) {
                for (Constructor<?> ctor : ilzClass.getDeclaredConstructors()) {
                    Class<?>[] pts = ctor.getParameterTypes();
                    if (pts.length == 3 && pts[2] == boolean.class) {
                        XposedBridge.hookMethod(ctor, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                param.args[2] = true;
                            }
                        });
                        XposedBridge.log(TAG + ": Hooked ilz constructor");
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook ilz: " + t.getMessage());
        }

        // 5. Hook twh.a()
        try {
            Class<?> twhClass = CardLayoutHelper.findTargetClass(cl, "twh");
            if (twhClass != null) {
                Field bField = CardLayoutHelper.findField(twhClass, "b");
                if (bField != null) {
                    for (Method m : twhClass.getDeclaredMethods()) {
                        if ("a".equals(m.getName()) && m.getParameterTypes().length == 0 && m.getReturnType() == Boolean.class) {
                            XposedBridge.hookMethod(m, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    int bVal = bField.getInt(param.thisObject);
                                    if (bVal == 0) param.setResult(Boolean.TRUE);
                                }
                            });
                            XposedBridge.log(TAG + ": Hooked twh#a()");
                        }
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook twh: " + t.getMessage());
        }

        // 6. Hook vmc.d(String, String)
        try {
            Class<?> vmcClass = CardLayoutHelper.findTargetClass(cl, "vmc");
            Class<?> qnuClass = CardLayoutHelper.findTargetClass(cl, "qnu");
            if (vmcClass != null && qnuClass != null) {
                Field cField = CardLayoutHelper.findField(qnuClass, "c");
                if (cField != null) {
                    Object trueQnu = cField.get(null);
                    for (Method m : vmcClass.getDeclaredMethods()) {
                        if ("d".equals(m.getName()) &&
                                m.getParameterTypes().length == 2 &&
                                m.getParameterTypes()[0] == String.class &&
                                m.getParameterTypes()[1] == String.class &&
                                m.getReturnType() == qnuClass) {
                            XposedBridge.hookMethod(m, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) {
                                    String flag = (String) param.args[1];
                                    if (flag != null && M3E_FLAGS.contains(flag)) param.setResult(trueQnu);
                                }
                            });
                            XposedBridge.log(TAG + ": Hooked vmc#d()");
                        }
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook vmc: " + t.getMessage());
        }

        // 7. Hook SystemProperties.getBoolean
        try {
            XposedHelpers.findAndHookMethod("android.os.SystemProperties", cl, "getBoolean",
                    String.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if ("is_expressive_design_enabled".equals(param.args[0])) param.setResult(true);
                        }
                    });
            XposedBridge.log(TAG + ": Hooked SystemProperties.getBoolean");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook SystemProperties: " + t.getMessage());
        }

        // 8. Hook Activity redirects
        try {
            Class<?> saClass = CardLayoutHelper.findTargetClass(cl, "com.google.android.apps.wellbeing.settings.SettingsActivity");
            if (saClass != null) {
                Method onCreate = CardLayoutHelper.findMethod(saClass, "onCreate", Bundle.class);
                if (onCreate != null) {
                    XposedBridge.hookMethod(onCreate, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Activity activity = (Activity) param.thisObject;
                                Intent intent = activity.getIntent();
                                if (intent != null && intent.getBooleanExtra("extra_m3e_redirected", false)) return;
                                Intent ni = new Intent();
                                ni.setClassName(activity, "com.google.android.apps.wellbeing.settings.preferences.SettingsPreferenceActivity");
                                if (intent != null) {
                                    if (intent.getAction() != null) ni.setAction(intent.getAction());
                                    if (intent.getData() != null) ni.setData(intent.getData());
                                    if (intent.getExtras() != null) ni.putExtras(intent.getExtras());
                                    ni.setFlags(intent.getFlags());
                                }
                                ni.putExtra("extra_m3e_redirected", true);
                                activity.startActivity(ni);
                                activity.finish();
                                param.setResult(null);
                            } catch (Throwable ignored) {}
                        }
                    });
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": SettingsActivity redirect failed: " + t.getMessage());
        }

        try {
            Class<?> daClass = CardLayoutHelper.findTargetClass(cl, "com.google.android.apps.wellbeing.dashboard.DashboardActivity");
            if (daClass != null) {
                Method onCreate = CardLayoutHelper.findMethod(daClass, "onCreate", Bundle.class);
                if (onCreate != null) {
                    XposedBridge.hookMethod(onCreate, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Activity activity = (Activity) param.thisObject;
                                Intent intent = activity.getIntent();
                                if (intent != null && intent.getBooleanExtra("extra_m3e_redirected", false)) return;
                                Intent ni = new Intent();
                                ni.setClassName(activity, "com.google.android.apps.wellbeing.dashboard.ui.preferences.DashboardPreferencesActivity");
                                if (intent != null) {
                                    if (intent.getAction() != null) ni.setAction(intent.getAction());
                                    if (intent.getData() != null) ni.setData(intent.getData());
                                    if (intent.getExtras() != null) ni.putExtras(intent.getExtras());
                                    ni.setFlags(intent.getFlags());
                                }
                                ni.putExtra("extra_m3e_redirected", true);
                                activity.startActivity(ni);
                                activity.finish();
                                param.setResult(null);
                            } catch (Throwable ignored) {}
                        }
                    });
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": DashboardActivity redirect failed: " + t.getMessage());
        }

        // ==================== SUB-PAGE HOOKS ====================

        // MainSwitchBar constructor (synchronous margin, no flash)
        hookMainSwitchBarConstructor(cl);

        // Mindful Nudge switch bar (使用提醒功能)
        hookMindfulNudgeSwitchBar(cl);

        // Walking Detection (看路提醒)
        hookWalkingDetection(cl);

        // CompositeToggle (CheckBox -> Switch + cards)
        hookCompositeToggle(cl);

        // App list binders for Mindful Nudge and Focus Mode
        hookAppListBinders(cl);

        // Focus Mode schedule button card styling
        hookScheduleButtonBinder(cl);

        // PopupWindow rounded corners
        hookPopupWindowStyle(cl);
    }

    private void hookMindfulNudgeSwitchBar(ClassLoader cl) {
        try {
            Class<?> jowClass = CardLayoutHelper.findTargetClass(cl, "jow");
            if (jowClass != null) {
                Field bField = CardLayoutHelper.findField(jowClass, "b");
                if (bField != null) {
                    for (Constructor<?> ctor : jowClass.getDeclaredConstructors()) {
                        XposedBridge.hookMethod(ctor, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                try {
                                    View view = (View) bField.get(param.thisObject);
                                    if (view != null) {
                                        CardLayoutHelper.applySwitchBarMargin(view);
                                    }
                                } catch (Throwable ignored) {}
                            }
                        });
                    }
                    XposedBridge.log(TAG + ": Hooked jow constructor for switch bar margin");
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook jow: " + t.getMessage());
        }

        try {
            Class<?> jouClass = CardLayoutHelper.findTargetClass(cl, "jou");
            if (jouClass != null) {
                for (Method m : jouClass.getDeclaredMethods()) {
                    if ("b".equals(m.getName()) && m.getParameterTypes().length == 2 && View.class.isAssignableFrom(m.getParameterTypes()[0])) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                try {
                                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof View) {
                                        View view = (View) param.args[0];
                                        int id = view.getResources().getIdentifier("mindful_nudge_switch", "id", view.getContext().getPackageName());
                                        View switchBar = id != 0 ? view.findViewById(id) : null;
                                        if (switchBar != null) {
                                            CardLayoutHelper.applySwitchBarMargin(switchBar);
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                        });
                    }
                }
                XposedBridge.log(TAG + ": Hooked jou.b for switch bar margin");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook jou: " + t.getMessage());
        }

        try {
            Class<?> mnsClass = CardLayoutHelper.findTargetClass(cl, "com.google.android.apps.wellbeing.mindfulnudge.ui.MindfulNudgeSettingsSwitchListItemView");
            if (mnsClass != null) {
                Method onAttached = CardLayoutHelper.findMethod(mnsClass, "onAttachedToWindow");
                if (onAttached != null) {
                    XposedBridge.hookMethod(onAttached, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                ViewGroup vg = (ViewGroup) param.thisObject;
                                int id = vg.getResources().getIdentifier("mindful_nudge_switch", "id", vg.getContext().getPackageName());
                                View switchBar = id != 0 ? vg.findViewById(id) : null;
                                if (switchBar != null) {
                                    CardLayoutHelper.applySwitchBarMargin(switchBar);
                                }
                            } catch (Throwable ignored) {}
                        }
                    });
                    XposedBridge.log(TAG + ": Hooked MindfulNudgeSettingsSwitchListItemView.onAttachedToWindow");
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook MindfulNudgeSettingsSwitchListItemView: " + t.getMessage());
        }
    }

    private void hookWalkingDetection(ClassLoader cl) {
        try {
            Class<?> actClass = CardLayoutHelper.findTargetClass(cl, "com.google.android.apps.wellbeing.walkingdetection.ui.WalkingDetectionSettingsActivity");
            if (actClass != null) {
                for (Method m : actClass.getDeclaredMethods()) {
                    String name = m.getName();
                    if ("onPostResume".equals(name) || "onResume".equals(name) || "onAttachedToWindow".equals(name)) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                try {
                                    Activity activity = (Activity) param.thisObject;
                                    CardLayoutHelper.formatWalkingDetectionActivity(activity);
                                    View decorView = activity.getWindow().getDecorView();
                                    decorView.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                                        CardLayoutHelper.formatWalkingDetectionViews(v);
                                    });
                                } catch (Throwable ignored) {}
                            }
                        });
                    }
                }
                XposedBridge.log(TAG + ": Hooked WalkingDetectionSettingsActivity onResume/onPostResume for card styling");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook WalkingDetectionSettingsActivity: " + t.getMessage());
        }

        // Also hook lkc fragment onViewCreated
        try {
            Class<?> lkcClass = CardLayoutHelper.findTargetClass(cl, "lkc");
            if (lkcClass != null) {
                Method onViewCreated = CardLayoutHelper.findMethod(lkcClass, "onViewCreated", View.class, Bundle.class);
                if (onViewCreated != null) {
                    XposedBridge.hookMethod(onViewCreated, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                if (param.args != null && param.args.length > 0 && param.args[0] instanceof View) {
                                    View v = (View) param.args[0];
                                    v.post(() -> CardLayoutHelper.formatWalkingDetectionViews(v));
                                }
                            } catch (Throwable ignored) {}
                        }
                    });
                    XposedBridge.log(TAG + ": Hooked lkc.onViewCreated for WalkingDetection");
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook lkc: " + t.getMessage());
        }
    }

    private void hookCompositeToggle(ClassLoader cl) {
        try {
            Class<?> ctClass = CardLayoutHelper.findTargetClass(cl,
                    "com.google.android.apps.wellbeing.common.ui.compositetoggle.CompositeToggle");
            if (ctClass != null) {
                Method hMethod = CardLayoutHelper.findMethod(ctClass, "h", String.class, boolean.class);
                if (hMethod != null) {
                    XposedBridge.hookMethod(hMethod, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                CardLayoutHelper.handleCompositeToggleSetup((View) param.thisObject, cl);
                            } catch (Throwable ignored) {}
                        }
                    });
                    XposedBridge.log(TAG + ": Hooked CompositeToggle.h()");
                }
                Method qMethod = CardLayoutHelper.findMethod(ctClass, "q", CharSequence.class);
                if (qMethod != null) {
                    XposedBridge.hookMethod(qMethod, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                CardLayoutHelper.handleCompositeToggleSetup((View) param.thisObject, cl);
                            } catch (Throwable ignored) {}
                        }
                    });
                    XposedBridge.log(TAG + ": Hooked CompositeToggle.q()");
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook CompositeToggle: " + t.getMessage());
        }
    }

    private void hookAppListBinders(ClassLoader cl) {
        hookCompositeToggleBinder(cl, "joq", "Mindful Nudge");
        hookCompositeToggleBinder(cl, "jdc", "Focus Mode");
    }

    private void hookCompositeToggleBinder(ClassLoader cl, String className, String desc) {
        try {
            Class<?> binderClass = CardLayoutHelper.findTargetClass(cl, className);
            if (binderClass != null) {
                for (Method m : binderClass.getDeclaredMethods()) {
                    if ("b".equals(m.getName()) && m.getParameterTypes().length == 2 && View.class.isAssignableFrom(m.getParameterTypes()[0])) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                try {
                                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof View) {
                                        View view = (View) param.args[0];
                                        CardLayoutHelper.handleCompositeToggleSetup(view, cl);
                                    }
                                } catch (Throwable ignored) {}
                            }
                        });
                        XposedBridge.log(TAG + ": Hooked " + className + ".b() for " + desc);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook " + className + ": " + t.getMessage());
        }
    }



    private void hookMainSwitchBarConstructor(ClassLoader cl) {
        try {
            Class<?> msbClass = CardLayoutHelper.findTargetClass(cl, "com.android.settingslib.widget.MainSwitchBar");
            if (msbClass != null) {
                for (Constructor<?> ctor : msbClass.getDeclaredConstructors()) {
                    XposedBridge.hookMethod(ctor, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                View view = (View) param.thisObject;
                                view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                                    @Override
                                    public void onViewAttachedToWindow(View v) {
                                        CardLayoutHelper.applySwitchBarMargin(v);
                                    }
                                    @Override
                                    public void onViewDetachedFromWindow(View v) {}
                                });
                            } catch (Throwable ignored) {}
                        }
                    });
                }

                for (Method m : msbClass.getDeclaredMethods()) {
                    String name = m.getName();
                    if ("f".equals(name) || "e".equals(name) || "c".equals(name)) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                try {
                                    View view = (View) param.thisObject;
                                    CardLayoutHelper.applySwitchBarMargin(view);
                                } catch (Throwable ignored) {}
                            }
                        });
                    }
                }
                XposedBridge.log(TAG + ": Hooked MainSwitchBar constructors and methods for 16dp margins");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook MainSwitchBar constructor: " + t.getMessage());
        }
    }

    private void hookScheduleButtonBinder(ClassLoader cl) {
        hookScheduleViewOnAttached(cl, "com.google.android.apps.wellbeing.focusmode.ui.FocusModeAddScheduleListItemView", "Focus Mode Add Schedule");
        hookScheduleViewOnAttached(cl, "com.google.android.apps.wellbeing.focusmode.ui.FocusModeScheduleListItemView", "Focus Mode Schedule Item");
    }

    private void hookScheduleViewOnAttached(ClassLoader cl, String className, String desc) {
        try {
            Class<?> clazz = CardLayoutHelper.findTargetClass(cl, className);
            if (clazz != null) {
                Method onAttached = CardLayoutHelper.findMethod(clazz, "onAttachedToWindow");
                if (onAttached != null) {
                    XposedBridge.hookMethod(onAttached, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                View view = (View) param.thisObject;
                                CardLayoutHelper.setupFocusModeScheduleCard(view);
                            } catch (Throwable ignored) {}
                        }
                    });
                    XposedBridge.log(TAG + ": Hooked " + className + ".onAttachedToWindow for " + desc);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook " + className + ": " + t.getMessage());
        }
    }

    private void hookPopupWindowStyle(ClassLoader cl) {
        try {
            Class<?> pwClass = android.widget.PopupWindow.class;
            for (Method m : pwClass.getDeclaredMethods()) {
                if ("showAsDropDown".equals(m.getName())) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                View anchor = (param.args.length > 0 && param.args[0] instanceof View) ? (View) param.args[0] : null;
                                if (CardLayoutHelper.isTopRightMenuAnchor(anchor)) {
                                    CardLayoutHelper.applyPopupBackground(param.thisObject);
                                }
                            } catch (Throwable ignored) {}
                        }
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                View anchor = (param.args.length > 0 && param.args[0] instanceof View) ? (View) param.args[0] : null;
                                if (CardLayoutHelper.isTopRightMenuAnchor(anchor)) {
                                    CardLayoutHelper.applyPopupBackground(param.thisObject);
                                }
                            } catch (Throwable ignored) {}
                        }
                    });
                }
            }
            XposedBridge.log(TAG + ": Hooked PopupWindow.showAsDropDown for top-right menu rounded corners");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook PopupWindow: " + t.getMessage());
        }
    }
}
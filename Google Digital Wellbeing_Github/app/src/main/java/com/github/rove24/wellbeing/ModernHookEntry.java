package com.github.rove24.wellbeing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

public class ModernHookEntry extends XposedModule {

    private static final String TAG = "WellbeingM3E";
    private static final String TARGET_PACKAGE = "com.google.android.apps.wellbeing";
    private static final Set<String> M3E_FLAGS = new HashSet<>(Arrays.asList(
            "45714530",
            "45761865",
            "45718700"
    ));

    public ModernHookEntry() {
        super();
    }

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        log(Log.INFO, TAG, "Module loaded into " + param.getProcessName() +
                ", framework: " + getFrameworkName() + " " + getFrameworkVersion() + " (API " + getApiVersion() + ")");
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        String packageName = param.getPackageName();
        if (!TARGET_PACKAGE.equals(packageName)) {
            return;
        }

        ClassLoader cl = param.getClassLoader();
        log(Log.INFO, TAG, "Target package " + packageName + " is ready. Applying M3E hooks...");

        // Core M3E flags
        hookSettingsThemeHelper(cl);
        hookRoutingGateways(cl);
        hookPhenotypeFlags(cl);
        hookSystemProperties(cl);
        hookActivityRedirects(cl);

        // Sub-page hooks
        hookMainSwitchBarConstructor(cl);
        hookMindfulNudgeSwitchBar(cl);
        hookWalkingDetection(cl);
        hookCompositeToggle(cl);
        hookAppListBinders(cl);
        hookScheduleButtonBinder(cl);
        hookPopupWindowStyle(cl);

        log(Log.INFO, TAG, "All M3E hooks successfully initialized!");
    }

    // ==================== CORE M3E FLAG HOOKS ====================

    private void hookSettingsThemeHelper(ClassLoader cl) {
        try {
            Class<?> fclClass = CardLayoutHelper.findTargetClass(cl, "fcl");
            if (fclClass != null) {
                for (Method m : fclClass.getDeclaredMethods()) {
                    if (Modifier.isStatic(m.getModifiers()) &&
                            m.getReturnType() == boolean.class &&
                            m.getParameterTypes().length == 1 &&
                            Context.class.isAssignableFrom(m.getParameterTypes()[0])) {
                        hook(m).intercept(chain -> true);
                        log(Log.INFO, TAG, "Hooked fcl#" + m.getName() + " -> true");
                    }
                }
            } else {
                log(Log.WARN, TAG, "fcl not found");
            }
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook SettingsThemeHelper", t);
        }
    }

    private void hookRoutingGateways(ClassLoader cl) {
        try {
            Class<?> iiqClass = CardLayoutHelper.findTargetClass(cl, "iiq");
            if (iiqClass != null) {
                for (Method m : iiqClass.getDeclaredMethods()) {
                    if (Modifier.isStatic(m.getModifiers()) &&
                            m.getReturnType() == boolean.class &&
                            m.getParameterTypes().length == 2 &&
                            Context.class.isAssignableFrom(m.getParameterTypes()[0]) &&
                            m.getParameterTypes()[1] == boolean.class) {
                        hook(m).intercept(chain -> true);
                        log(Log.INFO, TAG, "Hooked iiq#" + m.getName() + " -> true");
                    }
                }
            }
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook iiq.y", t);
        }

        try {
            Class<?> jyyClass = CardLayoutHelper.findTargetClass(cl, "jyy");
            if (jyyClass != null) {
                for (Constructor<?> ctor : jyyClass.getDeclaredConstructors()) {
                    Class<?>[] pts = ctor.getParameterTypes();
                    if (pts.length == 4 && pts[3] == boolean.class) {
                        hook(ctor).intercept(chain -> {
                            List<Object> args = chain.getArgs();
                            Object[] newArgs = new Object[]{args.get(0), args.get(1), args.get(2), Boolean.TRUE};
                            return chain.proceed(newArgs);
                        });
                        log(Log.INFO, TAG, "Hooked jyy constructor");
                    }
                }
            }
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook jyy", t);
        }

        try {
            Class<?> ilzClass = CardLayoutHelper.findTargetClass(cl, "ilz");
            if (ilzClass != null) {
                for (Constructor<?> ctor : ilzClass.getDeclaredConstructors()) {
                    Class<?>[] pts = ctor.getParameterTypes();
                    if (pts.length == 3 && pts[2] == boolean.class) {
                        hook(ctor).intercept(chain -> {
                            List<Object> args = chain.getArgs();
                            Object[] newArgs = new Object[]{args.get(0), args.get(1), Boolean.TRUE};
                            return chain.proceed(newArgs);
                        });
                        log(Log.INFO, TAG, "Hooked ilz constructor");
                    }
                }
            }
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook ilz", t);
        }
    }

    private void hookPhenotypeFlags(ClassLoader cl) {
        try {
            Class<?> twhClass = CardLayoutHelper.findTargetClass(cl, "twh");
            if (twhClass != null) {
                Field bField = CardLayoutHelper.findField(twhClass, "b");
                if (bField != null) {
                    for (Method m : twhClass.getDeclaredMethods()) {
                        if ("a".equals(m.getName()) && m.getParameterTypes().length == 0 && m.getReturnType() == Boolean.class) {
                            hook(m).intercept(chain -> {
                                int bVal = bField.getInt(chain.getThisObject());
                                if (bVal == 0) return Boolean.TRUE;
                                return chain.proceed();
                            });
                            log(Log.INFO, TAG, "Hooked twh#a()");
                        }
                    }
                }
            }
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook twh", t);
        }

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
                            hook(m).intercept(chain -> {
                                String flagName = (String) chain.getArg(1);
                                if (flagName != null && M3E_FLAGS.contains(flagName)) return trueQnu;
                                return chain.proceed();
                            });
                            log(Log.INFO, TAG, "Hooked vmc#d()");
                        }
                    }
                }
            }
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook vmc.d", t);
        }
    }

    private void hookSystemProperties(ClassLoader cl) {
        try {
            Class<?> spClass = Class.forName("android.os.SystemProperties");
            Method getBooleanMethod = spClass.getDeclaredMethod("getBoolean", String.class, boolean.class);
            hook(getBooleanMethod).intercept(chain -> {
                String key = (String) chain.getArg(0);
                if ("is_expressive_design_enabled".equals(key)) return true;
                return chain.proceed();
            });
            log(Log.INFO, TAG, "Hooked SystemProperties.getBoolean");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook SystemProperties", t);
        }
    }

    private void hookActivityRedirects(ClassLoader cl) {
        try {
            Class<?> saClass = Class.forName("com.google.android.apps.wellbeing.settings.SettingsActivity", true, cl);
            Method onCreate = CardLayoutHelper.findMethod(saClass, "onCreate", Bundle.class);
            if (onCreate != null) {
                hook(onCreate).intercept(chain -> {
                    Activity activity = (Activity) chain.getThisObject();
                    Intent intent = activity.getIntent();
                    if (intent != null && intent.getBooleanExtra("extra_m3e_redirected", false)) {
                        return chain.proceed();
                    }
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
                    return null;
                });
                log(Log.INFO, TAG, "Hooked SettingsActivity.onCreate redirect");
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "SettingsActivity redirect failed: " + t.getMessage());
        }

        try {
            Class<?> daClass = Class.forName("com.google.android.apps.wellbeing.dashboard.DashboardActivity", true, cl);
            Method onCreate = CardLayoutHelper.findMethod(daClass, "onCreate", Bundle.class);
            if (onCreate != null) {
                hook(onCreate).intercept(chain -> {
                    Activity activity = (Activity) chain.getThisObject();
                    Intent intent = activity.getIntent();
                    if (intent != null && intent.getBooleanExtra("extra_m3e_redirected", false)) {
                        return chain.proceed();
                    }
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
                    return null;
                });
                log(Log.INFO, TAG, "Hooked DashboardActivity.onCreate redirect");
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "DashboardActivity redirect failed: " + t.getMessage());
        }
    }

    // ==================== SUB-PAGE HOOKS ====================

    /**
     * Hook MainSwitchBar constructor to synchronously apply margin.
     * This eliminates the flash/shrink effect caused by post() delayed margin setting.
     */
    private void hookMainSwitchBarConstructor(ClassLoader cl) {
        try {
            Class<?> msbClass = CardLayoutHelper.findTargetClass(cl, "com.android.settingslib.widget.MainSwitchBar");
            if (msbClass != null) {
                for (Constructor<?> ctor : msbClass.getDeclaredConstructors()) {
                    hook(ctor).intercept(chain -> {
                        Object proceed = chain.proceed();
                        try {
                            View view = (View) chain.getThisObject();
                            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                                @Override
                                public void onViewAttachedToWindow(View v) {
                                    CardLayoutHelper.applySwitchBarMargin(v);
                                }
                                @Override
                                public void onViewDetachedFromWindow(View v) {}
                            });
                        } catch (Throwable ignored) {}
                        return proceed;
                    });
                }

                // Also hook f(), e(CharSequence), and c(boolean) when view is already configured
                for (Method m : msbClass.getDeclaredMethods()) {
                    String name = m.getName();
                    if ("f".equals(name) || "e".equals(name) || "c".equals(name)) {
                        hook(m).intercept(chain -> {
                            Object proceed = chain.proceed();
                            try {
                                View view = (View) chain.getThisObject();
                                CardLayoutHelper.applySwitchBarMargin(view);
                            } catch (Throwable ignored) {}
                            return proceed;
                        });
                    }
                }
                log(Log.INFO, TAG, "Hooked MainSwitchBar constructors and methods for 16dp margins");
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to hook MainSwitchBar constructor: " + t.getMessage());
        }
    }

    /**
     * Mindful Nudge (使用提醒功能) switch bar hook (v0.9).
     */
    private void hookMindfulNudgeSwitchBar(ClassLoader cl) {
        try {
            Class<?> jowClass = CardLayoutHelper.findTargetClass(cl, "jow");
            if (jowClass != null) {
                Field bField = CardLayoutHelper.findField(jowClass, "b");
                if (bField != null) {
                    for (Constructor<?> ctor : jowClass.getDeclaredConstructors()) {
                        hook(ctor).intercept(chain -> {
                            Object proceed = chain.proceed();
                            try {
                                View view = (View) bField.get(chain.getThisObject());
                                if (view != null) {
                                    CardLayoutHelper.applySwitchBarMargin(view);
                                }
                            } catch (Throwable ignored) {}
                            return proceed;
                        });
                    }
                    log(Log.INFO, TAG, "Hooked jow constructor for switch bar margin");
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to hook jow: " + t.getMessage());
        }

        try {
            Class<?> jouClass = CardLayoutHelper.findTargetClass(cl, "jou");
            if (jouClass != null) {
                for (Method m : jouClass.getDeclaredMethods()) {
                    if ("b".equals(m.getName()) && m.getParameterTypes().length == 2 && View.class.isAssignableFrom(m.getParameterTypes()[0])) {
                        hook(m).intercept(chain -> {
                            Object proceed = chain.proceed();
                            try {
                                List<Object> args = chain.getArgs();
                                if (args != null && !args.isEmpty() && args.get(0) instanceof View) {
                                    View view = (View) args.get(0);
                                    int id = view.getResources().getIdentifier("mindful_nudge_switch", "id", view.getContext().getPackageName());
                                    View switchBar = id != 0 ? view.findViewById(id) : null;
                                    if (switchBar != null) {
                                        CardLayoutHelper.applySwitchBarMargin(switchBar);
                                    }
                                }
                            } catch (Throwable ignored) {}
                            return proceed;
                        });
                    }
                }
                log(Log.INFO, TAG, "Hooked jou.b for switch bar margin");
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to hook jou: " + t.getMessage());
        }

        try {
            Class<?> mnsClass = CardLayoutHelper.findTargetClass(cl, "com.google.android.apps.wellbeing.mindfulnudge.ui.MindfulNudgeSettingsSwitchListItemView");
            if (mnsClass != null) {
                Method onAttached = CardLayoutHelper.findMethod(mnsClass, "onAttachedToWindow");
                if (onAttached != null) {
                    hook(onAttached).intercept(chain -> {
                        Object proceed = chain.proceed();
                        try {
                            ViewGroup vg = (ViewGroup) chain.getThisObject();
                            int id = vg.getResources().getIdentifier("mindful_nudge_switch", "id", vg.getContext().getPackageName());
                            View switchBar = id != 0 ? vg.findViewById(id) : null;
                            if (switchBar != null) {
                                CardLayoutHelper.applySwitchBarMargin(switchBar);
                            }
                        } catch (Throwable ignored) {}
                        return proceed;
                    });
                    log(Log.INFO, TAG, "Hooked MindfulNudgeSettingsSwitchListItemView.onAttachedToWindow");
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to hook MindfulNudgeSettingsSwitchListItemView: " + t.getMessage());
        }
    }

    /**
     * Walking Detection (看路提醒) hook - strict v0.9 baseline.
     */
    private void hookWalkingDetection(ClassLoader cl) {
        try {
            Class<?> actClass = CardLayoutHelper.findTargetClass(cl, "com.google.android.apps.wellbeing.walkingdetection.ui.WalkingDetectionSettingsActivity");
            if (actClass != null) {
                for (Method m : actClass.getDeclaredMethods()) {
                    String name = m.getName();
                    if ("onPostResume".equals(name) || "onResume".equals(name) || "onAttachedToWindow".equals(name)) {
                        hook(m).intercept(chain -> {
                            Object proceed = chain.proceed();
                            try {
                                Activity activity = (Activity) chain.getThisObject();
                                CardLayoutHelper.formatWalkingDetectionActivity(activity);
                                View decorView = activity.getWindow().getDecorView();
                                decorView.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                                    CardLayoutHelper.formatWalkingDetectionViews(v);
                                });
                            } catch (Throwable ignored) {}
                            return proceed;
                        });
                    }
                }
                log(Log.INFO, TAG, "Hooked WalkingDetectionSettingsActivity onResume/onPostResume for card styling");
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to hook WalkingDetectionSettingsActivity: " + t.getMessage());
        }

        // Also hook lkc fragment onViewCreated
        try {
            Class<?> lkcClass = CardLayoutHelper.findTargetClass(cl, "lkc");
            if (lkcClass != null) {
                Method onViewCreated = CardLayoutHelper.findMethod(lkcClass, "onViewCreated", View.class, Bundle.class);
                if (onViewCreated != null) {
                    hook(onViewCreated).intercept(chain -> {
                        Object proceed = chain.proceed();
                        try {
                            List<Object> args = chain.getArgs();
                            if (args != null && !args.isEmpty() && args.get(0) instanceof View) {
                                View v = (View) args.get(0);
                                v.post(() -> CardLayoutHelper.formatWalkingDetectionViews(v));
                            }
                        } catch (Throwable ignored) {}
                        return proceed;
                    });
                    log(Log.INFO, TAG, "Hooked lkc.onViewCreated for WalkingDetection");
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to hook lkc: " + t.getMessage());
        }
    }

    /**
     * Convert CheckBox -> Switch and apply card layout on data binding (v0.9).
     */
    private void hookCompositeToggle(ClassLoader cl) {
        try {
            Class<?> ctClass = CardLayoutHelper.findTargetClass(cl,
                    "com.google.android.apps.wellbeing.common.ui.compositetoggle.CompositeToggle");
            if (ctClass != null) {
                Method hMethod = CardLayoutHelper.findMethod(ctClass, "h", String.class, boolean.class);
                if (hMethod != null) {
                    hook(hMethod).intercept(chain -> {
                        Object result = chain.proceed();
                        try {
                            View view = (View) chain.getThisObject();
                            CardLayoutHelper.handleCompositeToggleSetup(view, cl);
                        } catch (Throwable ignored) {}
                        return result;
                    });
                    log(Log.INFO, TAG, "Hooked CompositeToggle.h(String, boolean)");
                }
                Method qMethod = CardLayoutHelper.findMethod(ctClass, "q", CharSequence.class);
                if (qMethod != null) {
                    hook(qMethod).intercept(chain -> {
                        Object result = chain.proceed();
                        try {
                            View view = (View) chain.getThisObject();
                            CardLayoutHelper.handleCompositeToggleSetup(view, cl);
                        } catch (Throwable ignored) {}
                        return result;
                    });
                    log(Log.INFO, TAG, "Hooked CompositeToggle.q(CharSequence)");
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to hook CompositeToggle: " + t.getMessage());
        }
    }

    /**
     * Hook RecyclerView item binders for Mindful Nudge and Focus Mode (v0.9).
     */
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
                        hook(m).intercept(chain -> {
                            Object result = chain.proceed();
                            try {
                                List<Object> args = chain.getArgs();
                                if (args != null && !args.isEmpty() && args.get(0) instanceof View) {
                                    View view = (View) args.get(0);
                                    CardLayoutHelper.handleCompositeToggleSetup(view, cl);
                                }
                            } catch (Throwable ignored) {}
                            return result;
                        });
                        log(Log.INFO, TAG, "Hooked " + className + ".b() for " + desc);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to hook " + className + ": " + t.getMessage());
        }
    }



    /**
     * Focus Mode schedule buttons card styling:
     * - FocusModeAddScheduleListItemView: "+ Set Schedule" button
     * - FocusModeScheduleListItemView: Existing schedule item
     */
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
                    hook(onAttached).intercept(chain -> {
                        Object result = chain.proceed();
                        try {
                            View view = (View) chain.getThisObject();
                            CardLayoutHelper.setupFocusModeScheduleCard(view);
                        } catch (Throwable ignored) {}
                        return result;
                    });
                    log(Log.INFO, TAG, "Hooked " + className + ".onAttachedToWindow for " + desc);
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to hook " + className + ": " + t.getMessage());
        }
    }

    /**
     * Style PopupWindows with rounded corners and dividers (ONLY for top-right overflow menu).
     * Excludes MenuChipView dropdowns to preserve official native M3 popup styling.
     */
    private void hookPopupWindowStyle(ClassLoader cl) {
        try {
            Class<?> pwClass = android.widget.PopupWindow.class;
            for (Method m : pwClass.getDeclaredMethods()) {
                if ("showAsDropDown".equals(m.getName())) {
                    hook(m).intercept(chain -> {
                        View anchor = null;
                        try {
                            List<Object> args = chain.getArgs();
                            if (args != null && !args.isEmpty() && args.get(0) instanceof View) {
                                anchor = (View) args.get(0);
                            }
                            if (CardLayoutHelper.isTopRightMenuAnchor(anchor)) {
                                CardLayoutHelper.applyPopupBackground(chain.getThisObject());
                            }
                        } catch (Throwable ignored) {}
                        Object result = chain.proceed();
                        try {
                            if (CardLayoutHelper.isTopRightMenuAnchor(anchor)) {
                                CardLayoutHelper.applyPopupBackground(chain.getThisObject());
                            }
                        } catch (Throwable ignored) {}
                        return result;
                    });
                }
            }
            log(Log.INFO, TAG, "Hooked PopupWindow.showAsDropDown for top-right menu rounded corners");
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to hook PopupWindow: " + t.getMessage());
        }
    }
}
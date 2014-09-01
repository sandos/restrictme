package se.sandos.restrictme;


import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.lang.reflect.Member;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedHooker implements IXposedHookZygoteInit, IXposedHookLoadPackage {
	private XSharedPreferences prefs;

	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if(lpparam.appInfo == null || lpparam.appInfo.packageName == null) {
        	return;
        }
        
		if (lpparam.appInfo.packageName.equals("com.android.settings")) {
			try {
				XC_MethodHook hook = new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param)
							throws Throwable {
						
						if(prefs == null) {
							return;
						}
						prefs.reload();
						if(!prefs.getBoolean("active", false)) {
							return;
						}
						
						Activity act = (Activity)param.thisObject;
						UserManager  systemService = (UserManager) act.getApplicationContext().getSystemService(Context.USER_SERVICE);
						Bundle restrictions = systemService.getUserRestrictions();
						if(restrictions.getBoolean(UserManager.DISALLOW_MODIFY_ACCOUNTS, false)) {
							XposedBridge.log("RestrictMe Killing settings");
							act.finish();
						}
					}
				};
				Unhook findAndHookMethod2 = findAndHookMethod("com.android.settings.Settings", lpparam.classLoader,
					"onCreate", Bundle.class, hook);
				Member hookedMethod = findAndHookMethod2.getHookedMethod();
				if(hookedMethod == null) {
					XposedBridge.log("RestrictMe Failed hooking " + lpparam.packageName);
				}
			} catch(Throwable e) {
				XposedBridge.log("RestrictMe Failed: " + e.getMessage());
			}
		}
    }

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		prefs = new XSharedPreferences(XposedHooker.class.getPackage().getName());
		prefs.makeWorldReadable();
	}
}

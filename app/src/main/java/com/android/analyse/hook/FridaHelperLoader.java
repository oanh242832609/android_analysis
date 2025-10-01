package com.android.analyse.hook;

import com.common.log;
import com.common.utils;

import dalvik.system.DexClassLoader;
import android.content.Context;
import android.content.pm.PackageManager;
import android.app.Application;
import java.io.File;
import de.robv.android.xposed.XposedHelpers;

public class FridaHelperLoader {
    static public boolean hasInjectFridaHelper = false;
    static public Class GsonJson;
    static public Class FridaHelper;

    static boolean InjectFridaHelp(ClassLoader classLoader) {
        if (hasInjectFridaHelper) {
            return hasInjectFridaHelper;
        }
        try {
            // 1) Пытаемся загрузить нужные классы напрямую из APK модуля (часто достаточно, без внешнего DEX)
            try {
                GsonJson = classLoader.loadClass("com.fucker.gson.Gson");
            } catch (Throwable ignore) { }
            try {
                FridaHelper = classLoader.loadClass("com.frida.frida_helper");
            } catch (Throwable ignore) { }

            if (GsonJson != null && FridaHelper != null) {
                hasInjectFridaHelper = true;
                log.i("inject from module classes success!");
                return true;
            }

            // 2) Если напрямую не вышло — готовим путь для локального dex во внутреннем каталоге целевого процесса
            Application app = (Application) XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.ActivityThread", classLoader),
                    "currentApplication"
            );
            Context ctx = app != null ? app.getApplicationContext() : null;
            if (ctx == null) {
                log.e("context is null, cannot prepare frida_helper.dex");
                return false;
            }

            // Пытаемся получить контекст пакета модуля, чтобы прочитать ассеты модуля
            Context moduleCtx = null;
            try {
                moduleCtx = ctx.createPackageContext("com.android.analyse", Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
            } catch (PackageManager.NameNotFoundException e) {
                log.e("createPackageContext error: " + e);
            }

            String localDexPath = new File(ctx.getFilesDir(), "frida_helper.dex").getAbsolutePath();

            // Если файла нет — извлечём из ассетов модуля
            try {
                File f = new File(localDexPath);
                if (!f.exists() && moduleCtx != null) {
                    byte[] data = utils.ReadFromAssets(moduleCtx, "frida_helper.dex");
                    utils.save_file(localDexPath, data);
                    log.i("frida_helper.dex extracted to: " + localDexPath);
                }
            } catch (Throwable e) {
                log.e("write frida_helper.dex error: " + e);
            }

            // 3) Пробуем загрузить через DexClassLoader: сначала локальный путь, затем legacy /data/
            DexClassLoader dexClassLoader = null;
            try {
                dexClassLoader = new DexClassLoader(localDexPath, null, null, classLoader);
            } catch (Throwable e) {
                log.e("DexClassLoader(local) error: " + e);
            }
            if (dexClassLoader == null) {
                try {
                    dexClassLoader = new DexClassLoader("/data/frida_helper.dex", null, null, classLoader);
                } catch (Throwable e) {
                    log.e("DexClassLoader(/data) error: " + e);
                }
            }
            if (dexClassLoader == null) {
                log.e("no DexClassLoader available");
                return false;
            }
//            try {
//                FastJson = dexClassLoader.loadClass("com.alibaba.fastjson2.JSON");
//            } catch (Exception e) {
//                log.e("load com.alibaba.fastjson.JSON error: " + e);
//            }

//            if (FastJson == null) {
//                log.e("load FastJson error!");
//                return false;
//            }

            try { GsonJson = dexClassLoader.loadClass("com.fucker.gson.Gson"); }
            catch (Exception e) { log.e("load com.fucker.gson.Gson error: " + e); }
            if (GsonJson == null) {
                log.e("load GsonJson error!");
                return false;
            }
            try { FridaHelper = dexClassLoader.loadClass("com.frida.frida_helper"); }
            catch (Exception e) { log.e("load FridaHelper error!"); return false; }
            hasInjectFridaHelper = true;
            log.i("inject jar success!");
            return true;
        } catch (Exception e) {
            log.e("inject jar error!" + e);
            return false;
        }
    }
}
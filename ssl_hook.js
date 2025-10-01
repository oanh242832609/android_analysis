// SSL Key Logging Hook Script
// Usage: frida -U -f <target.package> -l /sdcard/ssl_hook.js --no-pause

console.log("[+] SSL Hook Script Starting...");

// Загружаем нативную библиотеку через System.loadLibrary (надёжнее чем жесткий путь)
Java.perform(function () {
    try {
        var System = Java.use('java.lang.System');
        System.loadLibrary.overload('java.lang.String').call(System, 'ssl2');
        console.log('[+] System.loadLibrary(\'ssl2\') OK');

        // Библиотека сама установит хуки SSL и начнет логирование ключей
        var ctx = Java.use('android.app.ActivityThread').currentApplication();
        var pkg = ctx ? ctx.getApplicationContext().getPackageName() : '<unknown>';
        console.log('[+] SSL key logging active for package: ' + pkg);
        console.log('[i] Keys path (device): /sdcard/Android/' + pkg + '/ssl.log');
    } catch (e) {
        console.log('[-] Failed to load ssl2 via System.loadLibrary: ' + e);
        try {
            var lib = Module.load('libssl2.so');
            console.log('[+] Fallback Module.load libssl2.so OK');
        } catch (ee) {
            console.log('[-] Fallback Module.load failed: ' + ee);
        }
    }
});

console.log("[+] SSL Hook Script Ready");

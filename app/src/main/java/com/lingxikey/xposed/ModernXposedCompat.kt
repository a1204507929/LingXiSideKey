package com.lingxikey.custom.xposed

import android.util.Log
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method

/** Runtime state shared by the LibXposed 102 entry point and hook helpers. */
internal object XposedRuntime {
    @Volatile
    var api: XposedInterface? = null

    fun bind(value: XposedInterface) {
        api = value
    }

    fun log(priority: Int, message: String, error: Throwable? = null) {
        val current = api
        if (current != null) {
            current.log(priority, "LingXiKeyCustom", message, error)
        } else {
            Log.println(priority, "LingXiKeyCustom", message)
            if (error != null) Log.e("LingXiKeyCustom", message, error)
        }
    }
}

/** Logger backed by the LibXposed 102 interface. */
internal object XposedBridge {
    fun log(message: String) = XposedRuntime.log(Log.INFO, message)
    fun log(error: Throwable) = XposedRuntime.log(Log.ERROR, error.message ?: error.javaClass.name, error)
}

/** Small Kotlin callback facade over the LibXposed 102 interceptor chain. */
abstract class Hooker {
    open fun beforeHookedMethod(param: HookParam) = Unit
    open fun afterHookedMethod(param: HookParam) = Unit

    class HookParam internal constructor(private val chain: XposedInterface.Chain) {
        val method: Executable get() = chain.executable
        val thisObject: Any get() = chain.thisObject!!
        val args: Array<Any?> = chain.args.toTypedArray()

        private var hasResult = false
        private var result: Any? = null

        fun setResult(value: Any?) {
            hasResult = true
            result = value
        }

        internal fun invoke(hook: Hooker): Any? {
            hook.beforeHookedMethod(this)
            if (!hasResult) result = chain.proceed()
            hook.afterHookedMethod(this)
            return result
        }
    }
}

/** Reflection lookup helpers; registration itself uses XposedInterface.hook. */
internal object XposedHelpers {
    fun findClass(name: String, classLoader: ClassLoader?): Class<*> = Class.forName(name, false, classLoader)

    fun findClassIfExists(name: String, classLoader: ClassLoader?): Class<*>? = try {
        findClass(name, classLoader)
    } catch (_: Throwable) {
        null
    }

    fun hookMethod(
        className: String,
        classLoader: ClassLoader?,
        methodName: String,
        vararg parameterTypesAndCallback: Any
    ) = hookMethod(findClass(className, classLoader), methodName, *parameterTypesAndCallback)

    fun hookMethod(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypesAndCallback: Any
    ) {
        val callback = parameterTypesAndCallback.last() as Hooker
        val parameterTypes = parameterTypesAndCallback.dropLast(1).map { it as Class<*> }.toTypedArray()
        val method = findMethod(clazz, methodName, parameterTypes)
        hook(method, callback)
    }

    fun hookAllMethods(clazz: Class<*>, methodName: String, callback: Hooker) {
        allMethods(clazz, methodName).forEach { hook(it, callback) }
    }

    fun hookAllConstructors(clazz: Class<*>, callback: Hooker) {
        clazz.declaredConstructors.forEach { hook(it, callback) }
    }

    fun getObjectField(instance: Any, name: String): Any = findField(instance.javaClass, name).get(instance) ?: Unit

    fun getIntField(instance: Any, name: String): Int = findField(instance.javaClass, name).getInt(instance)

    fun callMethod(instance: Any, name: String, vararg args: Any?): Any {
        val method = allMethods(instance.javaClass, name)
            .firstOrNull { it.parameterTypes.size == args.size && it.parameterTypes.zip(args).all { (type, arg) -> accepts(type, arg) } }
            ?: error("$name(${args.size}) not found on ${instance.javaClass.name}")
        method.isAccessible = true
        return method.invoke(instance, *args) ?: Unit
    }

    private fun hook(executable: Executable, callback: Hooker) {
        val api = XposedRuntime.api ?: error("Xposed API is not bound")
        api.hook(executable)
            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
            .intercept { chain -> Hooker.HookParam(chain).invoke(callback) }
    }

    private fun findMethod(clazz: Class<*>, name: String, parameterTypes: Array<Class<*>>): Method =
        allMethods(clazz, name).firstOrNull { it.parameterTypes.contentEquals(parameterTypes) }
            ?: error("$name not found on ${clazz.name}")

    private fun allMethods(clazz: Class<*>, name: String): List<Method> =
        generateSequence(clazz) { it.superclass }
            .flatMap { it.declaredMethods.asSequence() }
            .filter { it.name == name }
            .toList()

    private fun findField(clazz: Class<*>, name: String): Field =
        generateSequence(clazz) { it.superclass }
            .mapNotNull {
                try {
                    it.getDeclaredField(name)
                } catch (_: NoSuchFieldException) {
                    null
                }
            }
            .firstOrNull()
            ?.also { it.isAccessible = true }
            ?: error("$name not found on ${clazz.name}")

    private fun accepts(type: Class<*>, value: Any?): Boolean {
        if (value == null) return !type.isPrimitive
        if (!type.isPrimitive) return type.isInstance(value)
        return when (type) {
            Boolean::class.javaPrimitiveType -> value is Boolean
            Byte::class.javaPrimitiveType -> value is Byte
            Short::class.javaPrimitiveType -> value is Short
            Int::class.javaPrimitiveType -> value is Int
            Long::class.javaPrimitiveType -> value is Long
            Float::class.javaPrimitiveType -> value is Float
            Double::class.javaPrimitiveType -> value is Double
            Char::class.javaPrimitiveType -> value is Char
            else -> false
        }
    }
}

internal data class PackageHookParam(
    val packageName: String,
    val classLoader: ClassLoader
)

/**
 * Resolves a usable Context from inside system_server (or any zygote-forked process).
 *
 * `ActivityThread.systemMain()` must NOT be used: it performs a full system bootstrap and
 * always fails when the system has already started, which is exactly why earlier code logged
 * "resolve system context failed: null". Instead we read the already-initialised singletons.
 */
internal object ContextResolver {
    @Volatile
    private var cached: android.content.Context? = null

    fun systemContext(): android.content.Context? {
        cached?.let { return it }
        val resolved = attempt()
        if (resolved != null) cached = resolved
        return resolved
    }

    /**
     * Returns the Context of the *current application* (the process we are hooked into).
     * This is what registerReceiver/sendBroadcast must use: the system context belongs to
     * the "android" package and the framework rejects receivers registered with it
     * ("Given caller package android is not running in process ...").
     */
    fun applicationContext(): android.content.Context? {
        val threadClass = runCatching { Class.forName("android.app.ActivityThread") }.getOrNull()
            ?: return null
        runCatching {
            threadClass.getMethod("currentApplication").invoke(null) as? android.content.Context
        }.getOrNull()?.let { if (it.packageName != "android") return it }
        // Fall back to the activity thread's own application object (private field on some ROMs).
        val thread = runCatching {
            threadClass.getMethod("currentActivityThread").invoke(null)
        }.getOrNull() ?: return null
        runCatching {
            val field = threadClass.getDeclaredField("mInitialApplication").apply { isAccessible = true }
            field.get(thread) as? android.content.Context
        }.getOrNull()?.let { return it }
        return null
    }

    private fun attempt(): android.content.Context? {
        val threadClass = runCatching { Class.forName("android.app.ActivityThread") }.getOrNull()
            ?: return null
        // 1) currentApplication(): non-null once the app/system process is fully attached.
        runCatching {
            threadClass.getMethod("currentApplication").invoke(null) as? android.content.Context
        }.getOrNull()?.let { return it }
        // 2) currentActivityThread().getSystemContext(): system_server's own context.
        val thread = runCatching {
            threadClass.getMethod("currentActivityThread").invoke(null)
        }.getOrNull() ?: return null
        runCatching {
            threadClass.getMethod("getSystemContext").invoke(thread) as? android.content.Context
        }.getOrNull()?.let { return it }
        // 3) Reflect the private mSystemContext field as a last resort (some ROMs omit the getter).
        runCatching {
            val field = threadClass.getDeclaredField("mSystemContext").apply { isAccessible = true }
            field.get(thread) as? android.content.Context
        }.getOrNull()?.let { return it }
        return null
    }
}

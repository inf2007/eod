#include <jni.h>

// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("eod");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("eod")
//      }
//    }
#include <jni.h>
#include <string>

// for logging in native
#include <android/log.h>
#define TAG "IN NATIVE C++"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_singaporetech_eod_Splash_getNativeString(JNIEnv *env, jobject thiz) {
    // TODO: implement getNativeString()
    std::string secretStr = "THIS IS FROM C++";
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "created string = %s", secretStr.c_str());
    return env->NewStringUTF(secretStr.c_str());
}
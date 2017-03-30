#include <jni.h>
#include <loki.h>

JNIEXPORT jboolean JNICALL
Java_org_efidroid_efidroidmanager_patching_LokiPatcher_nativePatchImage(JNIEnv *env,
                                                                        jclass class,
                                                                        jstring imageType_,
                                                                        jstring aBootImage_,
                                                                        jstring in_, jstring out_) {
    const char *imageType = (*env)->GetStringUTFChars(env, imageType_, 0);
    const char *aBootImage = (*env)->GetStringUTFChars(env, aBootImage_, 0);
    const char *in = (*env)->GetStringUTFChars(env, in_, 0);
    const char *out = (*env)->GetStringUTFChars(env, out_, 0);

    // loki_patch() returns '0' on successful exit
    int result = loki_patch(imageType, aBootImage, in, out);

    (*env)->ReleaseStringUTFChars(env, imageType_, imageType);
    (*env)->ReleaseStringUTFChars(env, aBootImage_, aBootImage);
    (*env)->ReleaseStringUTFChars(env, in_, in);
    (*env)->ReleaseStringUTFChars(env, out_, out);

    return (jboolean)(result == 0);
}

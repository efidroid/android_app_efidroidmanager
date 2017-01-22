#include <jni.h>
#include <loki.h>

JNIEXPORT jint JNICALL
Java_org_efidroid_efidroidmanager_LokiTool_lokiPatch(JNIEnv *env, jobject instance,
                                                     jstring partitionLabel_, jstring abootImage_,
                                                     jstring inImage_, jstring outImage_) {
    const char *partitionLabel = (*env)->GetStringUTFChars(env, partitionLabel_, 0);
    const char *abootImage = (*env)->GetStringUTFChars(env, abootImage_, 0);
    const char *inImage = (*env)->GetStringUTFChars(env, inImage_, 0);
    const char *outImage = (*env)->GetStringUTFChars(env, outImage_, 0);

    int ret = loki_patch(partitionLabel,abootImage,inImage,outImage);

    (*env)->ReleaseStringUTFChars(env, partitionLabel_, partitionLabel);
    (*env)->ReleaseStringUTFChars(env, abootImage_, abootImage);
    (*env)->ReleaseStringUTFChars(env, inImage_, inImage);
    (*env)->ReleaseStringUTFChars(env, outImage_, outImage);

    return ret;
}

JNIEXPORT jint JNICALL
Java_org_efidroid_efidroidmanager_LokiTool_lokiFlash(JNIEnv *env, jobject instance,
                                                     jstring partitionLabel_, jstring image_) {
    const char *partitionLabel = (*env)->GetStringUTFChars(env, partitionLabel_, 0);
    const char *image = (*env)->GetStringUTFChars(env, image_, 0);

    int ret = loki_flash(partitionLabel, image);

    (*env)->ReleaseStringUTFChars(env, partitionLabel_, partitionLabel);
    (*env)->ReleaseStringUTFChars(env, image_, image);

    return ret;
}

/*
An ultrafast Bruker BAF to MzML converter
Copyright (C) 2020 Jonathan Bisson

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

#include <jni.h>
#include <string.h>
#include <stdlib.h>

static const char *JNIT_CLASS = "net/nprod/baf2mzml/BAF2SQL";

int baf2sql_get_sqlite_cache_filename_v2(const char *, unsigned int, const char*, int);
u_int64_t baf2sql_array_open_storage(int, const char *);
void baf2sql_array_close_storage(u_int64_t);
int baf2sql_get_last_error_string(char *, int);
void baf2sql_set_num_threads(u_int32_t);
int baf2sql_array_get_num_elements(u_int64_t, u_int64_t, u_int64_t *);
int baf2sql_array_read_double(u_int64_t, u_int64_t, double []);
int baf2sql_array_read_float(u_int64_t, u_int64_t, float []);
int baf2sql_array_read_uint32(u_int64_t, u_int64_t, u_int32_t []);

static jstring c_baf2sql_get_sqlite_cache_filename(JNIEnv *env, jobject obj, jstring bafFile) {
    const char *bafFileString;
    char *sqliteFileString;
    int bafStringLength;
    int result;
    sqliteFileString = malloc(2048);
    bafFileString = (*env)->GetStringUTFChars(env, bafFile, 0);

    result = baf2sql_get_sqlite_cache_filename_v2(0, 0, bafFileString, 0);
    result = baf2sql_get_sqlite_cache_filename_v2(sqliteFileString, result, bafFileString, 0);

    return (*env)->NewStringUTF(env, sqliteFileString);
}

static jlong c_baf2sql_array_open_storage_calibrated(JNIEnv *env, jobject obj, jstring bafFile) {
    const char *bafFileString;
    bafFileString = (*env)->GetStringUTFChars(env, bafFile, 0);

    return baf2sql_array_open_storage(0, bafFileString);
}

static jint c_baf2sql_array_close_storage(JNIEnv *env, jobject obj, jlong storage) {
    baf2sql_array_close_storage(storage);
    return 0;
}

static jstring c_baf2sql_get_last_error_string(JNIEnv *env, jobject obj) {
    char *errorMessage;
    errorMessage = malloc(2048);
    baf2sql_get_last_error_string(errorMessage, 2048);
    return (*env)->NewStringUTF(env, errorMessage);
}

static void c_baf2sql_set_num_threads(JNIEnv *env, jobject obj, jint threads) {
    baf2sql_set_num_threads(threads);
}

static jobject c_baf2sql_array_get_num_elements(JNIEnv *env, jobject obj, jlong handle, jlong id) {
    printf("Trying");
    jlong num_elements;
    int ret;

    ret = baf2sql_array_get_num_elements(handle, id, &num_elements);

    jclass numElements = (*env)->FindClass(env, "net/nprod/baf2mzml/NumElements");
    jmethodID constructor = (*env)->GetMethodID(env, numElements, "<init>", "(IJ)V");
    jobject instance = (*env)->NewObject(env, numElements, constructor, ret, num_elements);
    return instance;
}

static jobject c_baf2sql_read_double_array(JNIEnv *env, jobject obj, jlong handle, jlong id) {
    jlong num_elements;
    double *array;
    int ret;

    ret = baf2sql_array_get_num_elements(handle, id, &num_elements);

    array = malloc(num_elements*sizeof(double));
    jdoubleArray outputArray = (*env)->NewDoubleArray(env, num_elements);
    baf2sql_array_read_double(handle, id, array);
    (*env)->SetDoubleArrayRegion(env, outputArray, 0, num_elements, array);
    free(array);
    jclass doubleArray = (*env)->FindClass(env, "net/nprod/baf2mzml/BAFDoubleArray");
    jmethodID constructor = (*env)->GetMethodID(env, doubleArray, "<init>", "(IJ[D)V");

    jobject instance = (*env)->NewObject(env, doubleArray, constructor, ret, num_elements, outputArray);
    return instance;

}

static JNINativeMethod funcs[] = {
    { "c_baf2sql_get_sqlite_cache_filename", "(Ljava/lang/String;)Ljava/lang/String;", (void *)&c_baf2sql_get_sqlite_cache_filename },
    { "c_baf2sql_array_open_storage_calibrated", "(Ljava/lang/String;)J", (void *)&c_baf2sql_array_open_storage_calibrated },
    { "c_baf2sql_array_close_storage", "(J)I", (void *)&c_baf2sql_array_close_storage},
    { "c_baf2sql_get_last_error_string", "()Ljava/lang/String;", (void *)&c_baf2sql_get_last_error_string},
    { "c_baf2sql_set_num_threads", "(I)V", (void *)&c_baf2sql_set_num_threads},
    { "c_baf2sql_array_get_num_elements", "(JJ)Lnet/nprod/baf2mzml/NumElements;", (void *)&c_baf2sql_array_get_num_elements},
    { "c_baf2sql_read_double_array", "(JJ)Lnet/nprod/baf2mzml/BAFDoubleArray;", (void *)&c_baf2sql_read_double_array}
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv *env;
    jclass  cls;
    jint    res;

    (void)reserved;

    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK)
        return -1;

    cls = (*env)->FindClass(env, JNIT_CLASS);
    if (cls == NULL)
        return -1;

    res = (*env)->RegisterNatives(env, cls, funcs, sizeof(funcs)/sizeof(*funcs));
    if (res != 0)
        return -1;

    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    JNIEnv *env;
    jclass  cls;

    (void)reserved;

    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK)
        return;

    cls = (*env)->FindClass(env, JNIT_CLASS);
    if (cls == NULL)
        return;
    (*env)->UnregisterNatives(env, cls);
}

#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_myapplication_MainActivity_preprocessFrame(JNIEnv *env, jobject, jbyteArray frameData, jint width, jint height) {
    // Convert the input frame data to a cv::Mat
    jbyte *data = env->GetByteArrayElements(frameData, nullptr);
    Mat yuvImage(height + height / 2, width, CV_8UC1, data);

    // Convert YUV to grayscale
    Mat grayImage;
    cvtColor(yuvImage, grayImage, COLOR_YUV2GRAY_NV21);

    // Apply Gaussian blur to reduce noise before edge detection
    Mat blurredImage;
    GaussianBlur(grayImage, blurredImage, Size(5, 5), 1.0);

    // Apply Canny edge detection
    Mat edges;
    Canny(blurredImage, edges, 40, 120);

    // Convert the result back to a byte array
    jbyteArray result = env->NewByteArray(edges.total());
    env->SetByteArrayRegion(result, 0, edges.total(), reinterpret_cast<jbyte *>(edges.data));

    // Release resources
    env->ReleaseByteArrayElements(frameData, data, 0);
    return result;
}
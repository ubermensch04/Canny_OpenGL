# Update app/src/main/cpp/CMakeLists.txt to app/src/main/jni/CMakeLists.txt
cmake_minimum_required(VERSION 3.22.1)
project("myapplication")

# Path to OpenCV SDK
set(OpenCV_DIR ../sdk/native/jni)
find_package(OpenCV REQUIRED)

# Define our library
add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        ${OpenCV_LIBS}
        android
        log)
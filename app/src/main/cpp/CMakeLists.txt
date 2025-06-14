# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.

cmake_minimum_required(VERSION 3.22.1)
project("myapplication")

# ----------------- ADD THIS SECTION FOR OPENCV -----------------
# 1. SET THE PATH TO YOUR OPENCV SDK
# The path is relative to THIS file (CMakeLists.txt in the cpp folder).
set(OpenCV_DIR ../sdk/native/jni)

# 2. FIND THE OPENCV PACKAGE
# This command uses the path you just set to find the necessary configuration files.
find_package(OpenCV REQUIRED)
# ----------------------------------------------------------------

add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        # ----------------- ADD THIS LINE FOR OPENCV -----------------
        # 3. LINK OPENCV LIBRARIES TO YOUR PROJECT
        ${OpenCV_LIBS}
        # ----------------------------------------------------------------
        android
        log)

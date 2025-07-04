# Canny_OpenGL: Real-time Edge Detection with OpenCV and OpenGL

## Features Implemented

### Core Requirements ✅

1. **📸 Camera Feed Integration**
   - Integrated CameraX API for efficient camera frame capture
   - Configured image analyzer to extract YUV frames in real-time

2. **🔁 Frame Processing via OpenCV (C++)**
   - Implemented JNI bridge to process frames in native C++ code
   - Applied Canny Edge Detection using OpenCV's efficient algorithms
   - Gaussian blur preprocessing to improve edge detection quality

### Bonus Features ✅

1. **🔄 Toggle View**
   - Added FAB button to switch between raw camera feed and processed edge view
   - Properly handles visibility and camera pipeline reconfiguration

2. **📊 Performance Monitoring**
   - Real-time FPS counter displayed on screen
   - Optimized processing with dedicated high-priority threads

3. **🎨 Visual Effects with OpenGL Shaders**
   - Normal mode: white edges on black background
   - Invert mode: black edges on white background

4. **🔍 OpenCV Processing**
   - Gaussian blur applied before edge detection to reduce noise
   - Optimal Canny threshold parameters for clear edge visualization

## Screenshots of Working App
![Image](https://github.com/user-attachments/assets/347ae81c-aae1-4a6c-9de8-aab8c3cd04bf)
![Image](https://github.com/user-attachments/assets/f39c92a2-95c0-4242-a543-0e62b60e9752)



## Setup Instructions

### Prerequisites

1. **Android Studio**: Arctic Fox or newer
2. **Android SDK**: API level 24+
3. **Android NDK**: r24+ (required for native C++ code)
4. **OpenCV for Android SDK**: version 4.5.0+

### NDK Setup

Download and Install NDK (Side by side) and CMake from SDK tools tab in Android Studio

### OpenCV Setup

1. Download OpenCV Android SDK from [OpenCV Releases](https://opencv.org/releases/)
2. Extract the ZIP file
3. Copy the `sdk` folder to `app/src/main` directory
4. Ensure the OpenCV path in `app/src/main/cpp/CMakeLists.txt` is correctly set:
   ```cmake
   set(OpenCV_DIR ../sdk/native/jni)
   ```

### Building the Project

1. Clone this repository:
   ```
   git clone https://github.com/ubermensch04/Canny_OpenGL.git
   ```
2. Open the project in Android Studio
3. Wait for Gradle to sync and build
4. Run the app on a physical device (camera required)

## Architecture

### JNI Integration

The application uses JNI (Java Native Interface) to bridge between the Kotlin/Java layer and C++ code:

1. **Native Method Declaration**: `external fun preprocessFrame(data: ByteArray, width: Int, height: Int): ByteArray` in MainActivity
2. **Native Implementation**: C++ function in `native-lib.cpp` using the JNI naming convention
3. **Library Loading**: `System.loadLibrary("myapplication")` loads the compiled C++ library

### Frame Flow (Data Flow)
![Image](https://github.com/user-attachments/assets/6cb2ab0d-8ae5-4c9f-8050-d432f554df9d)

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



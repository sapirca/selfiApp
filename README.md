
The se(l)fi app 
================


The se(l)fi app allows Sefi, a man paralyzed from the neck down, 
to remote control (WiFi) a Sony Camera, as well as controling a robotic arm responsible for the camera movement.

The repository contains an android and arduino projects. 
- The Arduino controls two servos which operate the movement of the camera.
- The Andorid app is responsible for the communication with the Arduino and the camera using Sony's Remote API.
NOTE: The communication between the Android and Arduino is achieved using an open source library (usbSerialForAndroid - https://github.com/mik3y/usb-serial-for-android).

This is a pre-alpha release which is currently still under heavy development.
You may contact me for further questions, comments, and help at sapir.caduri@gmail.com




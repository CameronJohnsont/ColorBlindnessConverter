# ColorBlindnessConverter
An android app that takes a picture and converts the picture to what a colorblind person would see. I wanted to create this app because a close 
friend of mine is colorblind and the idea of seeing what he sees intrigued me. The app uses a camera that is attached to a TextureView and a “take 
picture” button to capture the original image. The TextureView is then converted to a bitmap in order to edit the image. The 
convertPixel_Protanopia() method is used to bit shift the integer value to get the individual RGB values. Once the individual values are found, they 
are converted to protanopia, a color blindness to red, and then returned. When the take picture button was pressed, the visibility of the button and 
TextureView were set to invisible. The new protanopia converted image and “back to camera” button are set to visible. The picture taken is now 
displayed as if you were color blind. If the “back to camera” button is pressed, you are taken back to the camera and free to repeat the process.  I 
used the Android developer page to create the TextureView and camera capabilities. This app is not only a fun way to see what my friend sees but 
could also be used by medical professionals to have a better understanding of protanopia color blindness. Other forms of color blindness could easily 
be added to this app by changing the algorithm for changing the RGB values.

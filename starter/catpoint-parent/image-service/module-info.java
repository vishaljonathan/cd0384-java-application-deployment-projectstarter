module image.service {
    requires software.amazon.awssdk.rekognition;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.regions;
    requires webcam.capture;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.utils;


    exports com.udacity.catpoint.image;
}

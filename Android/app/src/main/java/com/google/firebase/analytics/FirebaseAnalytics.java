package com.google.firebase.analytics;

import android.os.Bundle;

public class FirebaseAnalytics {
    public static FirebaseAnalytics analytics;
    public static FirebaseAnalytics getInstance(Object o) {
        if (analytics == null)
            analytics = new FirebaseAnalytics();
        return analytics;
    }
    public void logEvent(String s, Bundle bundle) {

    }
}

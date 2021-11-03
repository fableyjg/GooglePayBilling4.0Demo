package com.hg.idlefarm;

public class Constants {
    // Use the fake local server data or real remote server.
    public static boolean USE_FAKE_SERVER = false;

    public static final String BASIC_SKU = "basic_subscription";
    public static final String PREMIUM_SKU = "premium_subscription";
    public static final String PLAY_STORE_SUBSCRIPTION_URL
            = "https://play.google.com/store/account/subscriptions";
    public static final String PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL
            = "https://play.google.com/store/account/subscriptions?sku=%s&package=%s";
}

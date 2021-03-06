package com.hg.idlefarm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        installReferrer();
        Log.i(TAG, "onCreate: before init");
        initPurchase();
        findViewById(R.id.btn_init).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConnect();
            }
        });

        findViewById(R.id.btn_startpurchase).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPurchase();
            }
        });

        findViewById(R.id.btn_querypurchase).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: ????????????????????????");
                queryPurchases();
            }
        });
    }


    InstallReferrerClient referrerClient;
    private void installReferrer(){
        referrerClient = InstallReferrerClient.newBuilder(this).build();
        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                switch (responseCode) {
                    case InstallReferrerClient.InstallReferrerResponse.OK:
                        // Connection established.
                        Log.i(TAG, "onInstallReferrerSetupFinished: ok");
                        getInstallReferrer();
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        // API not available on the current Play Store app.
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        // Connection couldn't be established.
                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    private void getInstallReferrer(){
        ReferrerDetails response = null;
        try {
            response = referrerClient.getInstallReferrer();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        String referrerUrl = response.getInstallReferrer();
        long referrerClickTime = response.getReferrerClickTimestampSeconds();
        long appInstallTime = response.getInstallBeginTimestampSeconds();
        boolean instantExperienceLaunched = response.getGooglePlayInstantParam();

        Log.i(TAG, "getInstallReferrer: referrerUrl???" + referrerUrl + " referrerClickTime: " +referrerClickTime + " appInstallTime: "+appInstallTime+ " instantExperienceLaunched:"+ instantExperienceLaunched);
    }



    /*
    ???2021???11???1??????????????????????????????????????????????????????????????????3???????????????
    ????????????
    https://developer.android.com/google/play/billing/integrate?hl=zh-cn
    1.???????????????????????????????????????
    2.???????????????????????????????????????????????????
    3.??????????????????????????????????????????
    4.??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????

    ??????????????????????????????????????????????????????????????????????????????
    ????????????????????????????????????????????????
    ??????????????????????????????????????????????????????????????????????????????
    ??????????????????????????????????????????????????????????????????????????????Google?????????????????????
    ?????????????????????????????????????????????????????????????????????Google?????????????????????
    ???????????????????????????????????????????????????????????????????????????
    ??????????????????????????????????????????????????????????????????????????????????????????
    ???????????????
     */

    // step1:?????????BillingClient
    private void initPurchase(){
        billingClient = BillingClient.newBuilder(MainActivity.this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
    }

    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
            Log.i(TAG, "onPurchasesUpdated: ");
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list !=null){
                Log.i(TAG, "onPurchasesUpdated: ok");

//                for (Purchase purchase : list) {
//                    handlePurchase(purchase);
//                    //?????? ID?????????????????????????????????
//                    secondVerify(purchase.getPurchaseToken(), productId);
//                }
            }else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Handle an error caused by a user cancelling the purchase flow.
                Log.i(TAG, "onPurchasesUpdated: ??????");
            } else {
                // Handle any other error codes.
                Log.i(TAG, "onPurchasesUpdated: ??????????????????");
            }
        }
    };

    private BillingClient billingClient;
    private boolean isServiceConnected = false;

    // step2:???Google Play????????????
    private void startConnect(){
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                Log.i(TAG, "onBillingServiceDisconnected: ??????????????????????????????");
                isServiceConnected = false;
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                Log.i(TAG, "onBillingSetupFinished: ");
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                    Log.i(TAG, "onBillingSetupFinished: ????????????????????????????????????????????????????????????");

                    //????????????????????????????????????SkuType?????????SkuType.INAPP(?????????????????????)???SkuType.SUBS(????????????)
                    isServiceConnected = true;

                    //step 3:?????????????????????????????????
                    queryPurchases();

                    querySku();
                }
            }
        });
    }

    private static final List<String> LIST_OF_SKUS = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add("farm.subscription");
//                add("1200diamond");
//                add("2500diamond");
//                add("500diamond");
            }});


    /**
     * SkuDetails for all known SKUs.
     */
    public MutableLiveData<Map<String, SkuDetails>> skusWithSkuDetails = new MutableLiveData<>();

    //??????????????????????????????????????????
    List<SkuDetails> mSkuDetails;

    //ProductId ??????ID???
    String productId = "";

    SkuDetails skuDetails;
    // ??????SKU???????????????
    //step 3:??????LIST_OF_SKUS???????????????
    //INAPP ???????????????  SUBS ????????????
    private void querySku(){
//        List<String> skuList = new ArrayList<>();
//        skuList.add("");
//        skuList.add("");

        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
//        params.setSkusList(LIST_OF_SKUS).setType(BillingClient.SkuType.INAPP);
        params.setSkusList(LIST_OF_SKUS).setType(BillingClient.SkuType.SUBS);
        billingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> skuDetailsList) {
                // ?????????????????????
//                skuDetails = list;

                if (billingResult == null) {
                    Log.wtf(TAG, "onSkuDetailsResponse: null BillingResult");
                    return;
                }

                Log.i(TAG, "onSkuDetailsResponse: skuDetailsList " + skuDetailsList.size());
                //????????????????????????????????????
                mSkuDetails = skuDetailsList;

                int responseCode = billingResult.getResponseCode();
                String debugMessage = billingResult.getDebugMessage();
                switch (responseCode) {
                    case BillingClient.BillingResponseCode.OK:
                        Log.i(TAG, "onSkuDetailsResponse ok : " + responseCode + " " + debugMessage);
                        final int expectedSkuDetailsCount = LIST_OF_SKUS.size();
                        if (skuDetailsList == null) {
                            skusWithSkuDetails.postValue(Collections.<String, SkuDetails>emptyMap());
                            Log.e(TAG, "onSkuDetailsResponse: " +
                                    "Expected " + expectedSkuDetailsCount + ", " +
                                    "Found null SkuDetails. " +
                                    "Check to see if the SKUs you requested are correctly published " +
                                    "in the Google Play Console.");
                        } else {
                            Map<String, SkuDetails> newSkusDetailList = new HashMap<String, SkuDetails>();
                            for (SkuDetails skuDetails : skuDetailsList) {
                                newSkusDetailList.put(skuDetails.getSku(), skuDetails);
                            }
                            skusWithSkuDetails.postValue(newSkusDetailList);
                            int skuDetailsCount = newSkusDetailList.size();
                            if (skuDetailsCount == expectedSkuDetailsCount) {
                                Log.i(TAG, "onSkuDetailsResponse: Found " + skuDetailsCount + " SkuDetails");
                            } else {
                                Log.e(TAG, "onSkuDetailsResponse: " +
                                        "Expected " + expectedSkuDetailsCount + ", " +
                                        "Found " + skuDetailsCount + " SkuDetails. " +
                                        "Check to see if the SKUs you requested are correctly published " +
                                        "in the Google Play Console.");
                            }
                        }
                        break;
                    case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                    case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                    case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                    case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                    case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                    case BillingClient.BillingResponseCode.ERROR:
                        Log.e(TAG, "onSkuDetailsResponse:  error " + responseCode + " " + debugMessage);
                        break;
                    case BillingClient.BillingResponseCode.USER_CANCELED:
                        Log.i(TAG, "onSkuDetailsResponse: user cancel " + responseCode + " " + debugMessage);
                        break;
                    // These response codes are not expected.
                    case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                    case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                    case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
                    default:
                        Log.wtf(TAG, "onSkuDetailsResponse: owned " + responseCode + " " + debugMessage);
                }

            }
        });
    }

    /**
     * Query Google Play Billing for existing purchases.
     * <p>
     * New purchases will be provided to the PurchasesUpdatedListener.
     * You still need to check the Google Play Billing API to know when purchase tokens are removed.
     */
    // step 3.2 ??????????????????????????????????????????
    public void queryPurchases() {
        if (!billingClient.isReady()) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready");
        }
        Log.d(TAG, "queryPurchases: SUBS");
        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                // TODO: 2021/10/26 ???????????????????????????????????????
//                 processPurchases(list);

                Log.i(TAG, "onQueryPurchasesResponse: " + billingResult.getResponseCode() + " debugmessage:" + billingResult.getDebugMessage() + " list:"+ list.size());
                for (Purchase purchase : list){
                    Log.i(TAG, "handlePurchase: ??????????????????  " + purchase.isAutoRenewing());
                    handlePurchase(purchase);
                }
            }
        });
    }

    //step4,??????????????????
    private void startPurchase(){
//        Activity activity;
//        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
//                .setSkuDetails(skuDetails)
//                .build();
//
//        int responseCode = billingClient.launchBillingFlow(MainActivity.this,billingFlowParams).getResponseCode();

        productId = "recommend3";

        //??????????????????????????????????????????????????????????????????;
        Log.i(TAG, "onSkuDetailsResponse: 222222222222222222222222");
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
//                .setSkuDetails(getSkuDetailByID(productId))
                .setSkuDetails(mSkuDetails.get(0))
                .build();
        //????????????OK????????????????????????
        launchBillingFlow(MainActivity.this, billingFlowParams);

    }

    public SkuDetails getSkuDetailByID(String itemId){
        Log.i(TAG, "getSkuDetailByID: " + itemId);
        if(!itemId.isEmpty() && mSkuDetails!=null){
            for (SkuDetails skuDetail : mSkuDetails){
                if(itemId.equals(skuDetail.getSku())){

                    Log.i(TAG, "getSkuDetailByID: ");
                    return skuDetail;
                }
            }
        }
        return null;
    }

    /**
     * Step 5???????????????????????????????????????????????????
     * <p>
     * Launching the UI to make a purchase requires a reference to the Activity.
     */
    public int launchBillingFlow(Activity activity, BillingFlowParams params) {
//        String sku = params.getSku();
//        String oldSku = params.getOldSku();
//        Log.i(TAG, "launchBillingFlow: sku: " + sku + ", oldSku: " + oldSku);
        if (!billingClient.isReady()) {
            Log.e(TAG, "launchBillingFlow: BillingClient is not ready");
        }

        //????????????OK??????????????????
        BillingResult billingResult = billingClient.launchBillingFlow(activity, params);
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "launchBillingFlow: BillingResponse " + responseCode + " " + debugMessage);
        return responseCode;
    }

    //?????????????????????
    void handlePurchase(Purchase purchase){

        Log.i(TAG, "handlePurchase: " + purchase.getSkus() + " id:" + purchase.getOrderId() + " token:" + purchase.getPurchaseToken());
        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
        ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String s) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                    Log.i(TAG, "onConsumeResponse: ");
                }
            }
        };
        billingClient.consumeAsync(consumeParams,listener);
    }
}


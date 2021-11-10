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
                Log.i(TAG, "onClick: 主动查询订阅状态");
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

        Log.i(TAG, "getInstallReferrer: referrerUrl：" + referrerUrl + " referrerClickTime: " +referrerClickTime + " appInstallTime: "+appInstallTime+ " instantExperienceLaunched:"+ instantExperienceLaunched);
    }



    /*
    从2021年11月1日起，现有应用所有新版本都必须使用结算库版本3或更高版本
    订阅流程
    https://developer.android.com/google/play/billing/integrate?hl=zh-cn
    1.向用户展示他们可以购买什么
    2.启动购买流程，以便用户接受购买交易
    3.在开发者服务器上验证购买交易
    4.先用户提供内容，并确认内容已传送给用户，还可以选择性地将商品标记为已消费，以便用户可以再次购买商品。

    订阅会自动续订，直到被取消，订阅可处于下面这几种状态
    有效：信誉良好，可以享受订阅内容
    已取消：用户已取消订阅，但在到期前仍可以享受订阅内容
    处于宽限期：用户遇到付款出问题，但仍可享用订阅内容，Google会重新尝试扣款
    暂时保留：用户遇到付款问题，不再享受订阅内容；Google会重新尝试扣款
    已暂停：用户暂停了订阅，在恢复之前不能享受订阅内容
    已到期：用户已取消且不能再享受订阅内容。用户在订阅到期时会被
    视为流失。
     */

    // step1:初始化BillingClient
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
//                    //订单 ID，提交到服务端二次验证
//                    secondVerify(purchase.getPurchaseToken(), productId);
//                }
            }else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Handle an error caused by a user cancelling the purchase flow.
                Log.i(TAG, "onPurchasesUpdated: 取消");
            } else {
                // Handle any other error codes.
                Log.i(TAG, "onPurchasesUpdated: 其他异常退出");
            }
        }
    };

    private BillingClient billingClient;
    private boolean isServiceConnected = false;

    // step2:与Google Play建立连接
    private void startConnect(){
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                Log.i(TAG, "onBillingServiceDisconnected: 尝试重新连接谷歌服务");
                isServiceConnected = false;
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                Log.i(TAG, "onBillingSetupFinished: ");
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                    Log.i(TAG, "onBillingSetupFinished: 与谷歌服务建立了连接，开始查询购买的内容");

                    //通过查询商品；展示商品，SkuType可以是SkuType.INAPP(一次性购买商品)，SkuType.SUBS(针对订阅)
                    isServiceConnected = true;

                    //step 3:查询当前可供购买的商品
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

    //查询信息时，存储所有商品信息
    List<SkuDetails> mSkuDetails;

    //ProductId 商品ID，
    String productId = "";

    SkuDetails skuDetails;
    // 查找SKU的详细信息
    //step 3:查询LIST_OF_SKUS商品的信息
    //INAPP 一次性商品  SUBS 订阅商品
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
                // 处理查询的结果
//                skuDetails = list;

                if (billingResult == null) {
                    Log.wtf(TAG, "onSkuDetailsResponse: null BillingResult");
                    return;
                }

                Log.i(TAG, "onSkuDetailsResponse: skuDetailsList " + skuDetailsList.size());
                //保存了购买列表的商品信息
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
    // step 3.2 查询之前是否有没有消耗的产品
    public void queryPurchases() {
        if (!billingClient.isReady()) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready");
        }
        Log.d(TAG, "queryPurchases: SUBS");
        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                // TODO: 2021/10/26 将已经购买的内购，发送出去
//                 processPurchases(list);

                Log.i(TAG, "onQueryPurchasesResponse: " + billingResult.getResponseCode() + " debugmessage:" + billingResult.getDebugMessage() + " list:"+ list.size());
                for (Purchase purchase : list){
                    Log.i(TAG, "handlePurchase: 订阅状态查询  " + purchase.isAutoRenewing());
                    handlePurchase(purchase);
                }
            }
        });
    }

    //step4,启动购买流程
    private void startPurchase(){
//        Activity activity;
//        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
//                .setSkuDetails(skuDetails)
//                .build();
//
//        int responseCode = billingClient.launchBillingFlow(MainActivity.this,billingFlowParams).getResponseCode();

        productId = "recommend3";

        //展示官方商品页面，当用户点击对应按钮时，触发;
        Log.i(TAG, "onSkuDetailsResponse: 222222222222222222222222");
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
//                .setSkuDetails(getSkuDetailByID(productId))
                .setSkuDetails(mSkuDetails.get(0))
                .build();
        //返回码为OK时，表示成功启动
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
     * Step 5：弹出购买具体内购的界面，谷歌决定
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

        //返回码为OK标识成功启动
        BillingResult billingResult = billingClient.launchBillingFlow(activity, params);
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "launchBillingFlow: BillingResponse " + responseCode + " " + debugMessage);
        return responseCode;
    }

    //处理一次性商品
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


package com.solutions.snap.anuncio;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;


//implementa service connection para compras in app
public class MainActivity extends AppCompatActivity implements ServiceConnection, AdapterView.OnItemClickListener{

     AdView mAdView;
     Button btnFullscreenAd;
     String deviceId;

    //anuncio full screen
    private InterstitialAd interstitialAd;

    //comprar inapp
    IInAppBillingService mService;

    //lista de produtos
    private ListView listaprodutos;

    //lista produtos retornados.
    ArrayList<String> listaProdutos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //compra in app
        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);


        mAdView = (AdView) findViewById(R.id.adView);

        listaprodutos = (ListView) findViewById(R.id.lstprodutos);
        listaprodutos.setOnItemClickListener(this);

        btnFullscreenAd = (Button) findViewById(R.id.btnfull);
        btnFullscreenAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //obter produtos
                try {
                    //produtos a procurar
                    ArrayList<String> skuList = new ArrayList<String> ();
                    skuList.add("teste");
                    skuList.add("queijadinha");
                    Bundle querySkus = new Bundle();
                    querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

                    Bundle skuDetails = mService.getSkuDetails(3,
                            getPackageName(), "inapp", querySkus);

                    listaProdutos = new ArrayList<String>();


                    int response = skuDetails.getInt("RESPONSE_CODE");

                    if (response == 0) {

                        //responsa ok
                        Toast.makeText(getApplication(), String.valueOf(response), Toast.LENGTH_SHORT).show();

                        ArrayList<String> responseList
                                = skuDetails.getStringArrayList("DETAILS_LIST");

                        for (String thisResponse : responseList) {
                            JSONObject object = new JSONObject(thisResponse);
                            String sku = object.getString("productId");
                            String price = object.getString("price");

                            listaProdutos.add(sku);

                        }
                    }

                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, listaProdutos);

                        listaprodutos.setAdapter(adapter);


                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }



        });


        //test device id
      //  String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
       // deviceId = md5(android_id).toUpperCase();


        //admob test
      //  AdRequest adRequest = new AdRequest.Builder()
          //      .addTestDevice(deviceId)
            //    .build();

        //admob producao
        AdRequest adRequest = new AdRequest.Builder()
               .build();
        mAdView.loadAd(adRequest);


        //anuncio full screen
        loadInterstitialAd();
    }


    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }

        //destroir servico de compra
        if (mService != null) {
            unbindService(this);
        }
        super.onDestroy();
    }


    //obter id do dispositivo por md5, utilizado para testes no admod
    public static final String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
           // Logger.logStackTrace(TAG,e);
        }
        return "";
    }


    private void loadInterstitialAd() {
        interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(getResources().getString(R.string.admob_interstitial_ad));


        //ad producao
        AdRequest adRequest = new AdRequest.Builder()
                .build();

        interstitialAd.loadAd(adRequest);
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                interstitialAd.show();
            }
        });
    }


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mService = IInAppBillingService.Stub.asInterface(iBinder);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mService = null;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {


        //retorna id do produto selecionado
     String idproduto = listaProdutos.get(i);


        //iniciando a compra
        try {
            Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                    idproduto, "inapp", "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");


            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");

            try {
                startIntentSenderForResult(pendingIntent.getIntentSender(),
                        1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                        Integer.valueOf(0));
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }



        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }


    //result da intent de compra
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    Toast.makeText(this, "You have bought the " + sku + ". Excellent choice, adventurer!" , Toast.LENGTH_SHORT).show();
                }
                catch (JSONException e) {
                    Toast.makeText(this, "Failed to parse purchase data." , Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
    }
}

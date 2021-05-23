package net.intensecorp.meeteazy.api;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://fcm.googleapis.com/fcm/";
    private static Retrofit mRetrofit = null;

    public static Retrofit getClient() {
        if (mRetrofit == null) {
            mRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }

        return mRetrofit;
    }
}

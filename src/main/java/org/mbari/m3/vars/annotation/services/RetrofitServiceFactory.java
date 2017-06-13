package org.mbari.m3.vars.annotation.services;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Base interface for generating services from Retrofit interfaces.
 *
 * @author Brian Schlining
 * @since 2017-05-26T11:00:00
 */
public abstract class RetrofitServiceFactory {

    private final Retrofit.Builder retrofitBuilder;
    private final Logger log = LoggerFactory.getLogger(getClass());


    public RetrofitServiceFactory(String endpoint) {
        String correctedEndpoint = (endpoint.endsWith("/")) ? endpoint : endpoint + "/";

        retrofitBuilder  = new Retrofit.Builder()
                .baseUrl(correctedEndpoint)
                .addConverterFactory(GsonConverterFactory.create(getGson()));
    }

    public <S> S create(Class<S> clazz) {
        return create(clazz, null);
    }


    public <S> S create(Class<S> clazz, AuthService auth) {

        HttpLoggingInterceptor logger = new HttpLoggingInterceptor(log::debug);
        logger.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .addInterceptor(logger);

        if (auth != null) {
            httpClient.addInterceptor(new BasicJWTAuthInterceptor(auth));
        }
        retrofitBuilder.client(httpClient.build());

        return retrofitBuilder.build()
                .create(clazz);
    }

    public abstract Gson getGson();

}

package com.chatproject.LetsChat.Fragments;

import com.chatproject.LetsChat.Notifications.MyResponse;
import com.chatproject.LetsChat.Notifications.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAAiP5cExA:APA91bHE15MYjNiYepA4PK4_fMCziHWxfeIMSMwSUCftx4CuUfrf_sQTI-kKuBCn0sWakGzRhcaGZq3tSfXGnlf4VJZ594OEyviEfeT-Jk6Vdvb4fhMshe-KsUPbT-bV5LCb09MRG48u"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}

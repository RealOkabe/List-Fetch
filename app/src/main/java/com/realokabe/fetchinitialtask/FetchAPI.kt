package com.realokabe.fetchinitialtask

import retrofit2.Response
import retrofit2.http.GET

interface FetchAPI {
    @GET("hiring.json")
    suspend fun getFetchData(): Response<Array<fetchedData>>
}
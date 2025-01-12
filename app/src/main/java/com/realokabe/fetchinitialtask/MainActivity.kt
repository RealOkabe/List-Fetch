package com.realokabe.fetchinitialtask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.realokabe.fetchinitialtask.ui.theme.FetchInitialTaskTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FetchInitialTaskTheme {
                GetDataAndShow()
            }
        }
    }
}

// Function to get the data and process it in the IO thread.
@OptIn(DelicateCoroutinesApi::class)
suspend fun getFetchData(): MutableMap<Int, List<FetchedData>> {
    return withContext(Dispatchers.IO) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://fetch-hiring.s3.amazonaws.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val fetchedData: FetchAPI = retrofit.create(FetchAPI::class.java)
        val result = fetchedData.getFetchData()
        // Using mutable list to remove empty and null names
        val nameList: MutableList<FetchedData> = result.body()!!.toMutableList()
        nameList.removeAll { (it.name == "" || it.name == null) }
        // Grouping by listId and sorting
        val groupedNameList: MutableMap<Int, List<FetchedData>> =
            nameList.groupBy { it.listId }.toSortedMap()
        // Sorting by name
        for (i in groupedNameList.keys) {
            groupedNameList[i] = groupedNameList[i]!!.sortedBy { it.name }
        }
        return@withContext groupedNameList
    }
}

// The UI maker. Code from many different places, not sure about the design, but it is functional.
@Composable
fun ShowList(dataMap: MutableMap<Int, List<FetchedData>>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .padding(16.dp)
    ) {
        // Added a spacer on top because the list started from top of the screen.
        item{ Spacer(modifier = Modifier.height(32.dp)) }
        dataMap.forEach {
            (listId, dataList) ->
            item {
                Text(
                    text = "List ID: $listId",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(dataList) { fetchedData ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(text = "Name: ${fetchedData.name}", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Id: ${fetchedData.id}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.secondary)
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// The function that gets the data and shows it
@Composable
fun GetDataAndShow() {
    // State that holds the Mutable Map
    val dataMap = remember { mutableStateMapOf<Int, List<FetchedData>>() }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch the data and update the Map variable
    LaunchedEffect(Unit) {
        isLoading = true
        val fetchedData = getFetchData()
        dataMap.clear()
        dataMap.putAll(fetchedData)
        isLoading = false
    }

    // Show a loading icon and then the List
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    else {
        ShowList(dataMap)
    }
}
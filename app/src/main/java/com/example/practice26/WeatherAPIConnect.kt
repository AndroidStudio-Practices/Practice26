import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

suspend fun getWeatherData(city: String): String = withContext(Dispatchers.IO) {
    val apiKey = "8cb819ad90c6174a17ed320d707d3dcf"
    val urlString = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric&lang=en"

    if(city.isBlank())
        return@withContext "Please enter city name"

    try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val responseCode = connection.responseCode

        return@withContext when (responseCode) {
            HttpURLConnection.HTTP_OK -> {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseWeatherJson(response)
            }
            HttpURLConnection.HTTP_NOT_FOUND -> {
                // Код 404 - город не найден
                "City '$city' not found"
            }
            HttpURLConnection.HTTP_UNAUTHORIZED -> {
                // Код 401 - неверный API ключ
                "Error: Incorrect API key"
            }
            else -> {
                // Читаем сообщение об ошибке из ответа
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                if (!errorResponse.isNullOrEmpty()) {
                    parseErrorJson(errorResponse)
                } else {
                    "Connection error: HTTP $responseCode"
                }
            }
        }
    } catch (e: Exception) {
        "Network Error: ${e.message}"
    }
}

private fun parseWeatherJson(json: String): String {
    return try {
        val city = json.substringAfter("\"name\":\"").substringBefore("\"")
        val description = json.substringAfter("\"description\":\"").substringBefore("\"")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val temp = json.substringAfter("\"temp\":").substringBefore(",")

        "Weather in $city\n$description, ${temp}°C"
    } catch (e: Exception) {
        "Data parsing error"
    }
}

private fun parseErrorJson(json: String): String {
    return try {
        // Парсим сообщение об ошибке от API
        val message = json.substringAfter("\"message\":\"").substringBefore("\"")
        "API error: $message"
    } catch (e: Exception) {
        "Unknown API error: $e"
    }
}
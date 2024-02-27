import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.stage.Stage
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import tornadofx.*
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

/**
 * The main entry point of the EarthquakeApp application.
 *
 * @param args The command-line arguments.
 */
fun main(args: Array<String>) = launch<EarthquakeApp>(args)

/**
 * The main application class that extends the TornadoFX App class.
 * It represents the entry point of the EarthquakeApp application.
 */
class EarthquakeApp : App(MainView::class) {
    /**
     * Configures the primary stage of the application and sets up the initial view.
     *
     * @param stage The primary stage of the application.
     */
    override fun start(stage: Stage) {
        stage.width = 1000.0
        stage.height = 600.0
        stage.setOnCloseRequest {
            Platform.exit()
            exitProcess(0)
        }
        super.start(stage)
    }
}

/**
 * The main view of the EarthquakeApp application.
 * It extends the TornadoFX View class.
 */
class MainView : View("EarthquakeApp") {
    private val tableController: TableController by inject()
    private val datePicker = datepicker()

    /**
     * The root UI element of the view.
     */
    override val root = vbox {
        hbox {
            form {
                fieldset {
                    field("Start date") {
                        add(datePicker)
                    }
                }
            }

            hbox {
                alignment = Pos.CENTER
                spacing = 10.0

               button("Search") {
                    action {
                        try {
                            print("checking")
                            val date = datePicker.value
                            if (date != null) {
                                tableController.searchByDate(date)
                            } else {
                                tableController.tableStatus.value = "Please select a date."
                            }
                        } catch (e: Exception) {
                            println("Error occurred: ${e.message}")
                            tableController.earthquakeData.clear()
                            tableController.tableStatus.value = "Invalid date format. Please enter a valid date."
                        }
                    }
                }

                button("Search today") {
                    action {
                        tableController.searchToday()
                    }
                    alignment = Pos.CENTER
                }


                label{
                    alignment = Pos.BOTTOM_RIGHT
                    padding = insets(10)
                }
                label().bind(tableController.tableStatus)
            }
        }

        tableview(tableController.earthquakeData)
        {
            readonlyColumn("Magnitude", Properties::mag)
            readonlyColumn("Location", Properties::place).remainingWidth()
            readonlyColumn("Time (UTC)", Properties::time).apply {
                cellFormat {
                    text = LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC).toString()
                }
                remainingWidth()
            }
            readonlyColumn("Type", Properties::type).remainingWidth()
            readonlyColumn("Title", Properties::title).remainingWidth()
            smartResize()
        }
        hbox {
            alignment = Pos.BOTTOM_RIGHT
            spacing = 10.0
            padding = insets(20)

            label("Earthquake Count: ")
            label().bind(tableController.earthquakeCount)
        }
    }
}

/**
 * The controller class for the EarthquakeApp application.
 * It extends the TornadoFX Controller class.
 */
class TableController : Controller() {
    private val baseUrl = "https://earthquake.usgs.gov/fdsnws/event/1/%s?format=geojson"
    val earthquakeData = mutableListOf<Properties>().asObservable()
    val earthquakeCount = SimpleIntegerProperty()
    val tableStatus = SimpleStringProperty()

    private var continuousUpdateTask: Task<Unit> = emptyTask()
    private var currentDate: LocalDate = LocalDate.now()

    /**
     * Initializes the controller and loads the earthquake data.
     */
    init {
        loadEarthquakeData()
    }

    private fun runTask() {
        runAsync {
            getEarthQuakesCount(
                "$baseUrl&starttime=${formatDate(currentDate)}&endtime=${formatDate(currentDate.plusDays(1))}"
            )
        } ui { count ->
            earthquakeCount.value = count.count
        } fail {
            tableStatus.value = "Calling API failed"
        }

        runAsync {
            println("updating table")
            getEarthQuakes(
                "$baseUrl&starttime=${formatDate(currentDate)}&endtime=${formatDate(currentDate.plusDays(1))}"
            )
        } ui { featureCollection ->
            val earthquakes = featureCollection.features.map { it.properties }
            earthquakeData.setAll(earthquakes)
            tableStatus.value = ""
        } fail {
            tableStatus.value = "Calling API failed"
        }
    }

    private fun loadEarthquakeData() {
        continuousUpdateTask = task {
            while (true) {
                runTask()
                Thread.sleep(5000) // Wait for 5 seconds before fetching data again
            }
        } cancel {
            println("Cancelled")
            tableStatus.value = ""
        }
    }

    /**
     * Fetches the earthquake count from the API.
     *
     * @param url The URL for the API endpoint.
     * @return The count of earthquakes.
     */
    private fun getEarthQuakesCount(url: String): EarthQuakesCount {
        val jsonString = URL(String.format(url, "count")).readText()
        return Json.decodeFromString(jsonString)
    }

    /**
     * Fetches the earthquake data from the API.
     *
     * @param url The URL for the API endpoint.
     * @return The collection of earthquake events.
     */
    private fun getEarthQuakes(url: String): EarthquakeCollection {
        val jsonString = URL(String.format(url, "query")).readText()
        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromString(jsonString)
    }

    /**
     * Searches for earthquakes that occurred today.
     * Fetches the earthquake data for the current date and updates the table.
     */
    fun searchToday() {
        currentDate = LocalDate.now()
        tableStatus.value = "loading"
        runTask()
    }

    /**
     * Searches for earthquakes that occurred on a specific date.
     *
     * @param date The date to search for earthquakes.
     */
    fun searchByDate(date: LocalDate) {
        currentDate = date
        tableStatus.value = "loading"
        runTask()
    }

    private fun formatDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_DATE)
    }

    private fun emptyTask(): Task<Unit> = task {} //empty Task as placeholder
}

/*
Programming with Kotlin,
Computer Science, Bern University of Applied Sciences
*/
/**
 * Count of Earthquakes
 *
 * @property count - count
 * @property maxAllowed - maximum count allowed
 */
@Serializable
data class EarthQuakesCount(
    val count: Int, val maxAllowed: Int
)

/**
 * Properties of an earthquake events
 *
 * @property mag - Magnitude
 * @property place - place
 * @property time
 * @property type
 * @property title
 * @constructor Create empty Properties
 */
@Serializable
data class Properties(
    val mag: Double, val place: String?, val time: Long, val type: String, val title: String
)

/**
 * Earthquake - describes one earthquake event
 *
 * @property type = "Feature"
 * @property properties
 * @constructor Create empty Feature
 */
@Serializable
data class Earthquake(
    val type: String, val properties: Properties
)

/**
 * Earthquake collection - Collection of earthquake events
 *
 * @property type = "EarthquakeCollection"
 * @property features - Array of earthquake events
 * @constructor Create empty Feature collection
 */
@Serializable
data class EarthquakeCollection(
    val type: String, val features: Array<Earthquake>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EarthquakeCollection

        if (type != other.type) return false
        if (!features.contentEquals(other.features)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + features.contentHashCode()
        return result
    }
}

case class DateSet(
    availability: Array[String])

case class SeriesItem(
    departureTime: Integer,
    arrivalTime: Integer,
    arrivalDayOffset: Integer,
    dateSets: Array[DateSet])
    
case class Flight(
    marketingFlightNumber: Integer,
    series: Array[SeriesItem])
    
case class Coordinate(
    latitude: Float,
    longitude: Float)
    
case class Airport(
    code: String,
    countryId: String,
    cityId: String,
    coordinates: Coordinate)
    
case class Route(
    origin: Airport,
    destination: Airport,
    flights: Array[Flight])

case class FlatDate(
    origin: Airport,
    destination: Airport,
    flightNumber: Integer,
    departureTime: Integer,
    arrivalTime: Integer,
    arrivalDayOffset: Integer,
    date: String)

val s3Bucket = "skyscanner-sandbox-timetable-data"
val path = "timetable/master/"
val version = "01699e9967d3001a221c3625803a83ca2312d0b0d6f2f9d3dd30c9dcb11ee31a"
val fileName = "part-00000.gz"

val filePath = "/" + path + version + "/" + fileName

val routesFilePath = "s3://" + s3Bucket + filePath

val rawRDD = sc.textFile(routesFilePath)
import com.google.gson.Gson
val routes = rawRDD.map(line => new Gson().fromJson(line, classOf[Route]))

val flatDates = routes.flatMap(route => route.flights
                      .flatMap(flight => flight.series
                      .flatMap(series => series.dateSets
                      .flatMap(dateSet => dateSet.availability
                      .map(date => FlatDate(
                          route.origin,
                          route.destination,
                          flight.marketingFlightNumber,
                          series.departureTime,
                          series.arrivalTime,
                          series.arrivalDayOffset,
                          date))))))
                          
val disticntFlatDates = flatDates.distinct()
val groupedDates = disticntFlatDates.groupBy(flatDate => flatDate.flightNumber + "-" + flatDate.date + "-" + flatDate.departureTime)

println("Number of routes: " + routes.count)
println("Number of dates: " + flatDates.count)
println("Number of distinct dates: " + disticntFlatDates.count)
println("Number of grouped dates: " + groupedDates.count)
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

case class Version(
    version: String,
    date: String)

case class FlatDate(
    origin: Airport,
    destination: Airport,
    flightNumber: Integer,
    departureTime: Integer,
    arrivalTime: Integer,
    arrivalDayOffset: Integer,
    date: String,
    version: Version)

val versions = Array(
    Version("01699e9967d3001a221c3625803a83ca2312d0b0d6f2f9d3dd30c9dcb11ee31a", "2018-01-20"),
    Version("01ef031155084cb30aeb3a68b6bbceff31d7424bd5c124306e2e5974a7bfd31d", "2018-01-10"),
    Version("0b3619bc1e823a84f4dc67745e0f3ed09b00a38613b4d5ffb218334100f49b9e", "2018-01-25"),
)

val s3Bucket = "skyscanner-sandbox-timetable-data"
val path = "timetable/master/"
val fileName = "part-00000.gz"

import com.google.gson.Gson

val flatDatesArray = versions.map(version => {
    val routesFilePath = "s3://skyscanner-sandbox-timetable-data/timetable/master/" +  version.version + "/part-00000.gz"
    println(routesFilePath)
    val rawRDD = sc.textFile(routesFilePath)
    val routes = rawRDD.map(line => new Gson().fromJson(line, classOf[Route]))
    routes.flatMap(route => route.flights
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
               date,
               version))))))
    })

val version0FlatDates = flatDatesArray(0).count
val version1FlatDates = flatDatesArray(1).count
val version2FlatDates = flatDatesArray(2).count

val flatDates = flatDatesArray(0)
                .union(flatDatesArray(1))
                .union(flatDatesArray(2))

val totalFlatDates = flatDates.count

val firstAppearence = (x: FlatDate, y: FlatDate) => {
    if (x.version.date < y.version.date) {
        x
    } else {
        y
    }
}

val dates = flatDates
        .groupBy(date => date.origin.code + "-" + date.destination.code + "-" + date.flightNumber + "-" + date.departureTime + "-" + 
                         date.arrivalTime + "-" + date.arrivalDayOffset + "-" + date.date)
        .mapValues(value => value.reduce(firstAppearence))
        .map(_._2)

val totalDates = dates.count

val mapRDD = dates.map(date => {
    val versionDate = date.version.date
    val route = date.origin.code + "-" + date.destination.code
    Map("vdate" -> versionDate, "route" -> route, "date" -> date.date)
})

val columns = mapRDD.take(1).flatMap(a => a.keys)

val df = mapRDD.map(value => {
                    val list = value.values.toList
                    (list(0), list(1), list(2))
               })
               .toDF(columns:_*)

df.registerTempTable("df")

%sql
SELECT *
FROM df


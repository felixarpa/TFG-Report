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
    Version("189f1031fae26812886532efae09fc13ee42f4e13d91cbcef07d572bde810eaa", "2018-02-07"),
    Version("24837e980c4ed4e0d90ab7eada4d299d9b848366888e789d8f78f70508bdbf38", "2018-02-09"),
    Version("33019e84ed8df516da656d22c3af5a89dfb154b9f389a175c7abccd064fcb354", "2018-02-04"),
    Version("7bdaa742ca73c636a29f6aae501f989e8d81da980419dc65d432cd30d5f63418", "2018-02-02"),
    Version("87874a39fd7a94cb3a1449bb65f74d2b29220be175da1cbc4d3ede008d54e60a", "2018-02-12"),
    Version("96f587e3f7f391061eed7fa802449bf6aaa210cf64e353cadfd4de1a1f445ca5", "2018-02-05"),
    Version("ba4605dc73f3ffa0d303dc2f35d5479ee69b9d3f87ac718300bf066a02415c74", "2018-02-11"),
    Version("cb2a5e056ab3a08fb1c59fb5faab98f49207cf79fdfbbbc3a3be065f26422d1a", "2018-02-06"),
    Version("dee91369af5161c351d57dba9b8ed9be1b63c20573b5e793cb821ccd6e0adc93", "2018-02-01"),
    Version("e591ca8f3240dcee0033a044d6e5d9db21ac58c633df00df51f6a1e0b4849b27", "2018-02-10"),
    Version("ef02a69973070ee621205aa0f4b77b54c0eee153a2ed7787697eb86aaf3d257f", "2018-02-03"),
    Version("f90b661948ba490172ac330992ca0c3e65e88c53ceb10b17ef320fecbe8f8e46", "2018-02-08"),
    Version("37772580f56e68fa64291d9b598934991d5cc8c4b5209502304e9e9c57ef0f4d", "2018-01-31"),
    Version("76715c8c168dc2473ce42dc76a6639e1d02e993877c3b8a05a7e057461f53843", "2018-01-30"),
    Version("e9d78ba402a96fcb4600a5615e93d9189d6f004ee858ecf0f2234d54dedd00e5", "2018-01-30"),
    Version("01699e9967d3001a221c3625803a83ca2312d0b0d6f2f9d3dd30c9dcb11ee31a", "2018-01-20"),
    Version("0b3619bc1e823a84f4dc67745e0f3ed09b00a38613b4d5ffb218334100f49b9e", "2018-01-25"),
    Version("1050751c1a4d8e01f842339e1e3c74266b1158d91a4505aabb339d0eb6493b68", "2018-01-24"),
    Version("28eb6d6afb29c42111a450254473889e6c9b7f27b3b869ff99383af58aa86226", "2018-01-27"),
    Version("3372554080e543af925db54e3b8794e76db98fef354b40c258167601e464f448", "2018-01-21"),
    Version("459d78226a3b319ad2186e08c151871ec0a4ee74c41db849c70672c47a72f7bf", "2018-01-23"),
    Version("50f149e1877ab9cc24bc4f14f777c3e3697896ec79d706669ce97510f01137b6", "2018-01-21"),
    Version("5ebc6956a282880c5c060a4e784e4b0ca61c38afb84b71b2ea004de889b7603c", "2018-01-25"),
    Version("61e48340759182595cb3a0a1212d82876edf562b679dd0f6b7472536f0a19a7e", "2018-01-26"),
    Version("6b726b1cdf96ebae27b8004b083fdc2ea8c43a4e511946044588be89aa8209aa", "2018-01-22"),
    Version("7f94124aebecb0e03a8de7632e8ca95c81a7709e38eafedf50526982c271eb90", "2018-01-20"),
    Version("834065193c5abc3b56988624f480b89a8f9d2726a85e39b03b521ec9b2b07af7", "2018-01-29"),
    Version("98983edd68900b9b9fafb81a25139ac7a25b01f0a6bfca6d53867375d3a8ea7b", "2018-01-22"),
    Version("a103e82ef95047ac91ebd642d3c8781df448fc592d22293d841b44db7ff4680e", "2018-01-28"),
    Version("f86cd0d92d9d258de1ccec7250facb33ee00d85e7c290eb552dca88e31050993", "2018-01-26"),
    Version("ff8691f0dfab9a345cb0587de6564a97327d578aa2316750cdb42de1a2dacfba", "2018-01-25"))

import com.google.gson.Gson

val filterOrigin = "JFK"

val flatDatesArray = versions.map(version => {
    val routesFilePath = "s3://skyscanner-sandbox-timetable-data/timetable/master/" +  version.version + "/part-00000.gz"
    val rawRDD = sc.textFile(routesFilePath)
    val routes = rawRDD.map(line => new Gson().fromJson(line, classOf[Route]))
    routes.filter(route => route.origin.code == filterOrigin && route.origin.countryId == route.destination.countryId)
          .flatMap(route => route.flights
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

val flatDates = sc.union(flatDatesArray)

case class FlatTrip(
    route: String,
    flightNumber: Integer,
    departureTime: Integer,
    arrivalTime: Integer,
    arrivalDayOffset: Integer,
    date: Long,
    versionDate: Long)

val format = new java.text.SimpleDateFormat("yyyy-MM-dd")

val flatTrips = flatDates.map(flatDate => {
    val route = flatDate.origin.code + "-" + flatDate.destination.code
    val date = format.parse(flatDate.date).getTime()
    val vDate = format.parse(flatDate.version.date).getTime()
    FlatTrip(
        route,
        flatDate.flightNumber,
        flatDate.departureTime,
        flatDate.arrivalTime,
        flatDate.arrivalDayOffset,
        date,
        vDate)
})

val totalFlatTrips = flatTrips.count

val tripsMap  =    flatTrips.map(t => Map("route" -> t.route, "flightDate" -> t.date.toString, "version" -> t.versionDate.toString))

val reducedTrips = flatTrips.map(t => (t.route + t.flightNumber + t.date + t.departureTime, t))
                            .reduceByKey((a, b) => if (a.versionDate < b.versionDate) a else b)
                            .map(_._2)

val tripsMapR = reducedTrips.map(t => Map("route" -> t.route, "flightDate" -> t.date.toString, "version" -> t.versionDate.toString))

val totalReducedTrips = reducedTrips.count
val columns = tripsMap.take(1).flatMap(a => a.keys)
//val columns = Array("route", "flightDate", "version", "val")

val dfR = tripsMapR.map(value => (value("route"), value("flightDate").toLong, value("version").toLong)).toDF(columns:_*)

dfR.registerTempTable("dfR")

val df = tripsMap.map(value => (value("route"), value("flightDate").toLong, value("version").toLong)).toDF(columns:_*)

df.registerTempTable("df")

%sql
SELECT *
FROM df
WHERE flightDate > 1551000000000 AND flightDate < 1552000000000 AND route = "JFK-SFO"

%sql
SELECT *
FROM dfR
WHERE flightDate > 1551000000000 AND flightDate < 1552000000000 AND route = "JFK-SFO"
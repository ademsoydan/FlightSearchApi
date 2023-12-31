package com.amadeus.FlightSearchApi.Service;

import com.amadeus.FlightSearchApi.Entity.Airport;
import com.amadeus.FlightSearchApi.Entity.Flight;
import com.amadeus.FlightSearchApi.Exception.AirportNotFoundException;
import com.amadeus.FlightSearchApi.Exception.FlightNotFoundException;
import com.amadeus.FlightSearchApi.Repository.AirportRepository;
import com.amadeus.FlightSearchApi.Repository.FlightRepository;
import com.amadeus.FlightSearchApi.Request.FlightRequest;
import com.amadeus.FlightSearchApi.Response.AirportResponse;
import com.amadeus.FlightSearchApi.Response.FlightResponse;
import com.amadeus.FlightSearchApi.Response.SearchFlightResponse;
import com.amadeus.FlightSearchApi.Util.MockAPI;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.amadeus.FlightSearchApi.Util.MockAPI.generateMockFlightInfoList;

@Service
public class FlightService {
    @Autowired
    private FlightRepository flightRepository;
    @Autowired
    private AirportRepository airportRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * ?")
    public void fetchFlightInfoAndSave() {
       List<FlightRequest> flightRequests = MockAPI.generateMockFlightInfoList(5);
       flightRequests.stream().forEach(this::createFlight);
    }





    public FlightResponse createFlight(FlightRequest request){
       Optional<Airport> departureAirport =  airportRepository.findById(request.getDepartureAirportId());
       Optional<Airport> arrivalAirport =  airportRepository.findById(request.getArrivalAirportId());
       if(departureAirport.isEmpty() || arrivalAirport.isEmpty()){
           throw new AirportNotFoundException("Departure or arrival airport not found");
       }

       Flight createdFlight =  Flight.builder()
               .departureAirport(departureAirport.get())
               .arrivalAirport(arrivalAirport.get())
               .arrivalTime(request.getArrivalTime())
               .departureTime(request.getDepartureTime())
               .price(request.getPrice())
               .build();

       Flight createdFligth =  flightRepository.save(createdFlight);
       return new FlightResponse(createdFligth);
    }

    public List<FlightResponse> getAllFlights(){
       return flightRepository.findAll().stream().map(FlightResponse::new).collect(Collectors.toList());
    }

    public FlightResponse getFlightById(int id){
       Optional<Flight> dbFlight =  flightRepository.findById(id);
       if(dbFlight.isEmpty())
           throw new FlightNotFoundException("The Flight is not found by the id: " + id);
       return new FlightResponse(dbFlight.get());
    }

    public FlightResponse updateFlight(int flightId, FlightRequest request) {
        Optional<Flight> dbFlight =  flightRepository.findById(flightId);
        Optional<Airport> dbArrivalAirport = airportRepository.findById(request.getArrivalAirportId());
        Optional<Airport> dbDepartureAirport = airportRepository.findById(request.getDepartureAirportId());

        if(dbFlight.isEmpty())
            throw new FlightNotFoundException("The Flight is not found by the id: " + flightId);
        if(dbArrivalAirport.isEmpty())
            throw new AirportNotFoundException("The Arrival airport is no found by the id: " + request.getArrivalAirportId());
        if(dbDepartureAirport.isEmpty())
            throw new AirportNotFoundException("The Departure airport is no found by the id: " + request.getArrivalAirportId());

        Flight flight = dbFlight.get();
        flight.setPrice(request.getPrice());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setArrivalAirport(dbArrivalAirport.get());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setDepartureAirport(dbDepartureAirport.get());

        flightRepository.save(flight);

        return new FlightResponse(flight);
    }

    public FlightResponse deleteFlightById(int flightId){
        Optional<Flight> dbFlight =  flightRepository.findById(flightId);
        if(dbFlight.isEmpty())
            throw new FlightNotFoundException("The Flight want to be deleted is not found by the id: " + flightId);
        flightRepository.deleteById(flightId);
        return new FlightResponse(dbFlight.get());
    }

    public SearchFlightResponse searchFlights(String departureCity, String arrivalCity,
                                              LocalDate departureDate, Optional<LocalDate> returnDate) {
        departureCity = departureCity.toUpperCase();
        arrivalCity = arrivalCity.toUpperCase();
        SearchFlightResponse response = new SearchFlightResponse();

        List<Flight> departureFlights = getFlights(departureDate,departureCity,arrivalCity);
        response.setDepartureFlights(departureFlights.stream().map(FlightResponse::new).collect(Collectors.toList()));

        if (returnDate.isPresent()) {
            List<Flight> returnFlights = getFlights(returnDate.get(),arrivalCity,departureCity);
            response.setReturnFlights(returnFlights.stream().map(FlightResponse::new).collect(Collectors.toList()));
        }
        return response;
    }

    private List<Flight> getFlights(LocalDate date, String departureCity, String arrivalCity){
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<Flight> flights = flightRepository.findByDepartureAirportAndArrivalAirportAndDepartureTime(
                departureCity, arrivalCity, startOfDay, endOfDay);

        return flights;
    }

}


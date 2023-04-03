package com.markobozic.parkinglots.service;

import com.markobozic.parkinglots.repository.entity.ParkingLotEntity;
import com.markobozic.parkinglots.repository.LoaderRepository;
import com.markobozic.parkinglots.mappers.MapperUtils;
import com.markobozic.parkinglots.service.model.ParkingLotDetails;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
public class LoaderServiceImpl implements LoaderService {

    private static final Logger LOG = LoggerFactory.getLogger(LoaderServiceImpl.class);

    private final NamedParameterJdbcOperations jdbcTemplate;
    private final LoaderRepository loaderRepository;

    public LoaderServiceImpl(final NamedParameterJdbcOperations jdbcTemplate, final LoaderRepository loaderRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.loaderRepository = loaderRepository;
    }

    @Override
    @Transactional
    public String addRecords() {
        List<UUID> recordsExists = loaderRepository.findOne(jdbcTemplate);
        if (!recordsExists.isEmpty()) {
            return "Records are already stored in database.";
        }

        List<ParkingLotDetails> parkingLots = getParkingLotsFromCVS();
        if (parkingLots.isEmpty()) {
            return "FAIL";
        }

        List<ParkingLotEntity> lotEntities = parkingLots.stream().map(MapperUtils::fromModelToEntity).collect(Collectors.toList());
        loaderRepository.saveAll(lotEntities);

        return "SUCCESS! Added " + lotEntities.size() + " records in database.";
    }

    private List<ParkingLotDetails> getParkingLotsFromCVS() {
        CSVReader csvReader;
        try {
            File initialFile = new File("src/main/resources/la-parking-lot.csv");
            InputStream input = new FileInputStream(initialFile);

            Reader reader = new InputStreamReader(input);
            csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();

        } catch (IOException e) {
            LOG.error("Something went wrong in reading CSV file! Error: {}", e.getMessage());
            return Collections.emptyList();
        }

        List<ParkingLotDetails> parkingLots = new ArrayList<>();
        try {
            String[] nextLine;
            while ((nextLine = csvReader.readNext()) != null) {
                parkingLots.add(new ParkingLotDetails(nextLine));
            }
        } catch (IOException | CsvValidationException e) {
            LOG.error("Something went wrong in reading line from CSV file. Error: {}", e.getMessage());
            return Collections.emptyList();
        }

        return parkingLots;
    }
}

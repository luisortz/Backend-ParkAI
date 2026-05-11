package com.parkai.backend.config;

import com.parkai.backend.model.Zone;
import com.parkai.backend.repository.ZoneRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final ZoneRepository zoneRepository;

    public DataLoader(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    @Override
    public void run(String... args) {

        if (zoneRepository.count() == 0) {

            Zone palermo = new Zone();
            palermo.setName("Palermo");
            palermo.setLatitude(-34.5883);
            palermo.setLongitude(-58.4300);

            Zone recoleta = new Zone();
            recoleta.setName("Recoleta");
            recoleta.setLatitude(-34.5875);
            recoleta.setLongitude(-58.3974);

            zoneRepository.save(palermo);
            zoneRepository.save(recoleta);

            System.out.println("Zones loaded successfully");
        }
    }
}
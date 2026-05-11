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

            Zone AltoPalermo = new Zone();
            AltoPalermo.setName("Alto Palermo");
            AltoPalermo.setLatitude(-34.5884018);
            AltoPalermo.setLongitude(-58.4133122);

            Zone SantafeYArmenia = new Zone();
            SantafeYArmenia.setName("Santa fe y Armenia");
            SantafeYArmenia.setLatitude(-34.5835748);
            SantafeYArmenia.setLongitude(-58.4208238);

            zoneRepository.save(AltoPalermo);
            zoneRepository.save(SantafeYArmenia);

            System.out.println("Zones loaded successfully");
        }
    }
}
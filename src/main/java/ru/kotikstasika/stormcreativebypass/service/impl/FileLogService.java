package ru.kotikstasika.stormcreativebypass.service.impl;

import ru.kotikstasika.stormcreativebypass.StormCreativeBypass;
import ru.kotikstasika.stormcreativebypass.service.ILogService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileLogService implements ILogService {

    private final SimpleDateFormat fileDateFormat = new SimpleDateFormat("dd.MM.yyyy");

    @Override
    public void logToFile(String message) {
        try {
            String date = fileDateFormat.format(new Date());
            File logFile = new File(StormCreativeBypass.getInstance().getDataFolder(), "logs/" + date + ".txt");

            if (!logFile.getParentFile().exists()) {
                logFile.getParentFile().mkdirs();
            }

            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(message + "\n");
            }
        } catch (IOException e) {
            StormCreativeBypass.getInstance().getLogger().warning("Ошибка записи в файл лога: " + e.getMessage());
        }
    }
}


package ru.kotikstasika.stormcreativebypass.service;

public interface ITelegramService {
    void sendMessage(String message);
    void addToPendingMessages(String message);
    void processPendingMessages();
    boolean sendTestMessage();
}



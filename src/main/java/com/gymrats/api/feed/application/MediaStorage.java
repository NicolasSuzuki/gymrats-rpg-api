package com.gymrats.api.feed.application;

public interface MediaStorage {
  void write(String key, byte[] bytes, String contentType);
  byte[] read(String key);
  void delete(String key);
}

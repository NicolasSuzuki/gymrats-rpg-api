package com.gymrats.api.shared.application;

public class DomainConflictException extends RuntimeException {
  public DomainConflictException(String message) { super(message); }
}

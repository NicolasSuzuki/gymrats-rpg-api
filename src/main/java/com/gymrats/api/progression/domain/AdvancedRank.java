package com.gymrats.api.progression.domain;

public enum AdvancedRank {
  A("A"),
  S5("S-5"),
  S4("S-4"),
  S3("S-3"),
  S2("S-2"),
  S1("S-1"),
  NATIONAL("Nacional");

  private final String displayName;

  AdvancedRank(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return displayName;
  }
}

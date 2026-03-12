package com.revature.vault.service.backup.importers;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ImporterFactory {

  private final Map<String, Importer> importerMap = new HashMap<>();

  public ImporterFactory(List<Importer> importers) {
    for (Importer importer : importers) {
      importerMap.put(importer.getSupportedSource().toUpperCase(), importer);
    }
  }

  public Importer getImporter(String source) {
    Importer importer = importerMap.get(source.toUpperCase());
    if (importer == null) {
      throw new IllegalArgumentException("Unsupported import source: " + source);
    }
    return importer;
  }

  public List<String> getSupportedFormats() {
    return new java.util.ArrayList<>(importerMap.keySet());
  }
}



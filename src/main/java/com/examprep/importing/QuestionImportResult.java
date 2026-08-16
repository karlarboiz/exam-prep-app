package com.examprep.importing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestionImportResult {

    private int importedCount;
    private int updatedCount;
    private final List<String> errors = new ArrayList<>();

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public void addError(String error) {
        errors.add(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}

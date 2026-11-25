package com.example.stockanalysis.backdata.validation.api;

import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationResult;

public interface BackdataValidationEngine {

    ValidationResult validate(ValidationCommand command);
}

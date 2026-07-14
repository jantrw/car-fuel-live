package io.github.jantrw.carfuellive.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link TrimmedSize}.
 *
 * <p>It treats {@code null} and blank values as the responsibility of other annotations such as
 * {@code @NotBlank}; this validator only enforces the trimmed length window.
 */
public class TrimmedSizeValidator implements ConstraintValidator<TrimmedSize, String> {

  private int min;
  private int max;

  @Override
  public void initialize(TrimmedSize constraintAnnotation) {
    min = constraintAnnotation.min();
    max = constraintAnnotation.max();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    final String trimmedValue = value.trim();
    if (trimmedValue.isEmpty()) {
      return true;
    }

    final int trimmedLength = trimmedValue.length();
    return trimmedLength >= min && trimmedLength <= max;
  }
}

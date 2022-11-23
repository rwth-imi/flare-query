package de.rwth.imi.flare.requestor;

public class IncorrectQueryInputException extends Exception{
  public IncorrectQueryInputException(String errorMessage) {
    super(errorMessage);
  }
}

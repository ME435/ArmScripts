package edu.rosehulman.armscripts;

public class ConsoleMessage {

  private String mCommandString;
  private boolean mIsAndroidToAccessory;
  
  public ConsoleMessage(String commandString, boolean isAndroidToAccessory) {
    this.mCommandString = commandString;
    this.mIsAndroidToAccessory = isAndroidToAccessory;
  }
  public String getCommandString() {
    return mCommandString;
  }
  public void setCommandString(String commandString) {
    this.mCommandString = commandString;
  }
  public boolean isAndroidToAccessory() {
    return mIsAndroidToAccessory;
  }
  public void setIsAndroidToAccessory(boolean isAndroidToAccessory) {
    this.mIsAndroidToAccessory = isAndroidToAccessory;
  }
  @Override
  public String toString() {
    return mCommandString;
  }
  
  
}

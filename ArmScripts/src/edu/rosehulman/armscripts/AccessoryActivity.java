package edu.rosehulman.armscripts;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

public class AccessoryActivity extends Activity {

  private static final String TAG = AccessoryActivity.class.getSimpleName();
  private PendingIntent mPermissionIntent;
  private static final String ACTION_USB_PERMISSION = "edu.rosehulman.armscripts.action.USB_PERMISSION";
  private boolean mPermissionRequestPending;
  private UsbManager mUsbManager;
  private UsbAccessory mAccessory;
  private ParcelFileDescriptor mFileDescriptor;
  private FileInputStream mInputStream;
  private FileOutputStream mOutputStream;

  

  Runnable mRxRunnable = new Runnable() {

    public void run() {
      int ret = 0;
      byte[] buffer = new byte[255];

      // Loop that runs forever (or until a -1 error state).
      while (ret >= 0) {
        try {
          ret = mInputStream.read(buffer);
        } catch (IOException e) {
          break;
        }
        
        if (ret > 0) {
          // Convert the bytes into a string.
          String received = new String(buffer, 0, ret);
          final String receivedCommand = received.trim();
          runOnUiThread(new Runnable() {
            public void run() {
              onCommandReceived(receivedCommand);
            }
          });
        }
      }
    }
  };
  
  /**
   * Override this method with your activity if you'd like to receive messages.
   * @param receivedCommand
   */
  protected void onCommandReceived(final String receivedCommand) {
    //Toast.makeText(this, "Received command = " + receivedCommand, Toast.LENGTH_SHORT).show();
    Log.d(TAG, "Received command = " + receivedCommand);
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
        ACTION_USB_PERMISSION), 0);

    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
    registerReceiver(mUsbReceiver, filter);
  }
  
  private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (ACTION_USB_PERMISSION.equals(action)) {
        synchronized (this) {
          UsbAccessory accessory = (UsbAccessory) intent
              .getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
          if (intent.getBooleanExtra(
              UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            openAccessory(accessory);
          } else {
            Log.d(TAG, "permission denied for accessory "
                + accessory);
          }
          mPermissionRequestPending = false;
        }
      } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
        UsbAccessory accessory = (UsbAccessory) intent
            .getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        if (accessory != null && accessory.equals(mAccessory)) {
          closeAccessory();
        }
      }
    }
  };
  
  protected void sendCommand(String commandString) {
    new AsyncTask<String, Void, Void>() {
      @Override
      protected Void doInBackground(String... params) {
        String command = params[0];
        char[] buffer = new char[command.length() + 1];
        byte[] byteBuffer = new byte[command.length() + 1];
        command.getChars(0, command.length(), buffer, 0);
        buffer[command.length()] = '\n';
        for (int i = 0; i < command.length() + 1; i++) {
          byteBuffer[i] = (byte) buffer[i];
        }
        if (mOutputStream != null) {
          try {
            mOutputStream.write(byteBuffer);
          } catch (IOException e) {
            Log.e(TAG, "write failed", e);
          }
        }        
        return null;
      }
    }.execute(commandString);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mInputStream != null && mOutputStream != null) {
      return;
    }

    UsbAccessory[] accessories = mUsbManager.getAccessoryList();
    UsbAccessory accessory = (accessories == null ? null : accessories[0]);
    if (accessory != null) {
      if (mUsbManager.hasPermission(accessory)) {
        Log.d(TAG, "Permission ready.");
        openAccessory(accessory);
      } else {
        Log.d(TAG, "Requesting permission.");
        synchronized (mUsbReceiver) {
          if (!mPermissionRequestPending) {
            mUsbManager.requestPermission(accessory,
                mPermissionIntent);
            mPermissionRequestPending = true;
          }
        }
      }
    } else {
      Log.d(TAG, "mAccessory is null.");
    }
  }

  private void openAccessory(UsbAccessory accessory) {
    Log.d(TAG, "Open accessory called.");
    mFileDescriptor = mUsbManager.openAccessory(accessory);
    if (mFileDescriptor != null) {
      mAccessory = accessory;
      FileDescriptor fd = mFileDescriptor.getFileDescriptor();
      mInputStream = new FileInputStream(fd);
      mOutputStream = new FileOutputStream(fd);
      Thread thread = new Thread(null, mRxRunnable, TAG);
      thread.start();
      Log.d(TAG, "accessory opened");
    } else {
      Log.d(TAG, "accessory open fail");
    }
  }

  private void closeAccessory() {
    Log.d(TAG, "Close accessory called.");
    try {
      if (mFileDescriptor != null) {
        mFileDescriptor.close();
      }
    } catch (IOException e) {
    } finally {
      mFileDescriptor = null;
      mAccessory = null;
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    closeAccessory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unregisterReceiver(mUsbReceiver);
  }
}
